<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
// Temporarily disable authentication for testing - TODO: Re-enable in production
// require_once '../../admin/auth/middleware.php';
session_start();

$database = new Database();
$db = $database->getConnection();

// Create feedback table if it doesn't exist
try {
    $create_table = "CREATE TABLE IF NOT EXISTS feedback (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NULL,
        user_name VARCHAR(100) NOT NULL,
        user_email VARCHAR(100) NULL,
        user_mobile VARCHAR(15) NULL,
        subject VARCHAR(255) NOT NULL,
        message TEXT NOT NULL,
        rating INT NULL COMMENT 'Rating from 1 to 5',
        category VARCHAR(50) DEFAULT 'general' COMMENT 'general, bug, feature, complaint, suggestion',
        status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending, reviewed, resolved, closed',
        admin_response TEXT NULL,
        admin_response_by INT NULL,
        admin_response_at TIMESTAMP NULL,
        is_archived BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
        INDEX idx_user_id (user_id),
        INDEX idx_status (status),
        INDEX idx_category (category),
        INDEX idx_created_at (created_at),
        INDEX idx_is_archived (is_archived)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    $db->exec($create_table);
} catch (PDOException $e) {
    // Table might already exist, ignore
}

$method = $_SERVER['REQUEST_METHOD'];

// Add error reporting for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0); // Don't display, but log
ini_set('log_errors', 1);

