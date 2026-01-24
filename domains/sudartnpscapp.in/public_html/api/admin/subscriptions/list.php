<?php
/**
 * Admin - List Subscriptions
 * 
 * GET /api/admin/subscriptions/list.php
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php';
require_once '../../models/Subscription.php';

header('Content-Type: application/json');

$database = new Database();
$db = $database->getConnection();

try {
    $subscription = new Subscription($db);
    
    // Get stats
    $stats = $subscription->getStats();
    
    // Get all subscriptions
    $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 100;
    $offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;
    
    $subscriptions = $subscription->getAllForAdmin($limit, $offset);

    http_response_code(200);
    echo json_encode([
        'success' => true,
        'stats' => $stats,
        'subscriptions' => $subscriptions,
        'count' => count($subscriptions)
    ]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error loading subscriptions',
        'error' => $e->getMessage()
    ]);
}
?>

