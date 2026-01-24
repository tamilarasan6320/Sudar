<?php
/**
 * Admin - Sync ALL Razorpay Subscription & Payment Statuses
 * 
 * Fetches live status for all subscriptions from Razorpay API
 * Called automatically when admin loads subscriptions page
 * 
 * POST /api/admin/subscriptions/sync_all_razorpay_status.php
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../config/razorpay.php';
require_once '../../admin/auth/middleware.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

$database = new Database();
$db = $database->getConnection();

try {
    // Get all subscriptions with razorpay_subscription_id
    $query = "SELECT s.id, s.user_id, s.razorpay_subscription_id, s.razorpay_status, s.last_payment_status,
              u.mobile, u.email
              FROM subscriptions s
              LEFT JOIN users u ON s.user_id = u.id
              WHERE s.razorpay_subscription_id IS NOT NULL 
              AND s.razorpay_subscription_id != ''
              ORDER BY s.id DESC";
    
    $stmt = $db->prepare($query);
    $stmt->execute();
    $subscriptions = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    if (empty($subscriptions)) {
        echo json_encode([
            'success' => true,
            'message' => 'No subscriptions to sync',
            'synced_count' => 0
        ]);
        exit;
    }
    
    // Fetch ALL recent payments from Razorpay once (to avoid multiple API calls)
    $allPayments = [];
    $paymentsResponse = razorpayApiRequest('payments?count=100', 'GET');
    if ($paymentsResponse['code'] === 200 && !empty($paymentsResponse['data']['items'])) {
        $allPayments = $paymentsResponse['data']['items'];
    }
    
    $syncedCount = 0;
    $errors = [];
    $results = [];
    
    foreach ($subscriptions as $sub) {
        $razorpaySubId = $sub['razorpay_subscription_id'];
        
        // Fetch subscription status from Razorpay
        $rzpResponse = razorpayApiRequest('subscriptions/' . $razorpaySubId, 'GET');
        
        if ($rzpResponse['code'] === 200) {
            $rzpData = $rzpResponse['data'];
            $newRazorpayStatus = $rzpData['status'] ?? null;
            
            if ($newRazorpayStatus) {
                // Find payment status for this user
                $latestPaymentStatus = null;
                
                if ($sub['mobile']) {
                    $userMobile = preg_replace('/[^0-9]/', '', $sub['mobile']);
                    $userMobileLast10 = substr($userMobile, -10);
                    
                    foreach ($allPayments as $payment) {
                        $paymentContact = $payment['contact'] ?? '';
                        $paymentContactClean = preg_replace('/[^0-9]/', '', $paymentContact);
                        $paymentContactLast10 = substr($paymentContactClean, -10);
                        
                        if ($paymentContactLast10 === $userMobileLast10) {
                            // Priority: refund_status > status
                            if (!empty($payment['refund_status']) && $payment['refund_status'] !== 'null') {
                                $latestPaymentStatus = $payment['refund_status'];
                            } else {
                                $latestPaymentStatus = $payment['status'];
                            }
                            break; // First match is most recent
                        }
                    }
                }
                
                // Update database
                $updateQuery = "UPDATE subscriptions SET razorpay_status = ?";
                $params = [$newRazorpayStatus];
                
                if ($latestPaymentStatus) {
                    $updateQuery .= ", last_payment_status = ?";
                    $params[] = $latestPaymentStatus;
                }
                
                $updateQuery .= " WHERE id = ?";
                $params[] = $sub['id'];
                
                $updateStmt = $db->prepare($updateQuery);
                $updateStmt->execute($params);
                
                $syncedCount++;
                $results[] = [
                    'id' => $sub['id'],
                    'razorpay_status' => $newRazorpayStatus,
                    'payment_status' => $latestPaymentStatus
                ];
            }
        } else {
            $errors[] = [
                'id' => $sub['id'],
                'error' => $rzpResponse['data']['error']['description'] ?? 'API error'
            ];
        }
    }
    
    echo json_encode([
        'success' => true,
        'message' => "Synced $syncedCount subscriptions",
        'synced_count' => $syncedCount,
        'total' => count($subscriptions),
        'results' => $results,
        'errors' => $errors
    ]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error syncing',
        'error' => $e->getMessage()
    ]);
}
?>
