<?php
/**
 * Test Category Model
 */

class TestCategory {
    private $conn;
    private $table_name = "test_categories";

    public $id;
    public $exam_category_id;
    public $name;
    public $description;
    public $icon;
    public $color;
    public $image;
    public $image_path;
    public $is_active;
    public $display_order;

    public function __construct($db) {
        $this->conn = $db;
    }

    public function getAll($exam_category_id = null, $user_id = null) {
        // Build the query with subquery for completed count
        $query = "SELECT tc.*, 
                         ec.name AS exam_name,
                         COUNT(DISTINCT qs.id) AS sessions_count";
        
        if ($user_id) {
            $query .= ", COALESCE((
                             SELECT COUNT(DISTINCT tr2.session_id)
                             FROM test_results tr2
                             INNER JOIN question_sessions qs2 ON tr2.session_id = qs2.id
                             WHERE qs2.test_category_id = tc.id 
                             AND qs2.is_active = 1
                             AND tr2.user_id = :user_id
                         ), 0) AS completed_count";
        } else {
            $query .= ", 0 AS completed_count";
        }
        
        $query .= " FROM " . $this->table_name . " tc
                  LEFT JOIN exam_categories ec ON tc.exam_category_id = ec.id
                  LEFT JOIN question_sessions qs ON qs.test_category_id = tc.id AND qs.is_active = 1
                  WHERE tc.is_active = 1";

        if ($exam_category_id) {
            $query .= " AND tc.exam_category_id = :exam_category_id";
        }

        $query .= " GROUP BY tc.id
                    ORDER BY tc.display_order, tc.name";

        $stmt = $this->conn->prepare($query);

        if ($exam_category_id) {
            $stmt->bindParam(':exam_category_id', $exam_category_id, PDO::PARAM_INT);
        }
        
        if ($user_id) {
            $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
        }

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
                  SET exam_category_id=:exam_category_id, name=:name, description=:description,
                      icon=:icon, color=:color, image=:image, image_path=:image_path, 
                      is_active=:is_active, display_order=:display_order";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':exam_category_id', $this->exam_category_id, PDO::PARAM_INT);
        $stmt->bindParam(':name', $this->name);
        $stmt->bindParam(':description', $this->description);
        $stmt->bindParam(':icon', $this->icon);
        $stmt->bindParam(':color', $this->color);
        $stmt->bindParam(':image', $this->image);
        $stmt->bindParam(':image_path', $this->image_path);
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
                  SET exam_category_id=:exam_category_id, name=:name, description=:description,
                      icon=:icon, color=:color, image=:image, image_path=:image_path,
                      is_active=:is_active, display_order=:display_order
                  WHERE id=:id";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':exam_category_id', $this->exam_category_id, PDO::PARAM_INT);
        $stmt->bindParam(':name', $this->name);
        $stmt->bindParam(':description', $this->description);
        $stmt->bindParam(':icon', $this->icon);
        $stmt->bindParam(':color', $this->color);
        $stmt->bindParam(':image', $this->image);
        $stmt->bindParam(':image_path', $this->image_path);
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
