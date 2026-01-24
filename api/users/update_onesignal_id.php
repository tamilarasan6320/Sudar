<?php
/**
 * Update User's OneSignal Player ID
 * 
 * Called from Flutter app when OneSignal initializes
 * Stores player ID in database for targeted notifications
 * 
 * POST /api/users/update_onesignal_id.php
 * 
 * Body:
 * {
 *   "user_id": 123,
 *   "player_id": "abc123-def456-..."
 * }
 */

require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../auth/auth_middleware.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        $data = json_decode(file_get_contents('php://input'), true);
        
        $userId = isset($data['user_id']) ? intval($data['user_id']) : null;
        $playerId = trim($data['player_id'] ?? '');
        
        if (!$userId || empty($playerId)) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'User ID and Player ID are required'
            ]);
            exit;
        }
        
        $database = new Database();
        $db = $database->getConnection();

        // Enforce single-device session
        requireUserSession($db, (int)$userId);
        
        // Check if column exists, if not add it
        try {
            $checkStmt = $db->query("SHOW COLUMNS FROM users LIKE 'onesignal_player_id'");
            if ($checkStmt->rowCount() == 0) {
                $db->exec("ALTER TABLE users ADD COLUMN onesignal_player_id VARCHAR(255) NULL AFTER last_login");
                $db->exec("CREATE INDEX idx_onesignal_player_id ON users(onesignal_player_id)");
            }
        } catch (PDOException $e) {
            // Column might already exist, ignore
        }
        
        // Update user's OneSignal player ID
        $stmt = $db->prepare("
            UPDATE users 
            SET onesignal_player_id = ? 
            WHERE id = ?
        ");
        $stmt->execute([$playerId, $userId]);
        
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => 'OneSignal Player ID updated successfully'
        ]);
        
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Database error: ' . $e->getMessage()
        ]);
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Error: ' . $e->getMessage()
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed'
    ]);
}
?>

