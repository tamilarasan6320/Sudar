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
            $hasEnglish = !empty($row['question_en']) && !empty($row['option_a_en']);
            $hasTamil = !empty($row['question_ta']) && !empty($row['option_a_ta']);
            
            // Return BOTH languages - app will display both simultaneously
            $formatted = [
                'id' => $row['id'],
                'session_id' => $row['session_id'],
                'correct_answer' => $row['correct_answer'],
                'difficulty' => $row['difficulty'],
                'marks' => $row['marks'],
                'negative_marks' => $row['negative_marks'],
                'display_order' => $row['display_order'],
                
                // English version
                'question_en' => $row['question_en'] ?? null,
                'option_a_en' => $row['option_a_en'] ?? null,
                'option_b_en' => $row['option_b_en'] ?? null,
                'option_c_en' => $row['option_c_en'] ?? null,
                'option_d_en' => $row['option_d_en'] ?? null,
                'explanation_en' => $row['explanation_en'] ?? null,
                
                // Tamil version
                'question_ta' => $row['question_ta'] ?? null,
                'option_a_ta' => $row['option_a_ta'] ?? null,
                'option_b_ta' => $row['option_b_ta'] ?? null,
                'option_c_ta' => $row['option_c_ta'] ?? null,
                'option_d_ta' => $row['option_d_ta'] ?? null,
                'explanation_ta' => $row['explanation_ta'] ?? null,
                
                // Language availability flags
                'has_english' => $hasEnglish,
                'has_tamil' => $hasTamil
            ];

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
