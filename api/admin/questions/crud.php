<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
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
        
        // Check for duplicates
        if ($question->checkDuplicate()) {
            http_response_code(409);
            echo json_encode(['success' => false, 'message' => 'Question already exists in this session']);
            exit;
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
            echo json_encode(['success' => false, 'message' => 'Failed to create question']);
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
    // Delete question
    $data = json_decode(file_get_contents('php://input'));
    
    if (!empty($data->id)) {
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
            
            http_response_code(200);
            echo json_encode(['success' => true, 'message' => 'Question deleted successfully']);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to delete question']);
        }
    } else {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'ID is required']);
    }

} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
}
?>

