<?php
/**
 * User Model
 */

class User {
    private $conn;
    private $table_name = "users";

    public $id;
    public $name;
    public $mobile;
    public $email;
    public $age;
    public $district;
    public $education;
    public $profile_pic;
    public $language;
    public $verification_method; // 'otp' or 'truecaller'
    public $created_at;
    public $updated_at;
    public $last_login;
    public $is_active;

    public function __construct($db) {
        $this->conn = $db;
    }

    public function getUserByMobile() {
        $query = "SELECT * FROM " . $this->table_name . " WHERE mobile = ? LIMIT 1";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->mobile);
        $stmt->execute();
        return $stmt;
    }

    public function create() {
        // Check if verification_method column exists
        $hasVerificationMethod = $this->columnExists('verification_method');
        
        if ($hasVerificationMethod) {
            $query = "INSERT INTO " . $this->table_name . " 
                      SET name=:name, mobile=:mobile, email=:email, age=:age,
                          district=:district, education=:education, language=:language,
                          verification_method=:verification_method";
        } else {
            $query = "INSERT INTO " . $this->table_name . " 
                      SET name=:name, mobile=:mobile, email=:email, age=:age,
                          district=:district, education=:education, language=:language";
        }

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(":name", $this->name);
        $stmt->bindParam(":mobile", $this->mobile);
        $stmt->bindParam(":email", $this->email);
        $stmt->bindParam(":age", $this->age);
        $stmt->bindParam(":district", $this->district);
        $stmt->bindParam(":education", $this->education);
        $stmt->bindParam(":language", $this->language);
        
        if ($hasVerificationMethod) {
            $verificationMethod = $this->verification_method ?? 'otp';
            $stmt->bindParam(":verification_method", $verificationMethod);
        }

        if ($stmt->execute()) {
            $this->id = $this->conn->lastInsertId();
            return true;
        }
        return false;
    }
    
    /**
     * Check if a column exists in the users table
     */
    private function columnExists($columnName) {
        try {
            $query = "SHOW COLUMNS FROM " . $this->table_name . " LIKE :column";
            $stmt = $this->conn->prepare($query);
            $stmt->bindParam(":column", $columnName);
            $stmt->execute();
            return $stmt->rowCount() > 0;
        } catch (Exception $e) {
            return false;
        }
    }

    public function update() {
        $query = "UPDATE " . $this->table_name . " 
                  SET name=:name, email=:email, age=:age, district=:district,
                      education=:education, language=:language, profile_pic=:profile_pic 
                  WHERE id=:id";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(":name", $this->name);
        $stmt->bindParam(":email", $this->email);
        $stmt->bindParam(":age", $this->age);
        $stmt->bindParam(":district", $this->district);
        $stmt->bindParam(":education", $this->education);
        $stmt->bindParam(":language", $this->language);
        $stmt->bindParam(":profile_pic", $this->profile_pic);
        $stmt->bindParam(":id", $this->id);

        return $stmt->execute();
    }

    public function updateLastLogin() {
        $query = "UPDATE " . $this->table_name . " SET last_login = NOW() WHERE id = ?";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->id);
        return $stmt->execute();
    }

    public function getAllUsers($limit = 100, $offset = 0) {
        // Check if verification_method column exists
        $hasVerificationMethod = $this->columnExists('verification_method');
        
        if ($hasVerificationMethod) {
            $query = "SELECT id, name, mobile, email, age, district, education,
                             language, verification_method, created_at, last_login, is_active
                      FROM " . $this->table_name . "
                      ORDER BY created_at DESC
                      LIMIT :limit OFFSET :offset";
        } else {
            $query = "SELECT id, name, mobile, email, age, district, education,
                             language, 'otp' as verification_method, created_at, last_login, is_active
                      FROM " . $this->table_name . "
                      ORDER BY created_at DESC
                      LIMIT :limit OFFSET :offset";
        }

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
        $stmt->bindParam(':offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt;
    }

    public function getUserStats() {
        $query = "SELECT
                    u.id,
                    u.name,
                    u.mobile,
                    u.email,
                    COUNT(DISTINCT tr.id) AS total_tests,
                    COALESCE(AVG(tr.percentage), 0) AS avg_score,
                    COALESCE(ur.rank, 0) AS rank,
                    MAX(tr.submitted_at) AS last_test_date
                  FROM " . $this->table_name . " u
                  LEFT JOIN test_results tr ON u.id = tr.user_id
                  LEFT JOIN user_rankings ur ON u.id = ur.user_id
                  WHERE u.id = ?
                  GROUP BY u.id";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->id, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt;
    }

    public function delete() {
        // Hard delete - actually removes from database
        $query = "DELETE FROM " . $this->table_name . " WHERE id = ?";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }
    
    public function softDelete() {
        // Soft delete - marks as inactive
        $query = "UPDATE " . $this->table_name . " SET is_active = 0 WHERE id = ?";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    public function searchUsers($search) {
        $query = "SELECT id, name, mobile, email, created_at
                  FROM " . $this->table_name . "
                  WHERE (name LIKE :search OR mobile LIKE :search OR email LIKE :search)
                  ORDER BY name
                  LIMIT 50";

        $stmt = $this->conn->prepare($query);
        $term = "%{$search}%";
        $stmt->bindParam(':search', $term, PDO::PARAM_STR);
        $stmt->execute();
        return $stmt;
    }
}
?>
