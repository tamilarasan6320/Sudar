<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/TestResult.php';
require_once '../models/UserAnswer.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $result_id = isset($_GET['result_id']) ? intval($_GET['result_id']) : null;
    $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : null;

    if (!$result_id) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Result ID is required.']);
        exit();
    }

    try {
        // Get test result details
        $query = "SELECT tr.*, 
                         u.name as user_name, 
                         u.mobile as user_mobile,
                         tc.name as category_name,
                         ec.name as exam_name,
                         qs.name as session_name
                  FROM test_results tr
                  LEFT JOIN users u ON tr.user_id = u.id
                  LEFT JOIN test_categories tc ON tr.test_category_id = tc.id
                  LEFT JOIN exam_categories ec ON tc.exam_category_id = ec.id
                  LEFT JOIN question_sessions qs ON tr.session_id = qs.id
                  WHERE tr.id = ?";
        
        $stmt = $db->prepare($query);
        $stmt->execute([$result_id]);
        $result = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$result) {
            http_response_code(404);
            echo json_encode(['success' => false, 'message' => 'Test result not found.']);
            exit();
        }

        // Check if user has permission (if user_id provided)
        if ($user_id && $result['user_id'] != $user_id) {
            http_response_code(403);
            echo json_encode(['success' => false, 'message' => 'Access denied.']);
            exit();
        }

        // Get user answers for this test
        $answers_query = "SELECT ua.*, 
                                 q.question_en, 
                                 q.question_ta,
                                 q.option_a_en, q.option_a_ta,
                                 q.option_b_en, q.option_b_ta,
                                 q.option_c_en, q.option_c_ta,
                                 q.option_d_en, q.option_d_ta,
                                 q.correct_answer,
                                 q.explanation_en,
                                 q.explanation_ta
                          FROM user_answers ua
                          LEFT JOIN questions q ON ua.question_id = q.id
                          WHERE ua.result_id = ?
                          ORDER BY ua.question_number ASC";
        
        $answers_stmt = $db->prepare($answers_query);
        $answers_stmt->execute([$result_id]);
        $answers = $answers_stmt->fetchAll(PDO::FETCH_ASSOC);

        // Format response
        $response_data = [
            'success' => true,
            'result' => [
                'id' => $result['id'],
                'user_name' => $result['user_name'],
                'user_mobile' => $result['user_mobile'],
                'exam_name' => $result['exam_name'],
                'category_name' => $result['category_name'],
                'session_name' => $result['session_name'],
                'total_questions' => $result['total_questions'],
                'attempted_questions' => $result['attempted_questions'],
                'correct_answers' => $result['correct_answers'],
                'wrong_answers' => $result['total_questions'] - $result['correct_answers'],
                'percentage' => round($result['percentage'], 1),
                'time_taken' => $result['time_taken'],
                'submitted_at' => $result['submitted_at'],
                'status' => $result['percentage'] >= 50 ? 'passed' : 'failed'
            ],
            'answers' => array_map(function($answer) {
                $is_correct = strtoupper($answer['user_answer']) === strtoupper($answer['correct_answer']);
                return [
                    'question_number' => $answer['question_number'] ?? 0,
                    'question_en' => $answer['question_en'],
                    'question_ta' => $answer['question_ta'],
                    'option_a_en' => $answer['option_a_en'],
                    'option_a_ta' => $answer['option_a_ta'],
                    'option_b_en' => $answer['option_b_en'],
                    'option_b_ta' => $answer['option_b_ta'],
                    'option_c_en' => $answer['option_c_en'],
                    'option_c_ta' => $answer['option_c_ta'],
                    'option_d_en' => $answer['option_d_en'],
                    'option_d_ta' => $answer['option_d_ta'],
                    'correct_answer' => $answer['correct_answer'],
                    'selected_answer' => $answer['user_answer'],
                    'is_correct' => $is_correct,
                    'explanation_en' => $answer['explanation_en'],
                    'explanation_ta' => $answer['explanation_ta'],
                    'time_spent' => $answer['time_spent'] ?? 0
                ];
            }, $answers)
        ];

        http_response_code(200);
        echo json_encode($response_data);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
    }

} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed.']);
}
?>

