<?php
/**
 * Create Admin Users Table
 * Run this once to set up admin authentication
 */

require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

try {
    // Create admin_users table
    $query = "CREATE TABLE IF NOT EXISTS admin_users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(50) UNIQUE NOT NULL,
        email VARCHAR(100),
        password VARCHAR(255) NOT NULL,
        role VARCHAR(20) DEFAULT 'admin',
        is_active TINYINT(1) DEFAULT 1,
        last_login DATETIME NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    
    $db->exec($query);
    echo "✅ Admin users table created successfully!\n";
    
    // Check if default admin exists
    $checkQuery = "SELECT COUNT(*) as count FROM admin_users WHERE username = 'admin'";
    $stmt = $db->query($checkQuery);
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($result['count'] == 0) {
        // Create default admin user
        // Password: admin123 (hashed with password_hash)
        $hashedPassword = password_hash('admin123', PASSWORD_DEFAULT);
        
        $insertQuery = "INSERT INTO admin_users (username, email, password, role) 
                        VALUES ('admin', 'admin@tnpscmocktest.com', :password, 'admin')";
        $stmt = $db->prepare($insertQuery);
        $stmt->bindParam(':password', $hashedPassword);
        $stmt->execute();
        
        echo "✅ Default admin user created!\n";
        echo "   Username: admin\n";
        echo "   Password: admin123\n";
        echo "   ⚠️  Please change the password after first login!\n";
    } else {
        echo "ℹ️  Default admin user already exists.\n";
    }
    
    echo "\n✅ Setup complete! Admin authentication is ready.\n";
    
} catch (PDOException $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>

