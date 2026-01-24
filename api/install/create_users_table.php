<?php
/**
 * Quick Fix: Create Users Table
 * Run this if you get "Table 'users' doesn't exist" error
 */

require_once '../config/database.php';

$database = new Database();
$conn = $database->getConnection();

try {
    // Create users table
    $sql_users = "CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        mobile VARCHAR(15) UNIQUE NOT NULL,
        email VARCHAR(100),
        age INT,
        district VARCHAR(50),
        education VARCHAR(100),
        profile_pic VARCHAR(255),
        language VARCHAR(10) DEFAULT 'en',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        last_login TIMESTAMP NULL,
        is_active TINYINT(1) DEFAULT 1,
        INDEX idx_mobile (mobile),
        INDEX idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

    $conn->exec($sql_users);
    
    echo json_encode([
        'success' => true,
        'message' => 'Users table created successfully!'
    ]);

} catch(PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error creating users table: ' . $e->getMessage()
    ]);
}
?>

