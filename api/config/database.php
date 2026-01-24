<?php
/**
 * Database Configuration
 */

class Database {
    private $host = "localhost";
    private $db_name = "u747149096_mock_test_db";
    private $username = "u747149096_dbuser";
    private $password = "tLaDgqzL3~";
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
                'message' => 'Database connection failed. Please contact administrator.'
            ]);
            exit;
        }

        return $this->conn;
    }

    public static function createDatabase() {
        $host = "localhost";
        $username = "root";
        $password = "";

        try {
            $conn = new PDO("mysql:host=$host", $username, $password);
            $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $sql = "CREATE DATABASE IF NOT EXISTS mock_test_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
            $conn->exec($sql);
            return true;
        } catch (PDOException $e) {
            echo "Database creation error: " . $e->getMessage();
            return false;
        }
    }
}
?>
