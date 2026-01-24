<?php
/**
 * Get App Settings API
 * 
 * GET /api/settings/get.php?key=subscription_video
 * GET /api/settings/get.php (returns all public settings)
 */

require_once '../config/cors.php';
require_once '../config/database.php';

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
    
    // Insert default subscription video setting if not exists
    $db->exec("INSERT IGNORE INTO app_settings (setting_key, setting_value, setting_type, is_public, description) 
               VALUES ('subscription_video_url', '', 'url', 1, 'Video URL for subscription offer page')");
    
    $key = isset($_GET['key']) ? trim($_GET['key']) : null;
    
    if ($key) {
        // Get specific setting
        $stmt = $db->prepare("SELECT setting_key, setting_value, setting_type FROM app_settings WHERE setting_key = ? AND is_public = 1");
        $stmt->execute([$key]);
        $setting = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($setting) {
            echo json_encode([
                'success' => true,
                'key' => $setting['setting_key'],
                'value' => $setting['setting_value'],
                'type' => $setting['setting_type']
            ]);
        } else {
            echo json_encode([
                'success' => false,
                'message' => 'Setting not found'
            ]);
        }
    } else {
        // Get all public settings
        $stmt = $db->query("SELECT setting_key, setting_value, setting_type FROM app_settings WHERE is_public = 1");
        $settings = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $settings[$row['setting_key']] = [
                'value' => $row['setting_value'],
                'type' => $row['setting_type']
            ];
        }
        
        echo json_encode([
            'success' => true,
            'settings' => $settings
        ]);
    }
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error fetching settings',
        'error' => $e->getMessage()
    ]);
}
?>
