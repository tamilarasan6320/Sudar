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
                "mysql:host=" . $this->host . ";dbname=" . $this->db_name,
                $this->username,
                $this->password
            );
            $this->conn->exec("SET NAMES utf8mb4");
            $this->conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        } catch (PDOException $exception) {
            echo "Connection error: " . $exception->getMessage();
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
