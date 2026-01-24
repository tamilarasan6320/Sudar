<?php
/**
 * Check Session API
 * =================
 * Validates if this device is still the active device for the account.
 *
 * Method: POST
 * Headers:
 * - Authorization: Bearer <session_token>
 * - X-Device-Id: <device_id>
 * Body:
 * { "user_id": 123 }
 */

require_once '../config/cors.php';
require_once '../config/database.php';
require_once './auth_middleware.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed. Use POST request.',
        'error_code' => 'METHOD_NOT_ALLOWED'
    ]);
    exit;
}

$data = json_decode(file_get_contents('php://input'));
if (empty($data->user_id)) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'user_id is required.',
        'error_code' => 'MISSING_FIELDS'
    ]);
    exit;
}

try {
    $database = new Database();
    $db = $database->getConnection();

    $userId = (int)$data->user_id;
    requireUserSession($db, $userId);

    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => 'Session is valid.'
    ]);
} catch (Exception $e) {
    error_log("Check Session Error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'An unexpected error occurred. Please try again.',
        'error_code' => 'SERVER_ERROR'
    ]);
}
?>

