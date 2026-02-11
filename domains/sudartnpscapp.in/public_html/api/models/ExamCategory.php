<?php
/**
 * Exam Category Model
 */

class ExamCategory {
    private $conn;
    private $table_name = "exam_categories";

    public $id;
    public $name;
    public $description;
    public $icon;
    public $is_active;
    public $display_order;

    public function __construct($db) {
        $this->conn = $db;
    }

    public function getAll() {
        $query = "SELECT * FROM " . $this->table_name . "
                  WHERE is_active = 1
                  ORDER BY display_order, name";
        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        return $stmt;
    }

    // Admin: show both active and inactive categories (so admin can re-enable via eye toggle)
    public function getAllAdmin() {
        $query = "SELECT * FROM " . $this->table_name . "
                  ORDER BY display_order, name";
        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        return $stmt;
    }

    public function getById() {
        $query = "SELECT * FROM " . $this->table_name . " WHERE id = ? LIMIT 1";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->id, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt;
    }

    public function create() {
        $query = "INSERT INTO " . $this->table_name . "
                  SET name=:name, description=:description, icon=:icon,
                      is_active=:is_active, display_order=:display_order";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':name', $this->name);
        $stmt->bindParam(':description', $this->description);
        $stmt->bindParam(':icon', $this->icon);
        $stmt->bindParam(':is_active', $this->is_active, PDO::PARAM_BOOL);
        $stmt->bindParam(':display_order', $this->display_order, PDO::PARAM_INT);

        if ($stmt->execute()) {
            $this->id = $this->conn->lastInsertId();
            return true;
        }
        return false;
    }

    public function update() {
        $query = "UPDATE " . $this->table_name . "
                  SET name=:name, description=:description, icon=:icon,
                      is_active=:is_active, display_order=:display_order
                  WHERE id=:id";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':name', $this->name);
        $stmt->bindParam(':description', $this->description);
        $stmt->bindParam(':icon', $this->icon);
        $stmt->bindParam(':is_active', $this->is_active, PDO::PARAM_BOOL);
        $stmt->bindParam(':display_order', $this->display_order, PDO::PARAM_INT);
        $stmt->bindParam(':id', $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    public function delete() {
        $query = "UPDATE " . $this->table_name . " SET is_active = 0 WHERE id = ?";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }
}
?>
