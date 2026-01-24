<?php
/**
 * Create Feedback Table
 * This script creates the feedback table for storing user feedback
 */

require_once '../config/database.php';

$database = new Database();
$conn = $database->getConnection();

try {
    $sql = "CREATE TABLE IF NOT EXISTS feedback (
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
    
    $conn->exec($sql);
    echo json_encode([
        'success' => true,
        'message' => 'Feedback table created successfully'
    ]);
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error creating feedback table: ' . $e->getMessage()
    ]);
}
?>

