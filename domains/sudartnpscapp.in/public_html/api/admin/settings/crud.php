<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication

$database = new Database();
$db = $database->getConnection();

// Create settings table if it doesn't exist
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

$method = $_SERVER['REQUEST_METHOD'];

try {
    switch ($method) {
        case 'GET':
            // Get all settings or a specific setting
            $key = isset($_GET['key']) ? $_GET['key'] : null;
            
            if ($key) {
                $query = "SELECT * FROM app_settings WHERE setting_key = ?";
                $stmt = $db->prepare($query);
                $stmt->execute([$key]);
                $setting = $stmt->fetch(PDO::FETCH_ASSOC);
                
                if ($setting) {
                    echo json_encode([
                        'success' => true,
                        'setting' => $setting
                    ]);
                } else {
                    http_response_code(404);
                    echo json_encode([
                        'success' => false,
                        'message' => 'Setting not found'
                    ]);
                }
            } else {
                $query = "SELECT * FROM app_settings ORDER BY setting_key ASC";
                $stmt = $db->prepare($query);
                $stmt->execute();
                $settings = $stmt->fetchAll(PDO::FETCH_ASSOC);
                
                echo json_encode([
                    'success' => true,
                    'settings' => $settings,
                    'count' => count($settings)
                ]);
            }
            break;
            
        case 'POST':
            // Create or update a setting
            $data = json_decode(file_get_contents('php://input'), true);
            
            if (!isset($data['setting_key'])) {
                throw new Exception('Setting key is required');
            }
            
            $key = $data['setting_key'];
            $value = isset($data['setting_value']) ? $data['setting_value'] : '';
            $type = isset($data['setting_type']) ? $data['setting_type'] : 'text';
            $description = isset($data['description']) ? $data['description'] : '';
            
            // Check if setting exists
            $check_query = "SELECT id FROM app_settings WHERE setting_key = ?";
            $check_stmt = $db->prepare($check_query);
            $check_stmt->execute([$key]);
            
            if ($check_stmt->rowCount() > 0) {
                // Update existing
                $update_query = "UPDATE app_settings 
                                SET setting_value = ?, setting_type = ?, description = ?
                                WHERE setting_key = ?";
                $update_stmt = $db->prepare($update_query);
                $update_stmt->execute([$value, $type, $description, $key]);
                
                echo json_encode([
                    'success' => true,
                    'message' => 'Setting updated successfully'
                ]);
            } else {
                // Create new
                $insert_query = "INSERT INTO app_settings (setting_key, setting_value, setting_type, description)
                                VALUES (?, ?, ?, ?)";
                $insert_stmt = $db->prepare($insert_query);
                $insert_stmt->execute([$key, $value, $type, $description]);
                
                echo json_encode([
                    'success' => true,
                    'message' => 'Setting created successfully',
                    'id' => $db->lastInsertId()
                ]);
            }
            break;
            
        case 'PUT':
            // Update a setting
            $data = json_decode(file_get_contents('php://input'), true);
            
            if (!isset($data['setting_key'])) {
                throw new Exception('Setting key is required');
            }
            
            $key = $data['setting_key'];
            $updates = [];
            $params = [];
            
            if (isset($data['setting_value'])) {
                $updates[] = "setting_value = ?";
                $params[] = $data['setting_value'];
            }
            if (isset($data['setting_type'])) {
                $updates[] = "setting_type = ?";
                $params[] = $data['setting_type'];
            }
            if (isset($data['description'])) {
                $updates[] = "description = ?";
                $params[] = $data['description'];
            }
            
            if (empty($updates)) {
                throw new Exception('No fields to update');
            }
            
            $params[] = $key;
            $query = "UPDATE app_settings SET " . implode(", ", $updates) . " WHERE setting_key = ?";
            $stmt = $db->prepare($query);
            $stmt->execute($params);
            
            echo json_encode([
                'success' => true,
                'message' => 'Setting updated successfully'
            ]);
            break;
            
        case 'DELETE':
            // Delete a setting
            $data = json_decode(file_get_contents('php://input'), true);
            
            if (!isset($data['setting_key'])) {
                throw new Exception('Setting key is required');
            }
            
            $query = "DELETE FROM app_settings WHERE setting_key = ?";
            $stmt = $db->prepare($query);
            $stmt->execute([$data['setting_key']]);
            
            echo json_encode([
                'success' => true,
                'message' => 'Setting deleted successfully'
            ]);
            break;
            
        default:
            throw new Exception('Method not supported');
    }
    
} catch (Exception $e) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
}
?>

