<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../models/Question.php';
require_once '../../models/QuestionSession.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'POST' || $_SERVER['REQUEST_METHOD'] === 'DELETE') {
    $data = json_decode(file_get_contents('php://input'));
    
    if (!empty($data->session_id)) {
        try {
            $question = new Question($db);
            $question->session_id = $data->session_id;
            
            if ($question->deleteBySession()) {
                // Update session question count to 0
                $session = new QuestionSession($db);
                $session->id = $data->session_id;
                $session->updateQuestionCount();
                
                http_response_code(200);
                echo json_encode([
                    'success' => true,
                    'message' => 'All questions cleared from session successfully'
                ]);
            } else {
                http_response_code(500);
                echo json_encode([
                    'success' => false,
                    'message' => 'Failed to clear questions'
                ]);
            }
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Error clearing questions',
                'error' => $e->getMessage()
            ]);
        }
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

