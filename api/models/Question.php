<?php
/**
 * Question Model
 */

class Question {
    private $conn;
    private $table_name = "questions";

    public $id;
    public $session_id;
    public $question_en;
    public $question_ta;
    public $option_a_en;
    public $option_a_ta;
    public $option_b_en;
    public $option_b_ta;
    public $option_c_en;
    public $option_c_ta;
    public $option_d_en;
    public $option_d_ta;
    public $correct_answer;
    public $explanation_en;
    public $explanation_ta;
    public $difficulty;
    public $marks;
    public $negative_marks;
    public $display_order;

    public function __construct($db) {
        $this->conn = $db;
    }

    public function getBySession($language = 'en') {
        $query = "SELECT * FROM " . $this->table_name . " 
                  WHERE session_id = ?
                  ORDER BY display_order, id";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->session_id, PDO::PARAM_INT);
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
                  SET session_id=:session_id,
                      question_en=:question_en, question_ta=:question_ta,
                      option_a_en=:option_a_en, option_a_ta=:option_a_ta,
                      option_b_en=:option_b_en, option_b_ta=:option_b_ta,
                      option_c_en=:option_c_en, option_c_ta=:option_c_ta,
                      option_d_en=:option_d_en, option_d_ta=:option_d_ta,
                      correct_answer=:correct_answer,
                      explanation_en=:explanation_en, explanation_ta=:explanation_ta,
                      difficulty=:difficulty, marks=:marks, negative_marks=:negative_marks,
                      display_order=:display_order";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':session_id', $this->session_id, PDO::PARAM_INT);
        $stmt->bindParam(':question_en', $this->question_en);
        $stmt->bindParam(':question_ta', $this->question_ta);
        $stmt->bindParam(':option_a_en', $this->option_a_en);
        $stmt->bindParam(':option_a_ta', $this->option_a_ta);
        $stmt->bindParam(':option_b_en', $this->option_b_en);
        $stmt->bindParam(':option_b_ta', $this->option_b_ta);
        $stmt->bindParam(':option_c_en', $this->option_c_en);
        $stmt->bindParam(':option_c_ta', $this->option_c_ta);
        $stmt->bindParam(':option_d_en', $this->option_d_en);
        $stmt->bindParam(':option_d_ta', $this->option_d_ta);
        $stmt->bindParam(':correct_answer', $this->correct_answer);
        $stmt->bindParam(':explanation_en', $this->explanation_en);
        $stmt->bindParam(':explanation_ta', $this->explanation_ta);
        $stmt->bindParam(':difficulty', $this->difficulty);
        $stmt->bindParam(':marks', $this->marks);
        $stmt->bindParam(':negative_marks', $this->negative_marks);
        $stmt->bindParam(':display_order', $this->display_order, PDO::PARAM_INT);

        if ($stmt->execute()) {
            $this->id = $this->conn->lastInsertId();
            return true;
        }
        return false;
    }

    public function update() {
        $query = "UPDATE " . $this->table_name . "
                  SET question_en=:question_en, question_ta=:question_ta,
                      option_a_en=:option_a_en, option_a_ta=:option_a_ta,
                      option_b_en=:option_b_en, option_b_ta=:option_b_ta,
                      option_c_en=:option_c_en, option_c_ta=:option_c_ta,
                      option_d_en=:option_d_en, option_d_ta=:option_d_ta,
                      correct_answer=:correct_answer,
                      explanation_en=:explanation_en, explanation_ta=:explanation_ta,
                      difficulty=:difficulty, marks=:marks, negative_marks=:negative_marks
                  WHERE id=:id";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':question_en', $this->question_en);
        $stmt->bindParam(':question_ta', $this->question_ta);
        $stmt->bindParam(':option_a_en', $this->option_a_en);
        $stmt->bindParam(':option_a_ta', $this->option_a_ta);
        $stmt->bindParam(':option_b_en', $this->option_b_en);
        $stmt->bindParam(':option_b_ta', $this->option_b_ta);
        $stmt->bindParam(':option_c_en', $this->option_c_en);
        $stmt->bindParam(':option_c_ta', $this->option_c_ta);
        $stmt->bindParam(':option_d_en', $this->option_d_en);
        $stmt->bindParam(':option_d_ta', $this->option_d_ta);
        $stmt->bindParam(':correct_answer', $this->correct_answer);
        $stmt->bindParam(':explanation_en', $this->explanation_en);
        $stmt->bindParam(':explanation_ta', $this->explanation_ta);
        $stmt->bindParam(':difficulty', $this->difficulty);
        $stmt->bindParam(':marks', $this->marks);
        $stmt->bindParam(':negative_marks', $this->negative_marks);
        $stmt->bindParam(':id', $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    public function delete() {
        $query = "DELETE FROM " . $this->table_name . " WHERE id = ?";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    public function deleteBySession() {
        $query = "DELETE FROM " . $this->table_name . " WHERE session_id = ?";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->session_id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    public function checkDuplicate() {
        $query = "SELECT id FROM " . $this->table_name . "
                  WHERE session_id = :session_id AND (
                      (question_en IS NOT NULL AND question_en = :question_en) OR
                      (question_ta IS NOT NULL AND question_ta = :question_ta)
                  ) LIMIT 1";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':session_id', $this->session_id, PDO::PARAM_INT);
        $stmt->bindParam(':question_en', $this->question_en);
        $stmt->bindParam(':question_ta', $this->question_ta);
        $stmt->execute();
        return $stmt->rowCount() > 0;
    }
}
?>
