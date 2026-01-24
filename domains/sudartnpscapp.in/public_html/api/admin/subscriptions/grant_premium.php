<?php
/**
 * Admin - Grant/Revoke Premium Access
 * 
 * Manually grant or revoke premium access for a user
 * 
 * POST /api/admin/subscriptions/grant_premium.php
 * Body: { 
 *   "user_id": 123, 
 *   "is_premium": true, 
 *   "days": 30 
 * }
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php';

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
    $is_premium = isset($data->is_premium) ? (bool)$data->is_premium : true;
    $days = isset($data->days) ? (int)$data->days : 30;
    
    $expires_at = $is_premium ? date('Y-m-d H:i:s', strtotime("+{$days} days")) : null;

    // Get user details first
    $userQuery = "SELECT name, mobile FROM users WHERE id = ?";
    $stmt = $db->prepare($userQuery);
    $stmt->execute([$data->user_id]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user) {
        http_response_code(404);
        echo json_encode(['success' => false, 'message' => 'User not found']);
        exit;
    }

    // Update premium status
    $query = "UPDATE users 
              SET is_premium = :is_premium, 
                  premium_expires_at = :expires_at
              WHERE id = :user_id";

    $stmt = $db->prepare($query);
    $stmt->bindParam(':is_premium', $is_premium, PDO::PARAM_BOOL);
    $stmt->bindParam(':expires_at', $expires_at);
    $stmt->bindParam(':user_id', $data->user_id, PDO::PARAM_INT);

    if ($stmt->execute()) {
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => $is_premium 
                ? "✅ Premium access granted to {$user['name']} for {$days} days" 
                : "❌ Premium access revoked for {$user['name']}",
            'data' => [
                'user_id' => $data->user_id,
                'user_name' => $user['name'],
                'is_premium' => $is_premium,
                'expires_at' => $expires_at,
                'days' => $days
            ]
        ]);
    } else {
        throw new Exception('Failed to update user');
    }

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error updating premium status',
        'error' => $e->getMessage()
    ]);
}
?>

