<?php
/**
 * Add verification_method column to users table
 * Run this once to update your database
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';

header('Content-Type: application/json');

try {
    $database = new Database();
    $db = $database->getConnection();
    
    // Check if column already exists
    $checkQuery = "SHOW COLUMNS FROM users LIKE 'verification_method'";
    $stmt = $db->prepare($checkQuery);
    $stmt->execute();
    
    if ($stmt->rowCount() > 0) {
        echo json_encode([
            'success' => true,
            'message' => 'Column verification_method already exists',
            'action' => 'none'
        ]);
        exit;
    }
    
    // Add the verification_method column
    $alterQuery = "ALTER TABLE users ADD COLUMN verification_method VARCHAR(20) DEFAULT 'otp' AFTER language";
    $db->exec($alterQuery);
    
    // Update existing users - mark all as 'otp' (since we don't have historical data)
    $updateQuery = "UPDATE users SET verification_method = 'otp' WHERE verification_method IS NULL";
    $db->exec($updateQuery);
    
    echo json_encode([
        'success' => true,
        'message' => 'Successfully added verification_method column to users table',
        'action' => 'column_added',
        'note' => 'All existing users marked as OTP verified. New registrations will track actual method.'
    ]);
    
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
}
?>
