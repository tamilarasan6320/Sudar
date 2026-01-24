<?php
/**
 * Complete Database Setup Script
 * This script will:
 * 1. Create the database if it doesn't exist
 * 2. Create all required tables
 * 3. Insert default admin user
 * 4. Insert sample data
 */

// Set correct path for config
$config_path = dirname(__DIR__) . '/config/database.php';
require_once $config_path;

echo "==============================================\n";
echo "   MOCK TEST DATABASE INSTALLATION\n";
echo "==============================================\n\n";

// Step 1: Create Database
echo "Step 1: Creating database...\n";
if (Database::createDatabase()) {
    echo "✓ Database 'mock_test_db' created successfully!\n\n";
} else {
    die("❌ Failed to create database. Please check MySQL connection.\n");
}

// Step 2: Connect to database
echo "Step 2: Connecting to database...\n";
$database = new Database();
$conn = $database->getConnection();

if (!$conn) {
    die("❌ Failed to connect to database.\n");
}
echo "✓ Connected to database successfully!\n\n";

try {
    echo "Step 3: Creating tables...\n";
    
    // 1. Users Table
    echo "  → Creating users table...\n";
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
        is_active BOOLEAN DEFAULT TRUE,
        INDEX idx_mobile (mobile),
        INDEX idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    $conn->exec($sql_users);

    // 2. OTP Table
    echo "  → Creating otp_verifications table...\n";
    $sql_otp = "CREATE TABLE IF NOT EXISTS otp_verifications (
        id INT AUTO_INCREMENT PRIMARY KEY,
        mobile VARCHAR(15) NOT NULL,
        otp VARCHAR(6) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        expires_at TIMESTAMP NULL DEFAULT NULL,
        is_verified BOOLEAN DEFAULT FALSE,
        attempts INT DEFAULT 0,
        INDEX idx_mobile (mobile),
        INDEX idx_expires_at (expires_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    $conn->exec($sql_otp);

    // 3. Exam Categories Table
    echo "  → Creating exam_categories table...\n";
    $sql_exam_categories = "CREATE TABLE IF NOT EXISTS exam_categories (
        id INT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        description TEXT,
        icon VARCHAR(50),
        is_active BOOLEAN DEFAULT TRUE,
        display_order INT DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    $conn->exec($sql_exam_categories);

    // 4. Test Categories Table
    echo "  → Creating test_categories table...\n";
    $sql_test_categories = "CREATE TABLE IF NOT EXISTS test_categories (
        id INT AUTO_INCREMENT PRIMARY KEY,
        exam_category_id INT NOT NULL,
        name VARCHAR(100) NOT NULL,
        description TEXT,
        icon VARCHAR(50),
        color VARCHAR(20),
        is_active BOOLEAN DEFAULT TRUE,
        display_order INT DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (exam_category_id) REFERENCES exam_categories(id) ON DELETE CASCADE,
        INDEX idx_exam_category (exam_category_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    $conn->exec($sql_test_categories);

    // 5. Question Sessions Table
    echo "  → Creating question_sessions table...\n";
    $sql_question_sessions = "CREATE TABLE IF NOT EXISTS question_sessions (
        id INT AUTO_INCREMENT PRIMARY KEY,
        test_category_id INT NOT NULL,
        name VARCHAR(200) NOT NULL,
        description TEXT,
        total_questions INT DEFAULT 0,
        duration INT DEFAULT 60,
        difficulty VARCHAR(20) DEFAULT 'medium',
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (test_category_id) REFERENCES test_categories(id) ON DELETE CASCADE,
        INDEX idx_test_category (test_category_id),
        INDEX idx_difficulty (difficulty)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    $conn->exec($sql_question_sessions);

    // 6. Questions Table
    echo "  → Creating questions table...\n";
    $sql_questions = "CREATE TABLE IF NOT EXISTS questions (
        id INT AUTO_INCREMENT PRIMARY KEY,
        session_id INT NOT NULL,
        question_en TEXT,
        question_ta TEXT,
        option_a_en VARCHAR(500),
        option_a_ta VARCHAR(500),
        option_b_en VARCHAR(500),
        option_b_ta VARCHAR(500),
        option_c_en VARCHAR(500),
        option_c_ta VARCHAR(500),
        option_d_en VARCHAR(500),
        option_d_ta VARCHAR(500),
        correct_answer CHAR(1) NOT NULL,
        explanation_en TEXT,
        explanation_ta TEXT,
        difficulty VARCHAR(20) DEFAULT 'medium',
        marks INT DEFAULT 1,
        negative_marks DECIMAL(3,2) DEFAULT 0.00,
        display_order INT DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (session_id) REFERENCES question_sessions(id) ON DELETE CASCADE,
        INDEX idx_session (session_id),
        INDEX idx_difficulty (difficulty)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    $conn->exec($sql_questions);

    // 7. Test Results Table
    echo "  → Creating test_results table...\n";
    $sql_test_results = "CREATE TABLE IF NOT EXISTS test_results (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        session_id INT NOT NULL,
        total_questions INT NOT NULL,
        attempted_questions INT DEFAULT 0,
        correct_answers INT DEFAULT 0,
        wrong_answers INT DEFAULT 0,
        unanswered INT DEFAULT 0,
        score DECIMAL(6,2) DEFAULT 0.00,
        percentage DECIMAL(5,2) DEFAULT 0.00,
        time_taken INT DEFAULT 0,
        rank INT,
        started_at TIMESTAMP NOT NULL,
        submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (session_id) REFERENCES question_sessions(id) ON DELETE CASCADE,
        INDEX idx_user (user_id),
        INDEX idx_session (session_id),
        INDEX idx_submitted_at (submitted_at),
        INDEX idx_score (score)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    $conn->exec($sql_test_results);

    // 8. User Answers Table
    echo "  → Creating user_answers table...\n";
    $sql_user_answers = "CREATE TABLE IF NOT EXISTS user_answers (
        id INT AUTO_INCREMENT PRIMARY KEY,
        result_id INT NOT NULL,
        question_id INT NOT NULL,
        user_answer CHAR(1),
        is_correct BOOLEAN DEFAULT FALSE,
        time_spent INT DEFAULT 0,
        marked_for_review BOOLEAN DEFAULT FALSE,
        FOREIGN KEY (result_id) REFERENCES test_results(id) ON DELETE CASCADE,
        FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
        INDEX idx_result (result_id),
        INDEX idx_question (question_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    $conn->exec($sql_user_answers);

    // 9. User Rankings Table
    echo "  → Creating user_rankings table...\n";
    $sql_rankings = "CREATE TABLE IF NOT EXISTS user_rankings (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        exam_category_id INT,
        total_tests INT DEFAULT 0,
        total_score DECIMAL(10,2) DEFAULT 0.00,
        average_score DECIMAL(5,2) DEFAULT 0.00,
        rank INT,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (exam_category_id) REFERENCES exam_categories(id) ON DELETE SET NULL,
        INDEX idx_user (user_id),
        INDEX idx_rank (rank),
        INDEX idx_exam_category (exam_category_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    $conn->exec($sql_rankings);

    // 10. Saved Tests Table
    echo "  → Creating saved_tests table...\n";
    $sql_saved_tests = "CREATE TABLE IF NOT EXISTS saved_tests (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        session_id INT NOT NULL,
        saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        FOREIGN KEY (session_id) REFERENCES question_sessions(id) ON DELETE CASCADE,
        UNIQUE KEY unique_user_session (user_id, session_id),
        INDEX idx_user (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    $conn->exec($sql_saved_tests);

    // 11. Admin Users Table
    echo "  → Creating admin_users table...\n";
    $sql_admin = "CREATE TABLE IF NOT EXISTS admin_users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(50) UNIQUE NOT NULL,
        password VARCHAR(255) NOT NULL,
        email VARCHAR(100),
        role VARCHAR(20) DEFAULT 'admin',
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        last_login TIMESTAMP NULL,
        INDEX idx_username (username)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    $conn->exec($sql_admin);

    echo "✓ All tables created successfully!\n\n";

    // Step 4: Insert default admin user
    echo "Step 4: Creating default admin user...\n";
    $admin_password = password_hash('admin123', PASSWORD_DEFAULT);
    $sql_default_admin = "INSERT IGNORE INTO admin_users (username, password, email, role)
                          VALUES ('admin', '$admin_password', 'admin@mocktest.com', 'super_admin')";
    $conn->exec($sql_default_admin);
    echo "✓ Default admin user created!\n";
    echo "  Username: admin\n";
    echo "  Password: admin123\n\n";

    // Step 5: Insert sample data
    echo "Step 5: Inserting sample data...\n";
    
    // Sample Exam Categories
    $sql_sample_exams = "INSERT IGNORE INTO exam_categories (id, name, description, icon, display_order) VALUES
        (1, 'TNPSC Group 1', 'Tamil Nadu Public Service Commission Group 1 Exams', 'users', 1),
        (2, 'TNPSC Group 2', 'Tamil Nadu Public Service Commission Group 2 Exams', 'users', 2),
        (3, 'TNPSC Group 4', 'Tamil Nadu Public Service Commission Group 4 Exams', 'users', 3),
        (4, 'TNUSRB', 'Tamil Nadu Uniformed Services Recruitment Board', 'shield', 4)";
    $conn->exec($sql_sample_exams);
    
    // Sample Test Categories
    $sql_sample_tests = "INSERT IGNORE INTO test_categories (id, exam_category_id, name, description, color, display_order) VALUES
        (1, 1, 'Tamil Language', 'Tamil Language and Grammar', '#FF6B6B', 1),
        (2, 1, 'General English', 'English Grammar and Comprehension', '#4ECDC4', 2),
        (3, 1, 'General Science', 'Physics, Chemistry, Biology', '#45B7D1', 3),
        (4, 1, 'Indian History', 'Ancient, Medieval and Modern History', '#96CEB4', 4),
        (5, 1, 'Geography', 'Indian and World Geography', '#FFEAA7', 5),
        (6, 2, 'Tamil Language', 'Tamil Language for Group 2', '#FF6B6B', 1),
        (7, 2, 'General Studies', 'General Studies for Group 2', '#DDA15E', 2),
        (8, 3, 'General Tamil', 'General Tamil for Group 4', '#FF6B6B', 1),
        (9, 3, 'General English', 'General English for Group 4', '#4ECDC4', 2),
        (10, 3, 'Aptitude', 'Numerical and Reasoning Aptitude', '#B392AC', 3)";
    $conn->exec($sql_sample_tests);
    
    // Sample Question Sessions
    $sql_sample_sessions = "INSERT IGNORE INTO question_sessions (id, test_category_id, name, description, total_questions, duration, difficulty) VALUES
        (1, 1, 'Tamil Basics - Session 1', 'Basic Tamil Grammar', 10, 15, 'easy'),
        (2, 2, 'English Grammar - Session 1', 'Basic English Grammar', 10, 15, 'easy'),
        (3, 3, 'General Science - Session 1', 'Physics Fundamentals', 15, 20, 'medium'),
        (4, 4, 'Indian History - Session 1', 'Ancient India', 10, 15, 'easy'),
        (5, 5, 'Geography Basics', 'World Geography', 10, 15, 'easy')";
    $conn->exec($sql_sample_sessions);
    
    // Sample Users
    $sql_sample_users = "INSERT IGNORE INTO users (id, name, mobile, email, age, district, education, language) VALUES
        (1, 'Rajesh Kumar', '9876543210', 'rajesh@example.com', 25, 'Chennai', 'B.E Computer Science', 'ta'),
        (2, 'Priya Devi', '9876543211', 'priya@example.com', 23, 'Coimbatore', 'B.Sc Mathematics', 'ta'),
        (3, 'Arun Kumar', '9876543212', 'arun@example.com', 27, 'Madurai', 'B.A History', 'en'),
        (4, 'Lakshmi S', '9876543213', 'lakshmi@example.com', 24, 'Trichy', 'M.A Tamil', 'ta'),
        (5, 'Karthik R', '9876543214', 'karthik@example.com', 26, 'Salem', 'B.Com', 'en')";
    $conn->exec($sql_sample_users);
    
    // Sample Test Results
    $sql_sample_results = "INSERT IGNORE INTO test_results (user_id, session_id, total_questions, attempted_questions, correct_answers, wrong_answers, unanswered, score, percentage, time_taken, started_at, submitted_at) VALUES
        (1, 1, 10, 10, 8, 2, 0, 8, 80.00, 12, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR),
        (2, 1, 10, 9, 7, 2, 1, 7, 70.00, 14, NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 1 HOUR),
        (3, 2, 10, 10, 9, 1, 0, 9, 90.00, 11, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR),
        (4, 3, 15, 15, 12, 3, 0, 12, 80.00, 18, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 4 HOUR),
        (5, 2, 10, 8, 6, 2, 2, 6, 60.00, 15, NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 5 HOUR)";
    $conn->exec($sql_sample_results);
    
    echo "✓ Sample data inserted successfully!\n\n";

    echo "==============================================\n";
    echo "  DATABASE SETUP COMPLETED SUCCESSFULLY! ✓\n";
    echo "==============================================\n\n";
    
    echo "Summary:\n";
    echo "--------\n";
    $stmt = $conn->query("SELECT COUNT(*) as count FROM users");
    $userCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    echo "→ Users: $userCount\n";
    
    $stmt = $conn->query("SELECT COUNT(*) as count FROM exam_categories");
    $examCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    echo "→ Exam Categories: $examCount\n";
    
    $stmt = $conn->query("SELECT COUNT(*) as count FROM test_categories");
    $testCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    echo "→ Test Categories: $testCount\n";
    
    $stmt = $conn->query("SELECT COUNT(*) as count FROM question_sessions");
    $sessionCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    echo "→ Question Sessions: $sessionCount\n";
    
    $stmt = $conn->query("SELECT COUNT(*) as count FROM test_results");
    $resultCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    echo "→ Test Results: $resultCount\n\n";
    
    echo "Admin Panel Access:\n";
    echo "-------------------\n";
    echo "URL: http://localhost/Mock_test/admin/index.html\n";
    echo "Username: admin\n";
    echo "Password: admin123\n\n";

} catch(PDOException $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
    exit(1);
}
?>

