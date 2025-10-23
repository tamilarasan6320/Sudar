<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/Question.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $session_id = isset($_GET['session_id']) ? $_GET['session_id'] : null;
    $language = isset($_GET['language']) ? $_GET['language'] : 'en';

    if (!empty($session_id)) {
        $question = new Question($db);
        $question->session_id = $session_id;
        $stmt = $question->getBySession($language);

        $questions = [];

        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $formatted = [
                'id' => $row['id'],
                'session_id' => $row['session_id'],
                'correct_answer' => $row['correct_answer'],
                'difficulty' => $row['difficulty'],
                'marks' => $row['marks'],
                'negative_marks' => $row['negative_marks'],
                'display_order' => $row['display_order']
            ];

            if ($language === 'ta' && !empty($row['question_ta'])) {
                $formatted['question'] = $row['question_ta'];
                $formatted['option_a'] = $row['option_a_ta'];
                $formatted['option_b'] = $row['option_b_ta'];
                $formatted['option_c'] = $row['option_c_ta'];
                $formatted['option_d'] = $row['option_d_ta'];
                $formatted['explanation'] = $row['explanation_ta'];
            } else {
                $formatted['question'] = $row['question_en'];
                $formatted['option_a'] = $row['option_a_en'];
                $formatted['option_b'] = $row['option_b_en'];
                $formatted['option_c'] = $row['option_c_en'];
                $formatted['option_d'] = $row['option_d_en'];
                $formatted['explanation'] = $row['explanation_en'];
            }

            $formatted['has_english'] = !empty($row['question_en']);
            $formatted['has_tamil'] = !empty($row['question_ta']);

            $questions[] = $formatted;
        }

        http_response_code(200);
        echo json_encode([
            'success' => true,
            'count' => count($questions),
            'language' => $language,
            'questions' => $questions
        ]);
    } else {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Session ID is required'
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