try {
    switch ($method) {
        case 'GET':
            $id = isset($_GET['id']) ? intval($_GET['id']) : null;
            $status = isset($_GET['status']) ? $_GET['status'] : null;
            $category = isset($_GET['category']) ? $_GET['category'] : null;
            // Handle is_archived parameter - can be string "true"/"false" or boolean
            $is_archived = false;
            if (isset($_GET['is_archived'])) {
                $is_archived_val = $_GET['is_archived'];
                if (is_string($is_archived_val)) {
                    $is_archived = strtolower($is_archived_val) === 'true' || $is_archived_val === '1';
                } else {
                    $is_archived = (bool)$is_archived_val;
                }
            }
            $limit = isset($_GET['limit']) ? intval($_GET['limit']) : 50;
            $offset = isset($_GET['offset']) ? intval($_GET['offset']) : 0;
            $search = isset($_GET['search']) ? $_GET['search'] : null;

            if ($id) {
                // Get single feedback
                $query = "SELECT f.*, u.name as user_name_full, u.email as user_email_full, u.mobile as user_mobile_full
                          FROM feedback f
                          LEFT JOIN users u ON f.user_id = u.id
                          WHERE f.id = ?";
                $stmt = $db->prepare($query);
                $stmt->execute([$id]);
                $feedback = $stmt->fetch(PDO::FETCH_ASSOC);

                if (!$feedback) {
                    throw new Exception('Feedback not found');
                }

                echo json_encode([
                    'success' => true,
                    'feedback' => $feedback
                ]);
            } else {
                // Get list of feedback - BULLETPROOF VERSION
                $is_archived_int = $is_archived ? 1 : 0;
                $limit_int = max(1, intval($limit));
                $offset_int = max(0, intval($offset));
                
                // Build WHERE conditions and parameters
                $where_parts = [];
                $params = [];
                
                // Always include is_archived
                $where_parts[] = "is_archived = ?";
                $params[] = $is_archived_int;
                
                if ($status && trim($status) !== '') {
                    $where_parts[] = "status = ?";
                    $params[] = $status;
                }
                
                if ($category && trim($category) !== '') {
                    $where_parts[] = "category = ?";
                    $params[] = $category;
                }
                
                if ($search && trim($search) !== '') {
                    $where_parts[] = "(subject LIKE ? OR message LIKE ? OR user_name LIKE ? OR user_email LIKE ?)";
                    $search_param = "%{$search}%";
                    $params[] = $search_param;
                    $params[] = $search_param;
                    $params[] = $search_param;
                    $params[] = $search_param;
                }
                
                $where_clause = implode(" AND ", $where_parts);
                
                // Main query - count placeholders to verify
                $query = "SELECT * FROM feedback WHERE $where_clause ORDER BY created_at DESC LIMIT $limit_int OFFSET $offset_int";
                $placeholder_count = substr_count($query, '?');
                
                error_log('Main query: ' . $query);
                error_log('Placeholders: ' . $placeholder_count . ', Params: ' . count($params));
                
                if ($placeholder_count !== count($params)) {
                    throw new Exception("Parameter mismatch: Query has $placeholder_count placeholders but " . count($params) . " parameters provided");
                }
                
                $stmt = $db->prepare($query);
                $stmt->execute($params);
                $feedback_list = $stmt->fetchAll(PDO::FETCH_ASSOC);

                // Get total count - rebuild params array to match
                $count_where_parts = [];
                $count_params = [];
                
                $count_where_parts[] = "is_archived = ?";
                $count_params[] = $is_archived_int;
                
                if ($status && trim($status) !== '') {
                    $count_where_parts[] = "status = ?";
                    $count_params[] = $status;
                }
                
                if ($category && trim($category) !== '') {
                    $count_where_parts[] = "category = ?";
                    $count_params[] = $category;
                }
                
                if ($search && trim($search) !== '') {
                    $count_where_parts[] = "(subject LIKE ? OR message LIKE ? OR user_name LIKE ? OR user_email LIKE ?)";
                    $search_param = "%{$search}%";
                    $count_params[] = $search_param;
                    $count_params[] = $search_param;
                    $count_params[] = $search_param;
                    $count_params[] = $search_param;
                }
                
                $count_where_clause = implode(" AND ", $count_where_parts);
                $count_query = "SELECT COUNT(*) FROM feedback WHERE $count_where_clause";
                $count_placeholder_count = substr_count($count_query, '?');
                
                error_log('Count query: ' . $count_query);
                error_log('Count placeholders: ' . $count_placeholder_count . ', Count params: ' . count($count_params));
                
                if ($count_placeholder_count !== count($count_params)) {
                    throw new Exception("Count parameter mismatch: Query has $count_placeholder_count placeholders but " . count($count_params) . " parameters provided");
                }
                
                $count_stmt = $db->prepare($count_query);
                $count_stmt->execute($count_params);
                $total_count = $count_stmt->fetchColumn();

                // Get statistics - use direct integer
                try {
                    $stats_query = "SELECT 
                        COUNT(*) as total,
                        SUM(CASE WHEN status = 'pending' THEN 1 ELSE 0 END) as pending,
                        SUM(CASE WHEN status = 'reviewed' THEN 1 ELSE 0 END) as reviewed,
                        SUM(CASE WHEN status = 'resolved' THEN 1 ELSE 0 END) as resolved,
                        SUM(CASE WHEN status = 'closed' THEN 1 ELSE 0 END) as closed
                        FROM feedback WHERE is_archived = " . intval($is_archived_int);
                    $stats_stmt = $db->query($stats_query);
                    $stats = $stats_stmt->fetch(PDO::FETCH_ASSOC);
                    
                    if (!$stats) {
                        $stats = [
                            'total' => 0,
                            'pending' => 0,
                            'reviewed' => 0,
                            'resolved' => 0,
                            'closed' => 0
                        ];
                    }
                } catch (PDOException $e) {
                    error_log('PDO Error in stats query: ' . $e->getMessage());
                    $stats = [
                        'total' => count($feedback_list),
                        'pending' => 0,
                        'reviewed' => 0,
                        'resolved' => 0,
                        'closed' => 0
                    ];
                }

                echo json_encode([
                    'success' => true,
                    'feedback' => $feedback_list,
                    'count' => count($feedback_list),
                    'total_count' => (int)$total_count,
                    'stats' => $stats
                ]);
            }
            break;

        case 'POST':
            $data = json_decode(file_get_contents('php://input'), true);

            if (!isset($data['user_name']) || !isset($data['subject']) || !isset($data['message'])) {
                throw new Exception('Name, subject, and message are required');
            }

            $user_id = isset($data['user_id']) ? intval($data['user_id']) : null;
            $user_name = trim($data['user_name']);
            $user_email = isset($data['user_email']) ? trim($data['user_email']) : null;
            $user_mobile = isset($data['user_mobile']) ? trim($data['user_mobile']) : null;
            $subject = trim($data['subject']);
            $message = trim($data['message']);
            $rating = isset($data['rating']) ? intval($data['rating']) : null;
            $category = isset($data['category']) ? $data['category'] : 'general';

            // Validate rating
            if ($rating !== null && ($rating < 1 || $rating > 5)) {
                throw new Exception('Rating must be between 1 and 5');
            }

            // Validate category
            $valid_categories = ['general', 'bug', 'feature', 'complaint', 'suggestion'];
            if (!in_array($category, $valid_categories)) {
                $category = 'general';
            }

            $query = "INSERT INTO feedback (user_id, user_name, user_email, user_mobile, subject, message, rating, category)
                      VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            $stmt = $db->prepare($query);
            $stmt->execute([
                $user_id,
                $user_name,
                $user_email,
                $user_mobile,
                $subject,
                $message,
                $rating,
                $category
            ]);

            $feedback_id = $db->lastInsertId();

            echo json_encode([
                'success' => true,
                'message' => 'Feedback submitted successfully',
                'id' => $feedback_id
            ]);
            break;

        case 'PUT':
            $data = json_decode(file_get_contents('php://input'), true);
            $id = isset($_GET['id']) ? intval($_GET['id']) : (isset($data['id']) ? intval($data['id']) : null);

            if (!$id) {
                throw new Exception('Feedback ID is required');
            }

            // Check if feedback exists
            $check_query = "SELECT id FROM feedback WHERE id = ?";
            $check_stmt = $db->prepare($check_query);
            $check_stmt->execute([$id]);
            if (!$check_stmt->fetch()) {
                throw new Exception('Feedback not found');
            }

            $updates = [];
            $params = [];

            if (isset($data['status'])) {
                $valid_statuses = ['pending', 'reviewed', 'resolved', 'closed'];
                if (in_array($data['status'], $valid_statuses)) {
                    $updates[] = "status = ?";
                    $params[] = $data['status'];
                }
            }

            if (isset($data['admin_response'])) {
                $updates[] = "admin_response = ?";
                $updates[] = "admin_response_by = ?";
                $updates[] = "admin_response_at = NOW()";
                $params[] = trim($data['admin_response']);
                $params[] = isset($_SESSION['admin_id']) ? $_SESSION['admin_id'] : null;
            }

            if (isset($data['is_archived'])) {
                $updates[] = "is_archived = ?";
                $params[] = filter_var($data['is_archived'], FILTER_VALIDATE_BOOLEAN) ? 1 : 0;
            }

            if (empty($updates)) {
                throw new Exception('No valid fields to update');
            }

            $params[] = $id;
            $query = "UPDATE feedback SET " . implode(", ", $updates) . " WHERE id = ?";
            $stmt = $db->prepare($query);
            $stmt->execute($params);

            echo json_encode([
                'success' => true,
                'message' => 'Feedback updated successfully'
            ]);
            break;

        case 'DELETE':
            $id = isset($_GET['id']) ? intval($_GET['id']) : null;

            if (!$id) {
                throw new Exception('Feedback ID is required');
            }

            // Soft delete by archiving
            $query = "UPDATE feedback SET is_archived = TRUE WHERE id = ?";
            $stmt = $db->prepare($query);
            $stmt->execute([$id]);

            echo json_encode([
                'success' => true,
                'message' => 'Feedback archived successfully'
            ]);
            break;

        default:
            throw new Exception('Method not allowed');
    }
} catch (PDOException $e) {
    http_response_code(500);
    error_log('Feedback API Database Error: ' . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage(),
        'error' => $e->getMessage(),
        'debug' => [
            'file' => $e->getFile(),
            'line' => $e->getLine()
        ]
    ]);
} catch (Exception $e) {
    http_response_code(400);
    error_log('Feedback API Error: ' . $e->getMessage());
    error_log('Stack trace: ' . $e->getTraceAsString());
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage(),
        'error' => $e->getMessage(),
        'debug' => [
            'file' => $e->getFile(),
            'line' => $e->getLine(),
            'trace' => $e->getTraceAsString()
        ]
    ]);
}
?>

