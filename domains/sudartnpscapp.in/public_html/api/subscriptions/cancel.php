<?php
/**
 * Cancel Subscription
 * 
 * Cancels user's subscription at end of current billing period
 * 
 * POST /api/subscriptions/cancel.php
 * Body: { "user_id": 123 }
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

if (empty($data->user_id)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'User ID is required']);
    exit;
}

$database = new Database();
$db = $database->getConnection();

try {
    $subscription = new Subscription($db);
    $subData = $subscription->getActiveByUserId($data->user_id);

    if (!$subData) {
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'No active subscription found'
        ]);
        exit;
    }

    // Cancel on Razorpay (at end of current period)
    if ($subData['razorpay_subscription_id']) {
        $cancelResponse = razorpayApiRequest(
            'subscriptions/' . $subData['razorpay_subscription_id'] . '/cancel',
            'POST',
            ['cancel_at_cycle_end' => true]
        );

        if ($cancelResponse['code'] !== 200) {
            // Log error but continue with local cancellation
            error_log('Razorpay cancel error: ' . json_encode($cancelResponse));
        }
    }

    // Update local database
    $subscription->id = $subData['id'];
    $subscription->cancel();

    // Determine when access ends
    $accessEndsAt = $subData['current_period_end'] ?? $subData['trial_end'] ?? date('Y-m-d H:i:s');

    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => 'Subscription cancelled successfully',
        'data' => [
            'access_until' => $accessEndsAt,
            'note' => 'You will have premium access until ' . date('d M Y', strtotime($accessEndsAt))
        ]
    ]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error cancelling subscription',
        'error' => $e->getMessage()
    ]);
}
?>

