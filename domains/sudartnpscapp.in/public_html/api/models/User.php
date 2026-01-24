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
    public $device_id;
    public $session_token;
    public $session_version;
    public $session_updated_at;
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
        $query = "INSERT INTO " . $this->table_name . " 
                  SET name=:name, mobile=:mobile, email=:email, age=:age,
                      district=:district, education=:education, language=:language";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(":name", $this->name);
        $stmt->bindParam(":mobile", $this->mobile);
        $stmt->bindParam(":email", $this->email);
        $stmt->bindParam(":age", $this->age);
        $stmt->bindParam(":district", $this->district);
        $stmt->bindParam(":education", $this->education);
        $stmt->bindParam(":language", $this->language);

        if ($stmt->execute()) {
            $this->id = $this->conn->lastInsertId();
            return true;
        }
        return false;
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

    /**
     * Bind current login session to one device only.
     * Call this on OTP-verified login or registration completion.
     */
    public function updateDeviceSession($deviceId, $sessionToken) {
        // Ensure required columns exist (safe for existing DBs)
        try {
            $check = $this->conn->query("SHOW COLUMNS FROM " . $this->table_name . " LIKE 'device_id'");
            if ($check->rowCount() == 0) {
                $this->conn->exec("ALTER TABLE " . $this->table_name . " ADD COLUMN device_id VARCHAR(64) NULL AFTER language");
            }
        } catch (PDOException $e) {
            // Ignore if already exists / not permitted
        }
        try {
            $check = $this->conn->query("SHOW COLUMNS FROM " . $this->table_name . " LIKE 'session_token'");
            if ($check->rowCount() == 0) {
                $this->conn->exec("ALTER TABLE " . $this->table_name . " ADD COLUMN session_token VARCHAR(128) NULL AFTER device_id");
                $this->conn->exec("CREATE INDEX idx_session_token ON " . $this->table_name . "(session_token)");
            }
        } catch (PDOException $e) {
            // Ignore if already exists / not permitted
        }
        try {
            $check = $this->conn->query("SHOW COLUMNS FROM " . $this->table_name . " LIKE 'session_version'");
            if ($check->rowCount() == 0) {
                $this->conn->exec("ALTER TABLE " . $this->table_name . " ADD COLUMN session_version INT(11) NOT NULL DEFAULT 0 AFTER session_token");
            }
        } catch (PDOException $e) {
            // Ignore if already exists / not permitted
        }
        try {
            $check = $this->conn->query("SHOW COLUMNS FROM " . $this->table_name . " LIKE 'session_updated_at'");
            if ($check->rowCount() == 0) {
                $this->conn->exec("ALTER TABLE " . $this->table_name . " ADD COLUMN session_updated_at TIMESTAMP NULL DEFAULT NULL AFTER session_version");
            }
        } catch (PDOException $e) {
            // Ignore if already exists / not permitted
        }

        $query = "UPDATE " . $this->table_name . "
                  SET device_id = :device_id,
                      session_token = :session_token,
                      session_version = session_version + 1,
                      session_updated_at = NOW(),
                      last_login = NOW()
                  WHERE id = :id";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(":device_id", $deviceId);
        $stmt->bindParam(":session_token", $sessionToken);
        $stmt->bindParam(":id", $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    public function getSessionById() {
        $query = "SELECT id, device_id, session_token, session_version, session_updated_at
                  FROM " . $this->table_name . " WHERE id = ? LIMIT 1";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->id, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt;
    }

    public function getAllUsers($limit = 100, $offset = 0) {
        $query = "SELECT id, name, mobile, email, age, district, education,
                         language, verification_method, created_at, last_login, is_active
                  FROM " . $this->table_name . "
                  ORDER BY created_at DESC
                  LIMIT :limit OFFSET :offset";

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

    public function searchUsers($search, $limit = 500) {
        $query = "SELECT id, name, mobile, email, language, is_active, verification_method, 
                         age, district, education, created_at, updated_at
                  FROM " . $this->table_name . "
                  WHERE is_active = 1 AND (name LIKE :search OR mobile LIKE :search OR email LIKE :search)
                  ORDER BY created_at DESC
                  LIMIT :limit";

        $stmt = $this->conn->prepare($query);
        $term = "%{$search}%";
        $stmt->bindParam(':search', $term, PDO::PARAM_STR);
        $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt;
    }
}
?>
