<?php
/**
 * Update App Settings API (Admin Only)
 * 
 * POST /api/admin/settings/update.php
 * Body: { "key": "subscription_video_url", "value": "https://..." }
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

if (empty($data->key)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Setting key is required']);
    exit;
}

try {
    $database = new Database();
    $db = $database->getConnection();
    
    $key = trim($data->key);
    $value = isset($data->value) ? trim($data->value) : '';
    
    // Check if setting exists
    $stmt = $db->prepare("SELECT id FROM app_settings WHERE setting_key = ?");
    $stmt->execute([$key]);
    
    if ($stmt->rowCount() > 0) {
        // Update existing
        $updateStmt = $db->prepare("UPDATE app_settings SET setting_value = ? WHERE setting_key = ?");
        $updateStmt->execute([$value, $key]);
    } else {
        // Insert new
        $insertStmt = $db->prepare("INSERT INTO app_settings (setting_key, setting_value, is_public) VALUES (?, ?, 1)");
        $insertStmt->execute([$key, $value]);
    }
    
    echo json_encode([
        'success' => true,
        'message' => 'Setting updated successfully',
        'key' => $key,
        'value' => $value
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error updating setting',
        'error' => $e->getMessage()
    ]);
}
?>
