<?php
/**
 * Test Result Model
 */

class TestResult {
    private $conn;
    private $table_name = "test_results";

    public $id;
    public $user_id;
    public $session_id;
    public $total_questions;
    public $attempted_questions;
    public $correct_answers;
    public $wrong_answers;
    public $unanswered;
    public $score;
    public $percentage;
    public $time_taken;
    public $rank;
    public $started_at;
    public $submitted_at;

    public function __construct($db) {
        $this->conn = $db;
    }

    public function create() {
        $query = "INSERT INTO " . $this->table_name . "
                  SET user_id=:user_id, session_id=:session_id,
                      total_questions=:total_questions, attempted_questions=:attempted_questions,
                      correct_answers=:correct_answers, wrong_answers=:wrong_answers,
                      unanswered=:unanswered, score=:score, percentage=:percentage,
                      time_taken=:time_taken, started_at=:started_at";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':user_id', $this->user_id, PDO::PARAM_INT);
        $stmt->bindParam(':session_id', $this->session_id, PDO::PARAM_INT);
        $stmt->bindParam(':total_questions', $this->total_questions, PDO::PARAM_INT);
        $stmt->bindParam(':attempted_questions', $this->attempted_questions, PDO::PARAM_INT);
        $stmt->bindParam(':correct_answers', $this->correct_answers, PDO::PARAM_INT);
        $stmt->bindParam(':wrong_answers', $this->wrong_answers, PDO::PARAM_INT);
        $stmt->bindParam(':unanswered', $this->unanswered, PDO::PARAM_INT);
        $stmt->bindParam(':score', $this->score);
        $stmt->bindParam(':percentage', $this->percentage);
        $stmt->bindParam(':time_taken', $this->time_taken, PDO::PARAM_INT);
        $stmt->bindParam(':started_at', $this->started_at);

        if ($stmt->execute()) {
            $this->id = $this->conn->lastInsertId();
            return true;
        }
        return false;
    }

    public function getUserHistory($limit = 50) {
        $query = "SELECT tr.*, qs.name AS test_name, tc.name AS category_name
                  FROM " . $this->table_name . " tr
                  LEFT JOIN question_sessions qs ON tr.session_id = qs.id
                  LEFT JOIN test_categories tc ON qs.test_category_id = tc.id
                  WHERE tr.user_id = ?
                  ORDER BY tr.submitted_at DESC
                  LIMIT ?";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->user_id, PDO::PARAM_INT);
        $stmt->bindParam(2, $limit, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt;
    }

    public function getById() {
        $query = "SELECT tr.*, qs.name AS test_name, tc.name AS category_name,
                         u.name AS user_name, u.mobile AS user_mobile
                  FROM " . $this->table_name . " tr
                  LEFT JOIN question_sessions qs ON tr.session_id = qs.id
                  LEFT JOIN test_categories tc ON qs.test_category_id = tc.id
                  LEFT JOIN users u ON tr.user_id = u.id
                  WHERE tr.id = ? LIMIT 1";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->id, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt;
    }

    public function getAllResults($limit = 100, $offset = 0) {
        $query = "SELECT tr.*, qs.name AS test_name, tc.name AS category_name,
                         u.name AS user_name, u.mobile AS user_mobile
                  FROM " . $this->table_name . " tr
                  LEFT JOIN question_sessions qs ON tr.session_id = qs.id
                  LEFT JOIN test_categories tc ON qs.test_category_id = tc.id
                  LEFT JOIN users u ON tr.user_id = u.id
                  ORDER BY tr.submitted_at DESC
                  LIMIT :limit OFFSET :offset";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
        $stmt->bindParam(':offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt;
    }

    public function getUserPerformance() {
        $query = "SELECT
                    COUNT(id) AS total_tests,
                    SUM(attempted_questions) AS total_attempted,
                    SUM(correct_answers) AS total_correct,
                    SUM(wrong_answers) AS total_wrong,
                    AVG(percentage) AS avg_percentage,
                    MAX(percentage) AS best_score,
                    MIN(percentage) AS lowest_score,
                    SUM(time_taken) AS total_time_spent
                  FROM " . $this->table_name . "
                  WHERE user_id = ?";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(1, $this->user_id, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt;
    }

    public function calculateAndUpdateRanks($session_id) {
        $query = "UPDATE " . $this->table_name . " tr1
                  JOIN (
                      SELECT id,
                             @rank := @rank + 1 AS new_rank
                      FROM " . $this->table_name . "
                      CROSS JOIN (SELECT @rank := 0) r
                      WHERE session_id = :session_id
                      ORDER BY score DESC, time_taken ASC
                  ) tr2 ON tr1.id = tr2.id
                  SET tr1.rank = tr2.new_rank";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':session_id', $session_id, PDO::PARAM_INT);
        return $stmt->execute();
    }
}
?>
