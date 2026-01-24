<?php
/**
 * Feedback System Setup Script
 * Run this file to set up the feedback system
 */

require_once '../config/database.php';

echo "==============================================\n";
echo "   FEEDBACK SYSTEM SETUP\n";
echo "==============================================\n\n";

// Step 1: Create Database Connection
echo "Step 1: Connecting to database...\n";
$database = new Database();
$conn = $database->getConnection();

if (!$conn) {
    die("❌ Failed to connect to database.\n");
}
echo "✓ Connected to database successfully!\n\n";

// Step 2: Create Feedback Table
echo "Step 2: Creating feedback table...\n";
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
    echo "✓ Feedback table created successfully!\n\n";
} catch (PDOException $e) {
    if (strpos($e->getMessage(), 'already exists') !== false) {
        echo "ℹ Feedback table already exists.\n\n";
    } else {
        die("❌ Error creating feedback table: " . $e->getMessage() . "\n");
    }
}

// Step 3: Verify API Files
echo "Step 3: Verifying API files...\n";
$required_files = [
    '../admin/feedback/crud.php' => 'Admin Feedback API',
    '../feedback/submit.php' => 'Public Feedback API'
];

$all_files_exist = true;
foreach ($required_files as $file => $description) {
    if (file_exists($file)) {
        echo "✓ {$description} exists\n";
    } else {
        echo "❌ {$description} NOT FOUND: {$file}\n";
        $all_files_exist = false;
    }
}

if (!$all_files_exist) {
    echo "\n⚠ Some API files are missing. Please check your file structure.\n";
} else {
    echo "\n✓ All API files are in place!\n";
}

echo "\n==============================================\n";
echo "   SETUP COMPLETE!\n";
echo "==============================================\n\n";
echo "Next steps:\n";
echo "1. Access admin panel and click 'Feedback' in sidebar\n";
echo "2. Test by submitting feedback via API or admin panel\n";
echo "3. Verify feedback appears in the admin panel\n\n";
echo "API Endpoints:\n";
echo "- Admin: /api/admin/feedback/crud.php\n";
echo "- Public: /api/feedback/submit.php\n\n";
?>

