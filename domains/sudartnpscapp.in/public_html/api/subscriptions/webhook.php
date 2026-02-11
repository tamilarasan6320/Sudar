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
require_once '../services/MetaAppEventsApiService.php';

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

    // Detect trial -> paid conversion (first successful ₹299 charge after trial)
    $wasTrial = (bool)($subData['is_trial'] ?? 0);
    $paidCountBefore = (int)($subData['total_payments'] ?? 0);
    $hasTrialStart = !empty($subData['trial_start']);
    $alreadySentTrialPaid = !empty($subData['meta_trial_paid_event_sent_at'] ?? null);
    
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

    // Track purchase for referral link (best-effort)
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS referral_installs (
            id INT(11) AUTO_INCREMENT PRIMARY KEY,
            code VARCHAR(64) NOT NULL,
            device_id VARCHAR(64) NOT NULL,
            user_id INT(11) NULL,
            install_referrer TEXT NULL,
            first_open_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE KEY uniq_device (device_id),
            KEY idx_code (code),
            KEY idx_user (user_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        $db->exec("CREATE TABLE IF NOT EXISTS referral_purchases (
            id INT(11) AUTO_INCREMENT PRIMARY KEY,
            code VARCHAR(64) NOT NULL,
            user_id INT(11) NOT NULL,
            subscription_id INT(11) NOT NULL,
            razorpay_payment_id VARCHAR(100) NOT NULL,
            amount DECIMAL(10,2) NOT NULL,
            currency VARCHAR(10) NOT NULL DEFAULT 'INR',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY uniq_payment (razorpay_payment_id),
            KEY idx_code (code),
            KEY idx_user (user_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        $rzpPaymentId = $paymentEntity['id'] ?? null;
        if (!empty($rzpPaymentId)) {
            $codeStmt = $db->prepare("SELECT code FROM referral_installs WHERE user_id = ? ORDER BY first_open_at DESC LIMIT 1");
            $codeStmt->execute([$subData['user_id']]);
            $codeRow = $codeStmt->fetch(PDO::FETCH_ASSOC);
            $refCode = isset($codeRow['code']) ? trim((string)$codeRow['code']) : '';

            if ($refCode !== '') {
                $currency = $paymentEntity['currency'] ?? 'INR';
                $ins = $db->prepare("INSERT IGNORE INTO referral_purchases
                                     (code, user_id, subscription_id, razorpay_payment_id, amount, currency)
                                     VALUES (?, ?, ?, ?, ?, ?)");
                $ins->execute([
                    $refCode,
                    (int)$subData['user_id'],
                    (int)$subData['id'],
                    (string)$rzpPaymentId,
                    $amount,
                    (string)$currency,
                ]);
            }
        }
    } catch (Exception $e) {
        // ignore
    }
    
    // Extend premium access until cycle end (prefer Razorpay current_end if present)
    $expiresAt = ($currentEnd ?? null) ? date('Y-m-d H:i:s', $currentEnd) : date('Y-m-d H:i:s', strtotime('+30 days'));
    $subscription->updateUserPremiumStatus($subData['user_id'], true, $expiresAt);
    
    file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] 💰 Charged: User {$subData['user_id']}, Amount: ₹$amount\n", FILE_APPEND);

    // === TRIAL -> PAID (₹299) META EVENT ===
    if ($wasTrial && $hasTrialStart && $paidCountBefore === 0) {
        if ($alreadySentTrialPaid) {
            file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ℹ️ Meta trial_paid_299 already sent at {$subData['meta_trial_paid_event_sent_at']}\n", FILE_APPEND);
            return;
        }

        file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] 📤 Trial → Paid ₹299 detected - sending Meta event...\n", FILE_APPEND);

        // Get user data for Meta matching
        $userStmt = $db->prepare("SELECT id, mobile, email, name FROM users WHERE id = ?");
        $userStmt->execute([$subData['user_id']]);
        $userData = $userStmt->fetch(PDO::FETCH_ASSOC);

        $metaService = new MetaAppEventsApiService($logFile);
        $result = $metaService->sendTrialPaid299Event($subData, $userData ?: [], $paymentEntity ?: []);

        if (!empty($result['success'])) {
            // Mark as sent (idempotency) - safe even if column isn't present (catch errors)
            try {
                $updateStmt = $db->prepare("UPDATE subscriptions SET meta_trial_paid_event_sent_at = NOW() WHERE id = ?");
                $updateStmt->execute([$subData['id']]);
            } catch (Exception $e) {
                file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ⚠️ Could not update meta_trial_paid_event_sent_at: " . $e->getMessage() . "\n", FILE_APPEND);
            }

            file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ✅ Meta trial_paid_299 event sent successfully\n", FILE_APPEND);
        } else {
            file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ❌ Meta trial_paid_299 event failed: " . ($result['error'] ?? 'Unknown error') . "\n", FILE_APPEND);
        }
    }
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
    
    // Determine access end date based on trial vs paid
    $now = time();
    $isTrial = (bool)$subData['is_trial'];
    $trialEnd = $subData['trial_end'] ? strtotime($subData['trial_end']) : null;
    $periodEnd = $subData['current_period_end'] ? strtotime($subData['current_period_end']) : null;
    
    // Keep premium until end of period (don't revoke immediately)
    if ($isTrial && $trialEnd && $trialEnd > $now) {
        // Trial user: keep premium until trial_end (7 days)
        $subscription->updateUserPremiumStatus($subData['user_id'], true, $subData['trial_end']);
        file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] 🚫 Cancelled (trial): User {$subData['user_id']} - Premium until {$subData['trial_end']}\n", FILE_APPEND);
        
        // === TRIAL CANCEL WITHIN 7 DAYS: Send Meta Event ===
        $alreadySent = !empty($subData['meta_trial_cancel_event_sent_at']);
        
        if (!$alreadySent) {
            file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] 📤 Trial cancel within 7d detected - sending Meta event...\n", FILE_APPEND);
            
            // Get user data for Meta matching
            $db = (new Database())->getConnection();
            $userStmt = $db->prepare("SELECT id, mobile, email, name FROM users WHERE id = ?");
            $userStmt->execute([$subData['user_id']]);
            $userData = $userStmt->fetch(PDO::FETCH_ASSOC);
            
            // Send Meta App Event
            $metaService = new MetaAppEventsApiService($logFile);
            $result = $metaService->sendTrialCancelEvent($subData, $userData ?: []);
            
            if ($result['success']) {
                // Mark event as sent (idempotency)
                $updateStmt = $db->prepare("UPDATE subscriptions SET meta_trial_cancel_event_sent_at = NOW() WHERE id = ?");
                $updateStmt->execute([$subData['id']]);
                file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ✅ Meta trial_cancel_within_7d event sent successfully\n", FILE_APPEND);
            } else {
                file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ❌ Meta event failed: " . ($result['error'] ?? 'Unknown error') . "\n", FILE_APPEND);
            }
        } else {
            file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] ℹ️ Meta event already sent at {$subData['meta_trial_cancel_event_sent_at']}\n", FILE_APPEND);
        }
        
    } elseif (!$isTrial && $periodEnd && $periodEnd > $now) {
        // Paid user: keep premium until current_period_end (1 month)
        $subscription->updateUserPremiumStatus($subData['user_id'], true, $subData['current_period_end']);
        file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] 🚫 Cancelled (paid): User {$subData['user_id']} - Premium until {$subData['current_period_end']}\n", FILE_APPEND);
    } else {
        // Period already ended - revoke premium
        $subscription->updateUserPremiumStatus($subData['user_id'], false, null);
        file_put_contents($logFile, "[" . date('Y-m-d H:i:s') . "] 🚫 Cancelled (expired): User {$subData['user_id']} - Premium revoked\n", FILE_APPEND);
    }
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

