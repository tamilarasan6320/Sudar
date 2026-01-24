<?php
// VERSION TEST - TABLES ENABLED - 2025-12-03
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/Question.php';

/**
 * Clean text: Strip HTML tags BUT KEEP <img> and <table> tags!
 */
function cleanText($text) {
    if (empty($text)) {
        return $text;
    }
    
    // Strip HTML tags but KEEP <img> and <table> tags for questions with images/tables
    $text = strip_tags($text, '<img><table><tr><td><th><tbody><thead><br><div>');
    
    // Decode HTML entities
    $text = html_entity_decode($text, ENT_QUOTES | ENT_HTML5, 'UTF-8');
    
    // Remove extra whitespace (but preserve single spaces)
    $text = preg_replace('/[ \t]+/u', ' ', $text);
    
    // Trim
    return trim($text);
}

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    // TEST: Check version
    if (isset($_GET['check_version'])) {
        die(json_encode(['version' => 'TABLES_ENABLED_2025_12_03', 'line15' => 'strip_tags with table tags']));
    }
    
    $session_id = isset($_GET['session_id']) ? $_GET['session_id'] : null;

    if (!empty($session_id)) {
        $question = new Question($db);
        $question->session_id = $session_id;
        // Always get both languages - ignore language parameter
        $stmt = $question->getBySession('both');

        $questions = [];

        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            // Check for English content
            $hasEnglish = !empty($row['question_en']) && !empty($row['option_a_en']);
            
            // Check for Tamil content - check both question_ta field AND Tamil characters in question_en
            $hasTamilInEn = !empty($row['question_en']) && preg_match('/[\x{0B80}-\x{0BFF}]/u', $row['question_en']);
            $hasTamilInTa = !empty($row['question_ta']) && !empty($row['option_a_ta']);
            $hasTamilOptions = !empty($row['option_a_ta']) || 
                              (!empty($row['option_a_en']) && preg_match('/[\x{0B80}-\x{0BFF}]/u', $row['option_a_en']));
            $hasTamil = $hasTamilInTa || ($hasTamilInEn && $hasTamilOptions);
            
            // 🔥 Clean ALL text fields - strip HTML tags!
            $formatted = [
                'id' => $row['id'],
                'session_id' => $row['session_id'],
                'correct_answer' => $row['correct_answer'],
                'difficulty' => $row['difficulty'],
                'marks' => $row['marks'],
                'negative_marks' => $row['negative_marks'],
                'display_order' => $row['display_order'],
                
                // English version - CLEANED
                'question_en' => $row['question_en'] ? cleanText($row['question_en']) : null,
                'option_a_en' => $row['option_a_en'] ? cleanText($row['option_a_en']) : null,
                'option_b_en' => $row['option_b_en'] ? cleanText($row['option_b_en']) : null,
                'option_c_en' => $row['option_c_en'] ? cleanText($row['option_c_en']) : null,
                'option_d_en' => $row['option_d_en'] ? cleanText($row['option_d_en']) : null,
                'explanation_en' => $row['explanation_en'] ? cleanText($row['explanation_en']) : null,
                
                // Tamil version - CLEANED
                'question_ta' => $row['question_ta'] ? cleanText($row['question_ta']) : null,
                'option_a_ta' => $row['option_a_ta'] ? cleanText($row['option_a_ta']) : null,
                'option_b_ta' => $row['option_b_ta'] ? cleanText($row['option_b_ta']) : null,
                'option_c_ta' => $row['option_c_ta'] ? cleanText($row['option_c_ta']) : null,
                'option_d_ta' => $row['option_d_ta'] ? cleanText($row['option_d_ta']) : null,
                'explanation_ta' => $row['explanation_ta'] ? cleanText($row['explanation_ta']) : null,
                
                // Language availability flags
                'has_english' => $hasEnglish,
                'has_tamil' => $hasTamil,
                
                // Table data (if exists)
                'table_data' => !empty($row['table_data']) ? json_decode($row['table_data'], true) : null
            ];

            $questions[] = $formatted;
        }

        http_response_code(200);
        echo json_encode([
            'success' => true,
            'count' => count($questions),
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
