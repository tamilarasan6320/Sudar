<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication
require_once '../../models/Question.php';
require_once '../../models/QuestionSession.php';

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    // Get questions for a session
    if (!empty($_GET['session_id'])) {
        $question = new Question($db);
        $question->session_id = $_GET['session_id'];
        $stmt = $question->getBySession();
        $questions = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $questions[] = $row;
        }
        http_response_code(200);
        echo json_encode(['success' => true, 'questions' => $questions]);
    } else {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Session ID is required']);
    }

} elseif ($method === 'POST') {
    // Create new question
    $data = json_decode(file_get_contents('php://input'));
    
    if (!empty($data->session_id) && !empty($data->correct_answer)) {
        // Validate: Must have question text AND 4 options in at least one language
        // Note: Question text can be in either field (some scrapers put Tamil in question_en)
        $hasQuestionText = !empty($data->question_en) || !empty($data->question_ta);
        
        $hasEnglishOptions = !empty($data->option_a_en) && 
                             !empty($data->option_b_en) && 
                             !empty($data->option_c_en) && 
                             !empty($data->option_d_en);
        
        $hasTamilOptions = !empty($data->option_a_ta) && 
                           !empty($data->option_b_ta) && 
                           !empty($data->option_c_ta) && 
                           !empty($data->option_d_ta);
        
        $hasCompleteOptions = $hasEnglishOptions || $hasTamilOptions;
        
        if (!$hasQuestionText || !$hasCompleteOptions) {
            http_response_code(400);
            echo json_encode([
                'success' => false, 
                'message' => 'Question must have complete data in at least one language (question + all 4 options)',
                'debug' => [
                    'hasQuestionText' => $hasQuestionText,
                    'hasEnglishOptions' => $hasEnglishOptions,
                    'hasTamilOptions' => $hasTamilOptions
                ]
            ]);
            exit;
        }
        
        $question = new Question($db);
        $question->session_id = $data->session_id;
        $question->question_en = $data->question_en ?? null;
        $question->question_ta = $data->question_ta ?? null;
        $question->option_a_en = $data->option_a_en ?? null;
        $question->option_a_ta = $data->option_a_ta ?? null;
        $question->option_b_en = $data->option_b_en ?? null;
        $question->option_b_ta = $data->option_b_ta ?? null;
        $question->option_c_en = $data->option_c_en ?? null;
        $question->option_c_ta = $data->option_c_ta ?? null;
        $question->option_d_en = $data->option_d_en ?? null;
        $question->option_d_ta = $data->option_d_ta ?? null;
        $question->correct_answer = $data->correct_answer;
        $question->explanation_en = $data->explanation_en ?? null;
        $question->explanation_ta = $data->explanation_ta ?? null;
        $question->difficulty = $data->difficulty ?? 'medium';
        $question->marks = $data->marks ?? 1;
        $question->negative_marks = $data->negative_marks ?? 0;
        $question->display_order = $data->display_order ?? 0;
        $question->table_data = $data->table_data ?? null;
        
        // Skip duplicate check for batch uploads (when display_order is set)
        // This allows uploading questions with similar text
        $skipDuplicateCheck = isset($data->display_order) && $data->display_order > 0;
        
        if (!$skipDuplicateCheck) {
            // Check for duplicates only for manual single question adds
            $duplicateId = $question->checkDuplicate();
            
            if ($duplicateId) {
                // Update existing question instead of rejecting
                $question->id = $duplicateId;
                if ($question->update()) {
                    http_response_code(200);
                    echo json_encode([
                        'success' => true,
                        'message' => 'Question updated (was duplicate)',
                        'id' => $question->id,
                        'updated' => true
                    ]);
                } else {
                    http_response_code(500);
                    echo json_encode([
                        'success' => false,
                        'message' => 'Failed to update duplicate question'
                    ]);
                }
                exit;
            }
        }
        
        if ($question->create()) {
            // Update session question count
            $session = new QuestionSession($db);
            $session->id = $data->session_id;
            $session->updateQuestionCount();
            
            http_response_code(201);
            echo json_encode([
                'success' => true,
                'message' => 'Question created successfully',
                'id' => $question->id
            ]);
        } else {
            http_response_code(500);
            echo json_encode([
                'success' => false, 
                'message' => 'Failed to create question. Ensure question has complete data in at least one language.'
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Session ID and correct answer are required']);
    }

} elseif ($method === 'PUT') {
    // Update question
    $data = json_decode(file_get_contents('php://input'));
    
    if (!empty($data->id) && !empty($data->correct_answer)) {
        $question = new Question($db);
        $question->id = $data->id;
        $question->question_en = $data->question_en ?? null;
        $question->question_ta = $data->question_ta ?? null;
        $question->option_a_en = $data->option_a_en ?? null;
        $question->option_a_ta = $data->option_a_ta ?? null;
        $question->option_b_en = $data->option_b_en ?? null;
        $question->option_b_ta = $data->option_b_ta ?? null;
        $question->option_c_en = $data->option_c_en ?? null;
        $question->option_c_ta = $data->option_c_ta ?? null;
        $question->option_d_en = $data->option_d_en ?? null;
        $question->option_d_ta = $data->option_d_ta ?? null;
        $question->correct_answer = $data->correct_answer;
        $question->explanation_en = $data->explanation_en ?? null;
        $question->explanation_ta = $data->explanation_ta ?? null;
        $question->difficulty = $data->difficulty ?? 'medium';
        $question->marks = $data->marks ?? 1;
        $question->negative_marks = $data->negative_marks ?? 0;
        
        if ($question->update()) {
            http_response_code(200);
            echo json_encode(['success' => true, 'message' => 'Question updated successfully']);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to update question']);
        }
    } else {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'ID and correct answer are required']);
    }

} elseif ($method === 'DELETE') {
    // Delete question(s)
    $data = json_decode(file_get_contents('php://input'));
    
    // If session_id is provided, delete all questions in the session
    if (!empty($data->session_id)) {
        try {
            $db->beginTransaction();
            
            $question = new Question($db);
            $question->session_id = $data->session_id;
            
            // Count questions before deleting
            $stmt = $question->getBySession();
            $count = $stmt->rowCount();
            
            // Delete all questions in the session
            if ($question->deleteBySession()) {
                // Update session question count
                $session = new QuestionSession($db);
                $session->id = $data->session_id;
                $session->updateQuestionCount();
                
                $db->commit();
                
                http_response_code(200);
                echo json_encode([
                    'success' => true, 
                    'message' => "Successfully deleted $count questions",
                    'deleted' => $count
                ]);
            } else {
                $db->rollBack();
                http_response_code(500);
                echo json_encode(['success' => false, 'message' => 'Failed to delete questions']);
            }
        } catch (Exception $e) {
            $db->rollBack();
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Error: ' . $e->getMessage()]);
        }
    } 
    // If id is provided, delete single question
    elseif (!empty($data->id)) {
        try {
            $db->beginTransaction();
            
            // Get session_id before deleting
            $question = new Question($db);
            $question->id = $data->id;
            $stmt = $question->getById();
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            $session_id = $row['session_id'];
            
            if ($question->delete()) {
                // Update session question count
                $session = new QuestionSession($db);
                $session->id = $session_id;
                $session->updateQuestionCount();
                
                $db->commit();
                
                http_response_code(200);
                echo json_encode(['success' => true, 'message' => 'Question deleted successfully']);
            } else {
                $db->rollBack();
                http_response_code(500);
                echo json_encode(['success' => false, 'message' => 'Failed to delete question']);
            }
        } catch (Exception $e) {
            $db->rollBack();
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Error: ' . $e->getMessage()]);
        }
    } else {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Session ID or Question ID is required']);
    }

} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
}
?>

