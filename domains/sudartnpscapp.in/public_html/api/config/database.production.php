<?php
/**
 * Database Configuration - PRODUCTION (Hostinger)
 * 
 * IMPORTANT: Update these values with your Hostinger database credentials
 * Get these from: hPanel → Databases → MySQL Databases
 */

class Database {
    // ============================================
    // UPDATE THESE VALUES FOR YOUR HOSTINGER ACCOUNT
    // ============================================
    private $host = "localhost";  // Usually 'localhost' on Hostinger
    private $db_name = "u123456789_mock_test_db";  // Your database name from hPanel
    private $username = "u123456789_dbuser";  // Your database username from hPanel
    private $password = "YourSecurePassword123!";  // Your database password from hPanel
    
    public $conn;

    public function getConnection() {
        $this->conn = null;

        try {
            $this->conn = new PDO(
                "mysql:host=" . $this->host . ";dbname=" . $this->db_name . ";charset=utf8mb4",
                $this->username,
                $this->password,
                [
                    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                    PDO::ATTR_EMULATE_PREPARES => false,
                ]
            );
            $this->conn->exec("SET NAMES utf8mb4");
        } catch (PDOException $exception) {
            // Log error instead of displaying (security)
            error_log("Database connection error: " . $exception->getMessage());
            // Return generic error message
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Database connection failed. Please contact administrator.'
            ]);
            exit;
        }

        return $this->conn;
    }

    public static function createDatabase() {
        // This method is typically not needed in production
        // Database should already exist
        return false;
    }
}
?>

