<?php
/**
 * Razorpay Webhook Handler
 * 
 * Handles subscription lifecycle events from Razorpay
 * 
 * Webhook URL: https://sudartnpscapp.in/api/subscriptions/webhook.php
 * 
 * Events handled:
 * - subscription.authenticated: Mandate authorized (trial active)
 * - subscription.activated: Subscription started
 * - subscription.charged: Recurring payment successful (₹299)
 * - subscription.pending: Payment pending/retry
 * - subscription.halted: Payment failed multiple times
 * - subscription.cancelled: User cancelled
 * - subscription.completed: All cycles completed
 * - subscription.paused: Subscription paused
 * - subscription.resumed: Subscription resumed
 */

require_once '../config/database.php';
require_once '../config/razorpay.php';
require_once '../models/Subscription.php';

// Log file for debugging
$logFile = __DIR__ . '/webhook_logs/' . date('Y-m-d') . '.log';
if (!is_dir(__DIR__ . '/webhook_logs')) {
    mkdir(__DIR__ . '/webhook_logs', 0755, true);
}

$payload = file_get_contents('php://input');
$timestamp = date('Y-m-d H:i:s');

// Log incoming webhook
file_put_contents($logFile, "\n[$timestamp] ===== WEBHOOK RECEIVED =====\n", FILE_APPEND);
file_put_contents($logFile, $payload . "\n", FILE_APPEND);

// Verify webhook signature
$signature = $_SERVER['HTTP_X_RAZORPAY_SIGNATURE'] ?? '';

if (!empty(RAZORPAY_WEBHOOK_SECRET) && RAZORPAY_WEBHOOK_SECRET !== 'your_webhook_secret_here') {
    if (!verifyRazorpaySignature($payload, $signature)) {
        file_put_contents($logFile, "[$timestamp] ❌ Invalid signature\n", FILE_APPEND);
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Invalid signature']);
        exit;
    }
}

$event = json_decode($payload, true);

if (!$event || !isset($event['event'])) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Invalid payload']);
    exit;
}

$database = new Database();
$db = $database->getConnection();
$subscription = new Subscription($db);

$eventType = $event['event'];
file_put_contents($logFile, "[$timestamp] Event: $eventType\n", FILE_APPEND);

try {
    switch ($eventType) {
        case 'subscription.authenticated':
            handleAuthenticated($subscription, $event, $logFile);
            break;

        case 'subscription.activated':
            handleActivated($subscription, $event, $logFile);
            break;

        case 'subscription.charged':
            handleCharged($subscription, $event, $db, $logFile);
            break;

        case 'subscription.pending':
            handlePending($subscription, $event, $logFile);
            break;

        case 'subscription.halted':
            handleHalted($subscription, $event, $logFile);
            break;

        case 'subscription.cancelled':
            handleCancelled($subscription, $event, $logFile);
            break;

        case 'subscription.paused':
            handlePaused($subscription, $event, $logFile);
            break;

        case 'subscription.resumed':
            handleResumed($subscription, $event, $logFile);
            break;

        case 'subscription.completed':
            handleCompleted($subscription, $event, $logFile);
            break;

        default:
            file_put_contents($logFile, "[$timestamp] Unhandled event: $eventType\n", FILE_APPEND);
    }

    http_response_code(200);
    echo json_encode(['success' => true, 'message' => 'Webhook processed']);

} catch (Exception $e) {
    file_put_contents($logFile, "[$timestamp] ❌ Error: " . $e->getMessage() . "\n", FILE_APPEND);
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => $e->getMessage()]);
}

// ============================================
// WEBHOOK HANDLERS
// ============================================

function handleAuthenticated($subscription, $event, $logFile) {
    $subEntity = $event['payload']['subscription']['entity'] ?? [];
    $razorpaySubId = $subEntity['id'] ?? null;
    
    if (!$razorpaySubId) return;
    
    $subData = $subscription->getByRazorpayId($razorpaySubId);
    if (!$subData) return;

    // Option A: 7-day FREE trial then ₹299/month.
    // Use Razorpay `start_at` (trial end) from webhook payload when available.
    $startAt = isset($subEntity['start_at']) ? (int)$subEntity['start_at'] : null;
    if (!$startAt) {
        // fallback: fetch from Razorpay API
        $rzpSubResp = razorpayApiRequest('subscriptions/' . $razorpaySubId);
        $rzpSub = $rzpSubResp['data'] ?? [];
        $startAt = isset($rzpSub['start_at']) ? (int)$rzpSub['start_at'] : null;
    }
    if (!$startAt) return;

    $trialStart = date('Y-m-d H:i:s');
    $trialEnd = date('Y-m-d H:i:s', $startAt);

    // Update DB without recording any payment
    $db = (new Database())->getConnection();
    $razorpayStatus = $subEntity['status'] ?? 'authenticated';
    $update = $db->prepare(
        "UPDATE subscriptions
         SET status = 'authenticated',
             razorpay_status = :razorpay_status,
             is_trial = 1,
             trial_start = :trial_start,
             trial_end = :trial_end,
             current_period_start = :trial_start,
             current_period_end = :trial_end,
             next_billing_date = :trial_end
         WHERE id = :id"
    );
    $update->execute([
        ':razorpay_status' => $razorpayStatus,
        ':trial_start' => $trialStart,
        ':trial_end' => $trialEnd,
        ':id' => $subData['id'],
    ]);

    // Activate premium until trial end
    $subscription->id = $subData['id'];
    $subscription->updateUserPremiumStatus($subData['user_id'], true, $trialEnd);

    file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ✅ Authenticated (trial): User {$subData['user_id']}, Trial ends: {$trialEnd}\n", FILE_APPEND);
}

