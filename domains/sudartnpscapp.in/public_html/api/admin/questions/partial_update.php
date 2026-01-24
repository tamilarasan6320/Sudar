<?php
/**
 * Partial Update API - Updates only specific questions by question number
 * Questions not in the uploaded file remain unchanged
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php';
require_once '../../models/Question.php';
require_once '../../models/QuestionSession.php';

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

if ($method !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed. Use POST.']);
    exit;
}

$data = json_decode(file_get_contents('php://input'));

if (empty($data->session_id) || empty($data->questions)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Session ID and questions array are required']);
    exit;
}

$session_id = intval($data->session_id);
$questions = $data->questions;

$updated_count = 0;
$added_count = 0;
$error_count = 0;
$errors = [];

try {
    $db->beginTransaction();
    
    // Get existing questions for this session
    $stmt = $db->prepare("SELECT id, question_number FROM questions WHERE session_id = ?");
    $stmt->execute([$session_id]);
    $existingQuestions = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $existingQuestions[$row['question_number']] = $row['id'];
    }
    
    foreach ($questions as $q) {
        $questionNumber = $q->questionNumber ?? null;
        
        if (!$questionNumber) {
            $error_count++;
            $errors[] = "Question missing questionNumber field";
            continue;
        }
        
        // Prepare question data
        $question_en = $q->question ?? null;
        $question_ta = $q->question_ta ?? null;
        
        // Options - handle both formats
        $option_a_en = $q->options->A ?? null;
        $option_b_en = $q->options->B ?? null;
        $option_c_en = $q->options->C ?? null;
        $option_d_en = $q->options->D ?? null;
        
        $option_a_ta = $q->options_ta->A ?? ($q->options->A ?? null);
        $option_b_ta = $q->options_ta->B ?? ($q->options->B ?? null);
        $option_c_ta = $q->options_ta->C ?? ($q->options->C ?? null);
        $option_d_ta = $q->options_ta->D ?? ($q->options->D ?? null);
        
        // If main options are in Tamil (no English), swap them
        if (!$question_en && $question_ta) {
            // Tamil only - keep Tamil options in Tamil fields
        } elseif ($question_en && !$question_ta) {
            // English only - keep English options in English fields
            $option_a_ta = null;
            $option_b_ta = null;
            $option_c_ta = null;
            $option_d_ta = null;
        }
        
        $correct_answer = strtoupper($q->correctAnswer ?? 'A');
        $explanation_en = $q->answerExplanation ?? null;
        $explanation_ta = $q->answerExplanation_ta ?? $explanation_en;
        
        // Handle table data if present
        $table_data = null;
        if (!empty($q->table)) {
            $table_data = json_encode($q->table);
        }
        
        // Check if question exists
        if (isset($existingQuestions[$questionNumber])) {
            // UPDATE existing question
            $questionId = $existingQuestions[$questionNumber];
            
            $updateStmt = $db->prepare("
                UPDATE questions SET
                    question_en = COALESCE(?, question_en),
                    question_ta = COALESCE(?, question_ta),
                    option_a_en = COALESCE(?, option_a_en),
                    option_b_en = COALESCE(?, option_b_en),
                    option_c_en = COALESCE(?, option_c_en),
                    option_d_en = COALESCE(?, option_d_en),
                    option_a_ta = COALESCE(?, option_a_ta),
                    option_b_ta = COALESCE(?, option_b_ta),
                    option_c_ta = COALESCE(?, option_c_ta),
                    option_d_ta = COALESCE(?, option_d_ta),
                    correct_answer = ?,
                    explanation_en = COALESCE(?, explanation_en),
                    explanation_ta = COALESCE(?, explanation_ta),
                    table_data = COALESCE(?, table_data),
                    updated_at = NOW()
                WHERE id = ?
            ");
            
            $result = $updateStmt->execute([
                $question_en,
                $question_ta,
                $option_a_en,
                $option_b_en,
                $option_c_en,
                $option_d_en,
                $option_a_ta,
                $option_b_ta,
                $option_c_ta,
                $option_d_ta,
                $correct_answer,
                $explanation_en,
                $explanation_ta,
                $table_data,
                $questionId
            ]);
            
            if ($result) {
                $updated_count++;
            } else {
                $error_count++;
                $errors[] = "Failed to update Q{$questionNumber}";
            }
        } else {
            // INSERT new question
            $insertStmt = $db->prepare("
                INSERT INTO questions (
                    session_id, question_number, question_en, question_ta,
                    option_a_en, option_b_en, option_c_en, option_d_en,
                    option_a_ta, option_b_ta, option_c_ta, option_d_ta,
                    correct_answer, explanation_en, explanation_ta, table_data,
                    difficulty, marks, negative_marks, display_order, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'medium', 1, 0, ?, NOW())
            ");
            
            $result = $insertStmt->execute([
                $session_id,
                $questionNumber,
                $question_en,
                $question_ta,
                $option_a_en,
                $option_b_en,
                $option_c_en,
                $option_d_en,
                $option_a_ta,
                $option_b_ta,
                $option_c_ta,
                $option_d_ta,
                $correct_answer,
                $explanation_en,
                $explanation_ta,
                $table_data,
                $questionNumber
            ]);
            
            if ($result) {
                $added_count++;
            } else {
                $error_count++;
                $errors[] = "Failed to add Q{$questionNumber}";
            }
        }
    }
    
    // Update session question count
    $session = new QuestionSession($db);
    $session->id = $session_id;
    $session->updateQuestionCount();
    
    $db->commit();
    
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => "Partial update completed",
        'updated_count' => $updated_count,
        'added_count' => $added_count,
        'error_count' => $error_count,
        'errors' => $errors
    ]);
    
} catch (Exception $e) {
    $db->rollBack();
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>

