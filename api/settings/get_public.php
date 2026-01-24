<?php
require_once '../config/cors.php';
require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

// Create settings tabl if it doesn't exist
try {
    $create_table = "CREATE TABLE IF NOT EXISTS app_settings (
        id INT(11) AUTO_INCREMENT PRIMARY KEY,
        setting_key VARCHAR(100) NOT NULL UNIQUE,
        setting_value TEXT,
        setting_type VARCHAR(50) DEFAULT 'text',
        description TEXT,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    $db->exec($create_table);
} catch (PDOException $e) {
    // Table might already exist, ignore
}

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $key = isset($_GET['key']) ? $_GET['key'] : null;
    
    try {
        if ($key) {
            // Get specific setting
            $query = "SELECT setting_value, setting_type FROM app_settings WHERE setting_key = ?";
            $stmt = $db->prepare($query);
            $stmt->execute([$key]);
            $setting = $stmt->fetch(PDO::FETCH_ASSOC);
            
            if ($setting) {
                $value = $setting['setting_value'];
                
                // If it's JSON type, decode it
                if ($setting['setting_type'] === 'json') {
                    $value = json_decode($value, true);
                }
                
                http_response_code(200);
                echo json_encode([
                    'success' => true,
                    'key' => $key,
                    'value' => $value
                ]);
            } else {
                http_response_code(404);
                echo json_encode([
                    'success' => false,
                    'message' => 'Setting not found'
                ]);
            }
        } else {
            // Get all public settings
            $query = "SELECT setting_key, setting_value, setting_type FROM app_settings 
                     WHERE setting_key IN ('about', 'privacy_policy', 'terms_conditions')";
            $stmt = $db->query($query);
            $settings = $stmt->fetchAll(PDO::FETCH_ASSOC);
            
            $result = [];
            foreach ($settings as $setting) {
                $value = $setting['setting_value'];
                
                // If it's JSON type, decode it
                if ($setting['setting_type'] === 'json') {
                    $value = json_decode($value, true);
                }
                
                $result[$setting['setting_key']] = $value;
            }
            
            http_response_code(200);
            echo json_encode([
                'success' => true,
                'settings' => $result
            ]);
        }
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Database error: ' . $e->getMessage()
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed.']);
}
?>