function handleActivated($subscription, $event, $logFile) {
    $subEntity = $event['payload']['subscription']['entity'] ?? [];
    $razorpaySubId = $subEntity['id'] ?? null;
    $razorpayStatus = $subEntity['status'] ?? 'active';
    
    if (!$razorpaySubId) return;
    
    $subData = $subscription->getByRazorpayId($razorpaySubId);
    if (!$subData) return;
    
    $subscription->id = $subData['id'];
    $subscription->updateRazorpayStatus($razorpayStatus);

    $now = time();
    $trialEnd = !empty($subData['trial_end']) ? strtotime($subData['trial_end']) : null;
    $inTrial = ($trialEnd && $trialEnd > $now);

    if ($inTrial) {
        $subscription->updateStatus('authenticated');
        file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ℹ️ Activated during trial → kept as authenticated. User {$subData['user_id']}\n", FILE_APPEND);
        return;
    }

    // Trial ended → treat as active
    $subscription->updateStatus('active');
    
    file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ✅ Activated: User {$subData['user_id']}\n", FILE_APPEND);
}

function handleCharged($subscription, $event, $db, $logFile) {
    $subEntity = $event['payload']['subscription']['entity'] ?? [];
    $paymentEntity = $event['payload']['payment']['entity'] ?? [];
    $razorpaySubId = $subEntity['id'] ?? null;
    $razorpayStatus = $subEntity['status'] ?? 'active';
    
    if (!$razorpaySubId) return;
    
    $subData = $subscription->getByRazorpayId($razorpaySubId);
    if (!$subData) return;
    
    $amount = ($paymentEntity['amount'] ?? 29900) / 100;
    
    // Record payment
    $subscription->id = $subData['id'];
    // Prefer Razorpay cycle timestamps to avoid date drift
    $subEntityCycle = $event['payload']['subscription']['entity'] ?? [];
    $currentStart = isset($subEntityCycle['current_start']) ? (int)$subEntityCycle['current_start'] : null;
    $currentEnd = isset($subEntityCycle['current_end']) ? (int)$subEntityCycle['current_end'] : null;

    if ($currentStart && $currentEnd) {
        $currentStartDt = date('Y-m-d H:i:s', $currentStart);
        $currentEndDt = date('Y-m-d H:i:s', $currentEnd);

        $update = $db->prepare(
            "UPDATE subscriptions
             SET is_trial = 0,
                 status = 'active',
                 razorpay_status = :razorpay_status,
                 current_period_start = :cps,
                 current_period_end = :cpe,
                 next_billing_date = :nbd,
                 last_payment_date = NOW(),
                 last_payment_amount = :amt,
                 total_payments = total_payments + 1
             WHERE id = :id"
        );
        $update->execute([
            ':razorpay_status' => $razorpayStatus,
            ':cps' => $currentStartDt,
            ':cpe' => $currentEndDt,
            ':nbd' => $currentEndDt,
            ':amt' => $amount,
            ':id' => $subData['id'],
        ]);
    } else {
        // Fallback
        $subscription->recordPayment($amount);
        $subscription->updateRazorpayStatus($razorpayStatus);
    }
    
    // Log payment
    $paymentQuery = "INSERT INTO subscription_payments 
                     (subscription_id, user_id, razorpay_payment_id, amount, status, payment_method)
                     VALUES (?, ?, ?, ?, 'captured', ?)";
    $stmt = $db->prepare($paymentQuery);
    $stmt->execute([
        $subData['id'],
        $subData['user_id'],
        $paymentEntity['id'] ?? null,
        $amount,
        $paymentEntity['method'] ?? 'unknown'
    ]);
    
    // Extend premium access until cycle end (prefer Razorpay current_end if present)
    $expiresAt = ($currentEnd ?? null) ? date('Y-m-d H:i:s', $currentEnd) : date('Y-m-d H:i:s', strtotime('+30 days'));
    $subscription->updateUserPremiumStatus($subData['user_id'], true, $expiresAt);
    
    file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] 💰 Charged: User {$subData['user_id']}, Amount: ₹$amount\n", FILE_APPEND);
}

