<?php
/**
 * Database Configuration - HOSTINGER
 * 
 * IMPORTANT: Update these values with your Hostinger database credentials
 * Get credentials from: Hostinger hPanel → Databases → MySQL Databases
 */

class Database {
    // UPDATE THESE VALUES:
    private $host = "localhost";  // Usually 'localhost' on Hostinger
    private $db_name = "YOUR_DATABASE_NAME";  // e.g., u123456789_mock_test_db
    private $username = "YOUR_DATABASE_USERNAME";  // e.g., u123456789_dbuser
    private $password = "YOUR_DATABASE_PASSWORD";  // Your database password
    
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
            error_log("Database connection error: " . $exception->getMessage());
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Database connection failed. Please update credentials in api/config/database.php'
            ]);
            exit;
        }

        return $this->conn;
    }

    public static function createDatabase() {
        return false;
    }
}
?>

