<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/TestResult.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'));

    if (!empty($data->user_id) && !empty($data->session_id) && isset($data->answers)) {
        try {
            $db->beginTransaction();

            $question_query = "SELECT id, correct_answer, marks, negative_marks FROM questions WHERE session_id = ?";
            $question_stmt = $db->prepare($question_query);
            $question_stmt->execute([$data->session_id]);
            $questions = $question_stmt->fetchAll(PDO::FETCH_ASSOC);

            $question_map = [];
            foreach ($questions as $q) {
                $question_map[$q['id']] = $q;
            }

            $total_questions = count($questions);
            $attempted = 0;
            $correct = 0;
            $wrong = 0;
            $score = 0;

            foreach ($data->answers as $answer) {
                $question_id = $answer->question_id;
                $user_answer = isset($answer->answer) ? $answer->answer : null;

                if ($user_answer !== null) {
                    $attempted++;

                    $correct_answer = $question_map[$question_id]['correct_answer'];
                    $is_correct = ($user_answer === $correct_answer);

                    if ($is_correct) {
                        $correct++;
                        $score += $question_map[$question_id]['marks'];
                    } else {
                        $wrong++;
                        $score -= $question_map[$question_id]['negative_marks'];
                    }
                }
            }

            $unanswered = $total_questions - $attempted;
            $max_score = array_sum(array_column($questions, 'marks'));
            $percentage = $max_score > 0 ? ($score / $max_score) * 100 : 0;

            $result = new TestResult($db);
            $result->user_id = $data->user_id;
            $result->session_id = $data->session_id;
            $result->total_questions = $total_questions;
            $result->attempted_questions = $attempted;
            $result->correct_answers = $correct;
            $result->wrong_answers = $wrong;
            $result->unanswered = $unanswered;
            $result->score = $score;
            $result->percentage = $percentage;
            $result->time_taken = isset($data->time_taken) ? $data->time_taken : 0;
            $result->started_at = isset($data->started_at) ? $data->started_at : date('Y-m-d H:i:s');

            if ($result->create()) {
                foreach ($data->answers as $answer) {
                    if (isset($answer->answer) && $answer->answer !== null) {
                        $answer_query = "INSERT INTO user_answers
                                         (result_id, question_id, user_answer, is_correct, time_spent, marked_for_review)
                                         VALUES (?, ?, ?, ?, ?, ?)";
                        $answer_stmt = $db->prepare($answer_query);

                        $correct_answer = $question_map[$answer->question_id]['correct_answer'];
                        $is_correct = ($answer->answer === $correct_answer) ? 1 : 0;
                        $time_spent = isset($answer->time_spent) ? $answer->time_spent : 0;
                        $marked = isset($answer->marked_for_review) ? $answer->marked_for_review : 0;

                        $answer_stmt->execute([
                            $result->id,
                            $answer->question_id,
                            $answer->answer,
                            $is_correct,
                            $time_spent,
                            $marked
                        ]);
                    }
                }

                $result->calculateAndUpdateRanks($data->session_id);

                $rank_query = "SELECT rank FROM test_results WHERE id = ?";
                $rank_stmt = $db->prepare($rank_query);
                $rank_stmt->execute([$result->id]);
                $rank_row = $rank_stmt->fetch(PDO::FETCH_ASSOC);

                $db->commit();

                http_response_code(201);
                echo json_encode([
                    'success' => true,
                    'message' => 'Test submitted successfully',
                    'result' => [
                        'result_id' => $result->id,
                        'total_questions' => $total_questions,
                        'attempted' => $attempted,
                        'correct' => $correct,
                        'wrong' => $wrong,
                        'unanswered' => $unanswered,
                        'score' => $score,
                        'percentage' => round($percentage, 2),
                        'rank' => $rank_row['rank'],
                        'time_taken' => $result->time_taken
                    ]
                ]);
            } else {
                $db->rollBack();
                http_response_code(500);
                echo json_encode([
                    'success' => false,
                    'message' => 'Failed to submit test'
                ]);
            }
        } catch (PDOException $e) {
            $db->rollBack();
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Error submitting test',
                'error' => $e->getMessage()
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'User ID, session ID, and answers are required'
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed'
    ]);
}
?>