function handlePending($subscription, $event, $logFile) {
    $subEntity = $event['payload']['subscription']['entity'] ?? [];
    $razorpaySubId = $subEntity['id'] ?? null;
    $razorpayStatus = $subEntity['status'] ?? 'pending';
    
    if (!$razorpaySubId) return;
    
    $subData = $subscription->getByRazorpayId($razorpaySubId);
    if (!$subData) return;
    
    $subscription->id = $subData['id'];
    $subscription->updateStatus('pending');
    $subscription->updateRazorpayStatus($razorpayStatus);
    
    file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ⏳ Pending: User {$subData['user_id']}\n", FILE_APPEND);
}

function handleHalted($subscription, $event, $logFile) {
    $subEntity = $event['payload']['subscription']['entity'] ?? [];
    $razorpaySubId = $subEntity['id'] ?? null;
    $razorpayStatus = $subEntity['status'] ?? 'halted';
    
    if (!$razorpaySubId) return;
    
    $subData = $subscription->getByRazorpayId($razorpaySubId);
    if (!$subData) return;
    
    $subscription->id = $subData['id'];
    $subscription->updateStatus('halted');
    $subscription->updateRazorpayStatus($razorpayStatus);
    
    // Suspend premium
    $subscription->updateUserPremiumStatus($subData['user_id'], false, null);
    
    file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ⚠️ Halted: User {$subData['user_id']} - Premium suspended\n", FILE_APPEND);
}

function handleCancelled($subscription, $event, $logFile) {
    $subEntity = $event['payload']['subscription']['entity'] ?? [];
    $razorpaySubId = $subEntity['id'] ?? null;
    $razorpayStatus = $subEntity['status'] ?? 'cancelled';
    
    if (!$razorpaySubId) return;
    
    $subData = $subscription->getByRazorpayId($razorpaySubId);
    if (!$subData) return;
    
    $subscription->id = $subData['id'];
    $subscription->cancel();
    $subscription->updateRazorpayStatus($razorpayStatus);

    // Requirement: revoke premium access immediately on cancel
    $subscription->updateUserPremiumStatus($subData['user_id'], false, null);
    
    file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] 🚫 Cancelled: User {$subData['user_id']}\n", FILE_APPEND);
}

function handlePaused($subscription, $event, $logFile) {
    $subEntity = $event['payload']['subscription']['entity'] ?? [];
    $razorpaySubId = $subEntity['id'] ?? null;
    $razorpayStatus = $subEntity['status'] ?? 'paused';
    
    if (!$razorpaySubId) return;
    
    $subData = $subscription->getByRazorpayId($razorpaySubId);
    if (!$subData) return;
    
    $subscription->id = $subData['id'];
    $subscription->updateStatus('paused');
    $subscription->updateRazorpayStatus($razorpayStatus);
    
    file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ⏸️ Paused: User {$subData['user_id']}\n", FILE_APPEND);
}

function handleResumed($subscription, $event, $logFile) {
    $subEntity = $event['payload']['subscription']['entity'] ?? [];
    $razorpaySubId = $subEntity['id'] ?? null;
    $razorpayStatus = $subEntity['status'] ?? 'active';
    
    if (!$razorpaySubId) return;
    
    $subData = $subscription->getByRazorpayId($razorpaySubId);
    if (!$subData) return;
    
    $subscription->id = $subData['id'];
    $subscription->updateStatus('active');
    $subscription->updateRazorpayStatus($razorpayStatus);
    
    file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ▶️ Resumed: User {$subData['user_id']}\n", FILE_APPEND);
}

function handleCompleted($subscription, $event, $logFile) {
    $subEntity = $event['payload']['subscription']['entity'] ?? [];
    $razorpaySubId = $subEntity['id'] ?? null;
    $razorpayStatus = $subEntity['status'] ?? 'completed';
    
    if (!$razorpaySubId) return;
    
    $subData = $subscription->getByRazorpayId($razorpaySubId);
    if (!$subData) return;
    
    $subscription->id = $subData['id'];
    $subscription->updateStatus('completed');
    $subscription->updateRazorpayStatus($razorpayStatus);
    
    file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ✔️ Completed: User {$subData['user_id']}\n", FILE_APPEND);
}
?>

