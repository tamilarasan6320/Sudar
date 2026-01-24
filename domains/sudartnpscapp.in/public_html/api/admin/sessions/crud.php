<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication
require_once '../../models/QuestionSession.php';

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    // Get all question sessions
    $category_id = isset($_GET['category_id']) ? $_GET['category_id'] : null;
    $session = new QuestionSession($db);
    $stmt = $session->getAll($category_id);
    $sessions = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $sessions[] = $row;
    }
    http_response_code(200);
    echo json_encode(['success' => true, 'sessions' => $sessions]);

} elseif ($method === 'POST') {
    // Create new question session
    $data = json_decode(file_get_contents('php://input'));
    
    // ✅ Professional Validation
    if (empty($data->name)) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Session name is required']);
        exit;
    }
    
    if (empty($data->test_category_id)) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Test category ID is required']);
        exit;
    }
    
    if (!isset($data->total_questions) || intval($data->total_questions) < 0) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Total questions must be 0 or greater']);
        exit;
    }
    
    if (!isset($data->duration) || intval($data->duration) <= 0) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Duration must be greater than 0']);
        exit;
    }
    
    $session = new QuestionSession($db);
    $session->test_category_id = intval($data->test_category_id);
    $session->name = trim($data->name);
    $session->description = $data->description ?? '';
    $session->total_questions = intval($data->total_questions);
    $session->duration = intval($data->duration);
    $session->difficulty = $data->difficulty ?? 'medium';
    
    // 📝 Debug log
    error_log("💾 Creating session: " . json_encode([
        'name' => $session->name,
        'test_category_id' => $session->test_category_id,
        'total_questions' => $session->total_questions,
        'duration' => $session->duration
    ]));
    
    if ($session->create()) {
        http_response_code(201);
        echo json_encode([
            'success' => true,
            'message' => 'Question session created successfully',
            'id' => $session->id,
            'data' => [
                'name' => $session->name,
                'total_questions' => $session->total_questions,
                'duration' => $session->duration
            ]
        ]);
    } else {
        http_response_code(500);
        echo json_encode(['success' => false, 'message' => 'Failed to create question session in database']);
    }

} elseif ($method === 'PUT') {
    // Update question session
    $rawInput = file_get_contents('php://input');
    $data = json_decode($rawInput);
    
    // Log received data for debugging
    error_log("📥 PUT Request received: " . $rawInput);
    
    // Basic validation only
    if (empty($data->id)) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Session ID is required', 'received' => $rawInput]);
        exit;
    }
    
    if (empty($data->name)) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Session name is required', 'received' => $rawInput]);
        exit;
    }
    
    if (empty($data->test_category_id)) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Test category ID is required', 'received' => $rawInput]);
        exit;
    }
    
    $session = new QuestionSession($db);
    $session->id = intval($data->id);
    $session->test_category_id = intval($data->test_category_id);
    $session->name = trim($data->name);
    $session->description = $data->description ?? '';
    $session->total_questions = isset($data->total_questions) ? intval($data->total_questions) : 0;
    $session->duration = isset($data->duration) ? intval($data->duration) : 180;
    $session->difficulty = $data->difficulty ?? 'medium';
    
    error_log("💾 Updating session: " . json_encode([
        'id' => $session->id,
        'name' => $session->name,
        'test_category_id' => $session->test_category_id,
        'total_questions' => $session->total_questions,
        'duration' => $session->duration
    ]));
    
    if ($session->update()) {
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => 'Session updated successfully'
        ]);
    } else {
        http_response_code(500);
        echo json_encode(['success' => false, 'message' => 'Database update failed']);
    }

} elseif ($method === 'DELETE') {
    // Delete question session
    $data = json_decode(file_get_contents('php://input'));
    
    if (!empty($data->id)) {
        $session = new QuestionSession($db);
        $session->id = $data->id;
        
        if ($session->delete()) {
            http_response_code(200);
            echo json_encode(['success' => true, 'message' => 'Question session deleted successfully']);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to delete question session']);
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

