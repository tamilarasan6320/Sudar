<?php
/**
 * Verify Razorpay Subscription Authorization (Mandate)
 *
 * Called after Razorpay checkout success for subscription authorization.
 * No money should be collected during the 7-day FREE trial.
 *
 * This endpoint only verifies the signature and marks the trial as active,
 * using Razorpay subscription `start_at` as the trial end date.
 * 
 * POST /api/subscriptions/verify.php
 * Body: {
 *   "razorpay_payment_id": "pay_xxx",
 *   "razorpay_subscription_id": "sub_xxx",
 *   "razorpay_signature": "xxx"
 * }
 */

require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../config/razorpay.php';
require_once '../models/Subscription.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

$data = json_decode(file_get_contents('php://input'));

// Validate required fields
if (empty($data->razorpay_payment_id) || 
    empty($data->razorpay_subscription_id) || 
    empty($data->razorpay_signature)) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Missing required fields: razorpay_payment_id, razorpay_subscription_id, razorpay_signature'
    ]);
    exit;
}

$database = new Database();
$db = $database->getConnection();

try {
    // Step 1: Verify signature
    $isValid = verifyPaymentSignature(
        $data->razorpay_subscription_id,
        $data->razorpay_payment_id,
        $data->razorpay_signature
    );

    if (!$isValid) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Invalid payment signature. Payment verification failed.'
        ]);
        exit;
    }

    // Step 2: Get subscription from database
    $subscription = new Subscription($db);
    $subData = $subscription->getByRazorpayId($data->razorpay_subscription_id);

    if (!$subData) {
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'Subscription not found in database'
        ]);
        exit;
    }

    // Step 3: Fetch subscription from Razorpay to get `start_at`
    $rzpSubResp = razorpayApiRequest('subscriptions/' . $data->razorpay_subscription_id);
    $rzpSub = $rzpSubResp['data'] ?? [];
    $startAt = isset($rzpSub['start_at']) ? (int)$rzpSub['start_at'] : null;

    if (!$startAt) {
        throw new Exception('Razorpay subscription start_at missing');
    }

    $trialStart = date('Y-m-d H:i:s');
    $trialEnd = date('Y-m-d H:i:s', $startAt); // trial ends when subscription starts charging

    // Step 4: Mark subscription as authenticated (trial active) WITHOUT recording any payment
    $update = $db->prepare(
        "UPDATE subscriptions
         SET status = 'authenticated',
             is_trial = 1,
             trial_start = :trial_start,
             trial_end = :trial_end,
             current_period_start = :trial_start,
             current_period_end = :trial_end,
             next_billing_date = :trial_end
         WHERE id = :id"
    );
    $update->execute([
        ':trial_start' => $trialStart,
        ':trial_end' => $trialEnd,
        ':id' => $subData['id'],
    ]);

    // Step 5: Update user premium until trial_end
    $subscription->id = $subData['id'];
    $subscription->updateUserPremiumStatus($subData['user_id'], true, $trialEnd);

    // Success response
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => '🎉 Subscription authorized! Your 7-day free trial is active.',
        'data' => [
            'is_premium' => true,
            'subscription_status' => 'authenticated',
            'is_trial' => true,
            'trial_ends_at' => $trialEnd,
            'next_billing_date' => date('Y-m-d', $startAt),
            'next_billing_amount' => 299,
            'payment_id' => $data->razorpay_payment_id
        ]
    ]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error verifying payment',
        'error' => $e->getMessage()
    ]);
}
?>

