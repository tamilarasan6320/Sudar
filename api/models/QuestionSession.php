<?php
/**
 * Question Session Model
 */

class QuestionSession {
    private $conn;
    private $table_name = "question_sessions";

    public $id;
    public $test_category_id;
    public $name;
    public $description;
    public $total_questions;
    public $duration;
    public $difficulty;
    public $is_active;

    public function __construct($db) {
        $this->conn = $db;
    }

    public function getAll($test_category_id = null) {
        $query = "SELECT qs.*, 
                  tc.name AS category_name,
                  tc.exam_category_id,
                  ec.name AS exam_category_name,
                  (SELECT COUNT(*) FROM questions WHERE session_id = qs.id) AS actual_question_count
                  FROM " . $this->table_name . " qs
                  LEFT JOIN test_categories tc ON qs.test_category_id = tc.id
                  LEFT JOIN exam_categories ec ON tc.exam_category_id = ec.id
                  WHERE qs.is_active = 1";

        if ($test_category_id) {
            $query .= " AND qs.test_category_id = :test_category_id";
        }

        $query .= " ORDER BY qs.created_at DESC";

        $stmt = $this->conn->prepare($query);

        if ($test_category_id) {
            $stmt->bindParam(':test_category_id', $test_category_id, PDO::PARAM_INT);
        }

        $stmt->execute();
        return $stmt;
    }

    public function getById() {
        $query = "SELECT qs.*, 
                  tc.name AS category_name,
                  tc.exam_category_id,
                  ec.name AS exam_category_name,
                  (SELECT COUNT(*) FROM questions WHERE session_id = qs.id) AS actual_question_count
                  FROM " . $this->table_name . " qs
                  LEFT JOIN test_categories tc ON qs.test_category_id = tc.id
                  LEFT JOIN exam_categories ec ON tc.exam_category_id = ec.id
                  WHERE qs.id = ? LIMIT 1";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->id, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt;
    }

    public function create() {
        $query = "INSERT INTO " . $this->table_name . "
                  SET test_category_id=:test_category_id, name=:name, description=:description,
                      total_questions=:total_questions, duration=:duration, difficulty=:difficulty";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':test_category_id', $this->test_category_id, PDO::PARAM_INT);
        $stmt->bindParam(':name', $this->name);
        $stmt->bindParam(':description', $this->description);
        $stmt->bindParam(':total_questions', $this->total_questions, PDO::PARAM_INT);
        $stmt->bindParam(':duration', $this->duration, PDO::PARAM_INT);
        $stmt->bindParam(':difficulty', $this->difficulty);

        if ($stmt->execute()) {
            $this->id = $this->conn->lastInsertId();
            return true;
        }
        return false;
    }

    public function update() {
        $query = "UPDATE " . $this->table_name . "
                  SET test_category_id=:test_category_id, name=:name, description=:description,
                      total_questions=:total_questions, duration=:duration, difficulty=:difficulty
                  WHERE id=:id";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':test_category_id', $this->test_category_id, PDO::PARAM_INT);
        $stmt->bindParam(':name', $this->name);
        $stmt->bindParam(':description', $this->description);
        $stmt->bindParam(':total_questions', $this->total_questions, PDO::PARAM_INT);
        $stmt->bindParam(':duration', $this->duration, PDO::PARAM_INT);
        $stmt->bindParam(':difficulty', $this->difficulty);
        $stmt->bindParam(':id', $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    public function delete() {
        $query = "UPDATE " . $this->table_name . " SET is_active = 0 WHERE id = ?";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    public function updateQuestionCount() {
        $query = "UPDATE " . $this->table_name . "
                  SET total_questions = (SELECT COUNT(*) FROM questions WHERE session_id = ?)
                  WHERE id = ?";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->id, PDO::PARAM_INT);
        $stmt->bindParam(2, $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }
}
?>
