<?php
/**
 * Public Feedback Submission API
 * Allows users to submit feedback without authentication
 */

require_once '../config/cors.php';
require_once '../config/database.php';

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

if ($method !== 'POST') {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed. Only POST is supported.'
    ]);
    exit;
}

try {
    $data = json_decode(file_get_contents('php://input'), true);

    // Validate required fields
    if (!isset($data['user_name']) || empty(trim($data['user_name']))) {
        throw new Exception('Name is required');
    }

    if (!isset($data['subject']) || empty(trim($data['subject']))) {
        throw new Exception('Subject is required');
    }

    if (!isset($data['message']) || empty(trim($data['message']))) {
        throw new Exception('Message is required');
    }

    $user_id = isset($data['user_id']) ? intval($data['user_id']) : null;
    $user_name = trim($data['user_name']);
    $user_email = isset($data['user_email']) ? trim($data['user_email']) : null;
    $user_mobile = isset($data['user_mobile']) ? trim($data['user_mobile']) : null;
    $subject = trim($data['subject']);
    $message = trim($data['message']);
    $rating = isset($data['rating']) ? intval($data['rating']) : null;
    $category = isset($data['category']) ? trim($data['category']) : 'general';

    // Validate rating
    if ($rating !== null && ($rating < 1 || $rating > 5)) {
        throw new Exception('Rating must be between 1 and 5');
    }

    // Validate category
    $valid_categories = ['general', 'bug', 'feature', 'complaint', 'suggestion'];
    if (!in_array($category, $valid_categories)) {
        $category = 'general';
    }

    // Validate email format if provided
    if ($user_email && !filter_var($user_email, FILTER_VALIDATE_EMAIL)) {
        throw new Exception('Invalid email format');
    }

    // Validate mobile format if provided
    if ($user_mobile && !preg_match('/^[0-9+\-\s()]+$/', $user_mobile)) {
        throw new Exception('Invalid mobile number format');
    }

    // Insert feedback
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
        'message' => 'Thank you for your feedback! We will review it soon.',
        'id' => $feedback_id
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    error_log('Feedback submission error: ' . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'Failed to submit feedback. Please try again later.'
    ]);
} catch (Exception $e) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
}
?>

