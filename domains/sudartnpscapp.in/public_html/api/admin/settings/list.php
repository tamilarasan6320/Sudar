<?php
/**
 * List All App Settings API (Admin Only)
 * 
 * GET /api/admin/settings/list.php
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

try {
    $database = new Database();
    $db = $database->getConnection();
    
    // Create settings table if not exists
    $db->exec("CREATE TABLE IF NOT EXISTS app_settings (
        id INT AUTO_INCREMENT PRIMARY KEY,
        setting_key VARCHAR(100) UNIQUE NOT NULL,
        setting_value TEXT,
        setting_type VARCHAR(50) DEFAULT 'string',
        is_public TINYINT(1) DEFAULT 1,
        description VARCHAR(255),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    )");
    
    // Insert default settings if not exist
    $defaultSettings = [
        ['subscription_promo_image', '', 'url', 1, 'Promotional image for subscription offer page'],
        ['subscription_video_url', '', 'url', 1, 'Video URL for subscription offer page (YouTube/MP4)'],
        ['subscription_video_thumbnail', '', 'url', 1, 'Thumbnail image for subscription video'],
        ['app_maintenance_mode', '0', 'boolean', 1, 'Enable/disable app maintenance mode'],
        ['app_update_required', '0', 'boolean', 1, 'Force app update'],
        ['app_min_version', '1.0.0', 'string', 1, 'Minimum app version required']
    ];
    
    $insertStmt = $db->prepare("INSERT IGNORE INTO app_settings (setting_key, setting_value, setting_type, is_public, description) VALUES (?, ?, ?, ?, ?)");
    foreach ($defaultSettings as $setting) {
        $insertStmt->execute($setting);
    }
    
    // Get all settings
    $stmt = $db->query("SELECT * FROM app_settings ORDER BY setting_key");
    $settings = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'success' => true,
        'settings' => $settings,
        'count' => count($settings)
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error fetching settings',
        'error' => $e->getMessage()
    ]);
}
?>
