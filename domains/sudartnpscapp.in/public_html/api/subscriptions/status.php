<?php
/**
 * Get Subscription Status
 * 
 * Returns user's premium status and subscription details
 * Auto-syncs with Razorpay to get latest status
 * 
 * GET /api/subscriptions/status.php?user_id=123
 */

require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../config/razorpay.php';
require_once '../models/Subscription.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

$user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : null;

if (!$user_id) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'User ID is required']);
    exit;
}

$database = new Database();
$db = $database->getConnection();

try {
    $subscription = new Subscription($db);
    
    // Auto-sync with Razorpay - get latest status from their API
    $latestSub = $db->prepare("SELECT id, razorpay_subscription_id, razorpay_status FROM subscriptions WHERE user_id = ? ORDER BY created_at DESC LIMIT 1");
    $latestSub->execute([$user_id]);
    $existingSub = $latestSub->fetch(PDO::FETCH_ASSOC);
    
    if ($existingSub && !empty($existingSub['razorpay_subscription_id'])) {
        // Fetch live status from Razorpay (silent - don't fail if error)
        try {
            $rzpResponse = razorpayApiRequest('subscriptions/' . $existingSub['razorpay_subscription_id'], 'GET');
            if ($rzpResponse['code'] === 200 && !empty($rzpResponse['data']['status'])) {
                $newRzpStatus = $rzpResponse['data']['status'];
                $oldRzpStatus = $existingSub['razorpay_status'];
                
                // Update if status changed
                if ($newRzpStatus !== $oldRzpStatus) {
                    $updateStmt = $db->prepare("UPDATE subscriptions SET razorpay_status = ? WHERE id = ?");
                    $updateStmt->execute([$newRzpStatus, $existingSub['id']]);
                    
                    // If cancelled, also update internal status
                    if (in_array($newRzpStatus, ['cancelled', 'expired', 'halted', 'completed'])) {
                        $cancelStmt = $db->prepare("UPDATE subscriptions SET status = 'cancelled', auto_renew = 0 WHERE id = ?");
                        $cancelStmt->execute([$existingSub['id']]);
                    }
                }
            }
        } catch (Exception $syncError) {
            // Silently ignore sync errors - continue with cached status
        }
    }
    
    // Check if user is premium
    $isPremium = $subscription->isUserPremium($user_id);
    
    // Get active subscription details
    $subData = $subscription->getActiveByUserId($user_id);
    
    // Get user details
    $userQuery = "SELECT is_premium, premium_expires_at FROM users WHERE id = ?";
    $stmt = $db->prepare($userQuery);
    $stmt->execute([$user_id]);
    $userData = $stmt->fetch(PDO::FETCH_ASSOC);

    $response = [
        'success' => true,
        'is_premium' => $isPremium,
        // IMPORTANT: only treat subscription as "active" after Razorpay confirms payment
        // (created/pending should NOT block user from trying payment again)
        'has_subscription' => false,
        'subscription' => null,
        'premium_source' => null,
        'subscription_requires_payment' => false,
        'checkout' => null
    ];

    if ($subData) {
        $status = $subData['status'] ?? '';
        $isConfirmed = in_array($status, ['authenticated', 'active']);
        $isPendingPayment = in_array($status, ['created', 'pending']);

        $now = time();
        $trialEnd = $subData['trial_end'] ? strtotime($subData['trial_end']) : null;
        $periodEnd = $subData['current_period_end'] ? strtotime($subData['current_period_end']) : null;

        $trialDaysLeft = null;
        $daysLeft = null;

        // Option A: Trial is the period BEFORE Razorpay start_at.
        // During trial, we show trial days left even if Razorpay subscription status is "active".
        if ($trialEnd && $trialEnd > $now) {
            // Use "ceil" for friendly UX (show 1 day when a few hours left),
            // but cap to the actual trial duration to avoid showing 8 days.
            $trialStart = $subData['trial_start'] ? strtotime($subData['trial_start']) : null;
            $trialDurationDays = 7;
            if ($trialStart && $trialEnd && $trialEnd > $trialStart) {
                $trialDurationDays = max(1, (int)ceil(($trialEnd - $trialStart) / 86400));
            }

            $trialDaysLeft = max(0, (int)ceil(($trialEnd - $now) / 86400));
            if ($trialDaysLeft > $trialDurationDays) {
                $trialDaysLeft = $trialDurationDays;
            }
        } elseif ($periodEnd) {
            $daysLeft = max(0, ceil(($periodEnd - $now) / 86400));
        }

        $response['subscription'] = [
            'id' => (int)$subData['id'],
            'razorpay_subscription_id' => $subData['razorpay_subscription_id'],
            'status' => $subData['status'],
            'plan_name' => $subData['plan_name'],
            'amount' => (float)$subData['amount'],
            'currency' => $subData['currency'],
            'is_trial' => (bool)($trialEnd && $trialEnd > $now),
            'trial_start' => $subData['trial_start'],
            'trial_end' => $subData['trial_end'],
            'trial_days_left' => $trialDaysLeft,
            'current_period_start' => $subData['current_period_start'],
            'current_period_end' => $subData['current_period_end'],
            'days_left' => $daysLeft,
            'next_billing_date' => $subData['next_billing_date'],
            'total_payments' => (int)$subData['total_payments'],
            'auto_renew' => (bool)$subData['auto_renew'],
            'created_at' => $subData['created_at'],
            'short_url' => $subData['short_url'] ?? null
        ];

        if ($isConfirmed) {
            $response['has_subscription'] = true;
            $response['premium_source'] = 'subscription';
        } else if ($isPendingPayment) {
            $response['subscription_requires_payment'] = true;
            $response['checkout'] = [
                'subscription_id' => $subData['razorpay_subscription_id'],
                'short_url' => $subData['short_url'] ?? null,
                'status' => $status
            ];
        }

        // Add trial-specific info
        if ($trialDaysLeft !== null) {
            $response['trial_days_left'] = $trialDaysLeft;
        }
        if ($daysLeft !== null) {
            $response['days_left'] = $daysLeft;
        }
    }

    // Check for manual premium (admin granted)
    if (!$isPremium && !$subData && $userData && $userData['is_premium']) {
        $expiresAt = $userData['premium_expires_at'];
        if (!$expiresAt || strtotime($expiresAt) > time()) {
            $response['is_premium'] = true;
            $response['premium_source'] = 'manual';
            if ($expiresAt) {
                $response['premium_expires_at'] = $expiresAt;
                $response['days_left'] = max(0, ceil((strtotime($expiresAt) - time()) / 86400));
            }
        }
    }

    http_response_code(200);
    echo json_encode($response);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error fetching subscription status',
        'error' => $e->getMessage()
    ]);
}
?>

