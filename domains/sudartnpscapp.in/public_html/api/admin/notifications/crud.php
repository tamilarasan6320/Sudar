<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication

$database = new Database();
$db = $database->getConnection();

// Create notifications table if it doesn't exist
try {
    $create_table = "CREATE TABLE IF NOT EXISTS notifications (
        id INT(11) AUTO_INCREMENT PRIMARY KEY,
        title VARCHAR(255) NOT NULL,
        message TEXT NOT NULL,
        type VARCHAR(50) DEFAULT 'info', -- info, success, warning, error
        target_audience VARCHAR(50) DEFAULT 'all', -- all, specific_user, specific_group
        target_user_id INT(11) NULL,
        is_read BOOLEAN DEFAULT FALSE,
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        expires_at DATETIME NULL,
        created_by INT(11) NULL,
        INDEX idx_user_id (target_user_id),
        INDEX idx_is_active (is_active),
        INDEX idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    $db->exec($create_table);
} catch (PDOException $e) {
    // Table might already exist, ignore
}

$method = $_SERVER['REQUEST_METHOD'];

try {
    switch ($method) {
        case 'GET':
            $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : null;
            $is_read = isset($_GET['is_read']) ? filter_var($_GET['is_read'], FILTER_VALIDATE_BOOLEAN) : null;
            $limit = isset($_GET['limit']) ? intval($_GET['limit']) : 50;
            $offset = isset($_GET['offset']) ? intval($_GET['offset']) : 0;

            $where_conditions = ["is_active = 1"];
            $params = [];

            if ($user_id) {
                $where_conditions[] = "(target_audience = 'all' OR (target_audience = 'specific_user' AND target_user_id = ?))";
                $params[] = $user_id;
            }

            if ($is_read !== null) {
                if ($user_id) {
                    // For user-specific notifications, check read status
                    $where_conditions[] = "id IN (
                        SELECT notification_id FROM notification_reads WHERE user_id = ? AND is_read = ?
                    )";
                    $params[] = $user_id;
                    $params[] = $is_read ? 1 : 0;
                }
            }

            // Check expiration
            $where_conditions[] = "(expires_at IS NULL OR expires_at > NOW())";

            $where_clause = implode(" AND ", $where_conditions);
            $query = "SELECT * FROM notifications WHERE $where_clause ORDER BY created_at DESC LIMIT ? OFFSET ?";
            $params[] = $limit;
            $params[] = $offset;

            $stmt = $db->prepare($query);
            $stmt->execute($params);
            $notifications = $stmt->fetchAll(PDO::FETCH_ASSOC);

            // Get total count
            $count_query = "SELECT COUNT(*) FROM notifications WHERE $where_clause";
            $count_stmt = $db->prepare($count_query);
            $count_params = array_slice($params, 0, -2); // Remove limit and offset
            $count_stmt->execute($count_params);
            $total_count = $count_stmt->fetchColumn();

            echo json_encode([
                'success' => true,
                'notifications' => $notifications,
                'count' => count($notifications),
                'total_count' => (int)$total_count
            ]);
            break;

        case 'POST':
            $data = json_decode(file_get_contents('php://input'), true);

            if (!isset($data['title']) || !isset($data['message'])) {
                throw new Exception('Title and message are required');
            }

            $title = $data['title'];
            $message = $data['message'];
            $type = isset($data['type']) ? $data['type'] : 'info';
            $target_audience = isset($data['target_audience']) ? $data['target_audience'] : 'all';
            $target_user_id = isset($data['target_user_id']) ? intval($data['target_user_id']) : null;
            $expires_at = isset($data['expires_at']) ? $data['expires_at'] : null;
            $created_by = isset($_SESSION['admin_id']) ? $_SESSION['admin_id'] : null;

            $query = "INSERT INTO notifications (title, message, type, target_audience, target_user_id, expires_at, created_by)
                     VALUES (?, ?, ?, ?, ?, ?, ?)";
            $stmt = $db->prepare($query);
            $stmt->execute([$title, $message, $type, $target_audience, $target_user_id, $expires_at, $created_by]);

            echo json_encode([
                'success' => true,
                'message' => 'Notification created successfully',
                'id' => $db->lastInsertId()
            ]);
            break;

        case 'PUT':
            $data = json_decode(file_get_contents('php://input'), true);

            if (!isset($data['id'])) {
                throw new Exception('Notification ID is required');
            }

            $id = intval($data['id']);
            $updates = [];
            $params = [];

            if (isset($data['title'])) {
                $updates[] = "title = ?";
                $params[] = $data['title'];
            }
            if (isset($data['message'])) {
                $updates[] = "message = ?";
                $params[] = $data['message'];
            }
            if (isset($data['type'])) {
                $updates[] = "type = ?";
                $params[] = $data['type'];
            }
            if (isset($data['is_active'])) {
                $updates[] = "is_active = ?";
                $params[] = $data['is_active'] ? 1 : 0;
            }
            if (isset($data['expires_at'])) {
                $updates[] = "expires_at = ?";
                $params[] = $data['expires_at'];
            }

            if (empty($updates)) {
                throw new Exception('No fields to update');
            }

            $params[] = $id;
            $query = "UPDATE notifications SET " . implode(", ", $updates) . " WHERE id = ?";
            $stmt = $db->prepare($query);
            $stmt->execute($params);

            echo json_encode([
                'success' => true,
                'message' => 'Notification updated successfully'
            ]);
            break;

        case 'DELETE':
            $data = json_decode(file_get_contents('php://input'), true);

            if (!isset($data['id'])) {
                throw new Exception('Notification ID is required');
            }

            // Soft delete
            $query = "UPDATE notifications SET is_active = 0 WHERE id = ?";
            $stmt = $db->prepare($query);
            $stmt->execute([$data['id']]);

            echo json_encode([
                'success' => true,
                'message' => 'Notification deleted successfully'
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

