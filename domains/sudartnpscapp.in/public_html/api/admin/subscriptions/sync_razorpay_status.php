<?php
/**
 * Admin - Sync Razorpay Subscription Status + Payment Status
 * 
 * Fetches live subscription status AND latest payment status from Razorpay API
 * 
 * POST /api/admin/subscriptions/sync_razorpay_status.php
 * Body: { "subscription_id": 123 }
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../config/razorpay.php';
require_once '../../admin/auth/middleware.php';
require_once '../../models/Subscription.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

$data = json_decode(file_get_contents('php://input'));

if (empty($data->subscription_id)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'subscription_id is required']);
    exit;
}

$database = new Database();
$db = $database->getConnection();

try {
    $subscription = new Subscription($db);
    
    // Get subscription by ID
    $subData = $subscription->getById($data->subscription_id);
    
    if (!$subData) {
        http_response_code(404);
        echo json_encode(['success' => false, 'message' => 'Subscription not found']);
        exit;
    }
    
    $razorpaySubId = $subData['razorpay_subscription_id'] ?? null;
    
    if (empty($razorpaySubId)) {
        http_response_code(400);
        echo json_encode([
            'success' => false, 
            'message' => 'Subscription has no Razorpay subscription ID'
        ]);
        exit;
    }
    
    // Fetch live SUBSCRIPTION status from Razorpay API
    $rzpResponse = razorpayApiRequest('subscriptions/' . $razorpaySubId, 'GET');
    
    if ($rzpResponse['code'] !== 200) {
        $errorMsg = $rzpResponse['data']['error']['description'] ?? 'Failed to fetch from Razorpay';
        http_response_code(502);
        echo json_encode([
            'success' => false,
            'message' => 'Razorpay API error: ' . $errorMsg,
            'razorpay_response' => $rzpResponse
        ]);
        exit;
    }
    
    $rzpData = $rzpResponse['data'];
    $newRazorpayStatus = $rzpData['status'] ?? null;
    $oldRazorpayStatus = $subData['razorpay_status'] ?? null;
    
    if (!$newRazorpayStatus) {
        http_response_code(502);
        echo json_encode([
            'success' => false,
            'message' => 'Razorpay response missing status field'
        ]);
        exit;
    }
    
    // Get user's mobile to search payments
    $userQuery = "SELECT mobile, email FROM users WHERE id = ?";
    $stmt = $db->prepare($userQuery);
    $stmt->execute([$subData['user_id']]);
    $userData = $stmt->fetch(PDO::FETCH_ASSOC);
    
    $latestPayments = [];
    $latestPaymentStatus = null;
    
    // Fetch ALL recent payments from Razorpay and filter by user's mobile
    if ($userData && $userData['mobile']) {
        $userMobile = preg_replace('/[^0-9]/', '', $userData['mobile']); // Remove non-digits
        $userMobileLast10 = substr($userMobile, -10); // Get last 10 digits
        
        // Fetch recent payments (up to 100)
        $paymentsResponse = razorpayApiRequest('payments?count=100', 'GET');
        
        if ($paymentsResponse['code'] === 200 && !empty($paymentsResponse['data']['items'])) {
            foreach ($paymentsResponse['data']['items'] as $payment) {
                $paymentContact = $payment['contact'] ?? '';
                $paymentContactClean = preg_replace('/[^0-9]/', '', $paymentContact);
                $paymentContactLast10 = substr($paymentContactClean, -10);
                
                // Match by last 10 digits of mobile number
                if ($paymentContactLast10 === $userMobileLast10) {
                    $paymentInfo = [
                        'payment_id' => $payment['id'] ?? null,
                        'status' => $payment['status'] ?? null,
                        'amount' => isset($payment['amount']) ? $payment['amount'] / 100 : null,
                        'method' => $payment['method'] ?? null,
                        'error_code' => $payment['error_code'] ?? null,
                        'error_description' => $payment['error_description'] ?? null,
                        'refund_status' => $payment['refund_status'] ?? null,
                        'created_at' => isset($payment['created_at']) ? date('Y-m-d H:i:s', $payment['created_at']) : null
                    ];
                    $latestPayments[] = $paymentInfo;
                    
                    // Set the latest payment status (first match is most recent)
                    if (!$latestPaymentStatus) {
                        // Priority: refund_status > status
                        if (!empty($payment['refund_status']) && $payment['refund_status'] !== 'null') {
                            $latestPaymentStatus = $payment['refund_status']; // partial, full
                        } else {
                            $latestPaymentStatus = $payment['status']; // captured, failed, refunded
                        }
                    }
                }
            }
        }
    }
    
    // Also check invoices for subscription-linked payments
    if (empty($latestPayments)) {
        $invoicesResponse = razorpayApiRequest('invoices?subscription_id=' . $razorpaySubId . '&count=5', 'GET');
        if ($invoicesResponse['code'] === 200 && !empty($invoicesResponse['data']['items'])) {
            foreach ($invoicesResponse['data']['items'] as $invoice) {
                $paymentInfo = [
                    'invoice_id' => $invoice['id'] ?? null,
                    'status' => $invoice['status'] ?? null,
                    'amount' => isset($invoice['amount']) ? $invoice['amount'] / 100 : null,
                    'created_at' => isset($invoice['created_at']) ? date('Y-m-d H:i:s', $invoice['created_at']) : null
                ];
                $latestPayments[] = $paymentInfo;
                if (!$latestPaymentStatus) {
                    $latestPaymentStatus = $invoice['status'] ?? null;
                }
            }
        }
    }
    
    // Update razorpay_status in database
    $subscription->id = $subData['id'];
    $updated = $subscription->updateRazorpayStatus($newRazorpayStatus);
    
    // Also update last_payment_status if we have it
    if ($latestPaymentStatus) {
        $updatePaymentStatus = $db->prepare("UPDATE subscriptions SET last_payment_status = ? WHERE id = ?");
        $updatePaymentStatus->execute([$latestPaymentStatus, $subData['id']]);
    }
    
    if (!$updated) {
        throw new Exception('Failed to update razorpay_status in database');
    }
    
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => 'Razorpay status synced successfully',
        'data' => [
            'subscription_id' => (int)$subData['id'],
            'razorpay_subscription_id' => $razorpaySubId,
            'old_razorpay_status' => $oldRazorpayStatus,
            'new_razorpay_status' => $newRazorpayStatus,
            'internal_status' => $subData['status'],
            'latest_payment_status' => $latestPaymentStatus,
            'razorpay_details' => [
                'status' => $rzpData['status'] ?? null,
                'current_start' => isset($rzpData['current_start']) ? date('Y-m-d H:i:s', $rzpData['current_start']) : null,
                'current_end' => isset($rzpData['current_end']) ? date('Y-m-d H:i:s', $rzpData['current_end']) : null,
                'charge_at' => isset($rzpData['charge_at']) ? date('Y-m-d H:i:s', $rzpData['charge_at']) : null,
                'paid_count' => $rzpData['paid_count'] ?? null,
                'remaining_count' => $rzpData['remaining_count'] ?? null
            ],
            'latest_payments' => $latestPayments
        ]
    ]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error syncing Razorpay status',
        'error' => $e->getMessage()
    ]);
}
?>
