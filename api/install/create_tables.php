<?php
/**
 * Database Installation Script
 */

require_once '../config/database.php';

Database::createDatabase();

$database = new Database();
$conn = $database->getConnection();

try {
    // 1. Users Table
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

    // 2. OTP Table
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

    // 3. Exam Categories Table
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

    // 4. Test Categories Table
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

    // 5. Question Sessions Table
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

    // 6. Questions Table
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

    // 7. Test Results Table
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

    // 8. User Answers Table
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

    // 9. User Rankings Table
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

    // 10. Saved Tests Table
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

    // 11. Admin Users Table
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

    $conn->exec($sql_users);
    $conn->exec($sql_otp);
    $conn->exec($sql_exam_categories);
    $conn->exec($sql_test_categories);
    $conn->exec($sql_question_sessions);
    $conn->exec($sql_questions);
    $conn->exec($sql_test_results);
    $conn->exec($sql_user_answers);
    $conn->exec($sql_rankings);
    $conn->exec($sql_saved_tests);
    $conn->exec($sql_admin);

    $admin_password = password_hash('admin123', PASSWORD_DEFAULT);
    $sql_default_admin = "INSERT IGNORE INTO admin_users (username, password, email, role)
                          VALUES ('admin', '$admin_password', 'admin@mocktest.com', 'super_admin')";
    $conn->exec($sql_default_admin);

    echo "✓ Database installation completed successfully!";

} catch(PDOException $e) {
    echo "❌ Error creating tables: " . $e->getMessage();
}
?>
