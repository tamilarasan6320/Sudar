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
    
    if (!empty($data->name) && !empty($data->test_category_id)) {
        $session = new QuestionSession($db);
        $session->test_category_id = $data->test_category_id;
        $session->name = $data->name;
        $session->description = $data->description ?? '';
        $session->total_questions = $data->total_questions ?? 0;
        $session->duration = $data->duration ?? 60;
        $session->difficulty = $data->difficulty ?? 'medium';
        
        if ($session->create()) {
            http_response_code(201);
            echo json_encode([
                'success' => true,
                'message' => 'Question session created successfully',
                'id' => $session->id
            ]);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to create question session']);
        }
    } else {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Name and test category ID are required']);
    }

} elseif ($method === 'PUT') {
    // Update question session
    $data = json_decode(file_get_contents('php://input'));
    
    if (!empty($data->id) && !empty($data->name) && !empty($data->test_category_id)) {
        $session = new QuestionSession($db);
        $session->id = $data->id;
        $session->test_category_id = $data->test_category_id;
        $session->name = $data->name;
        $session->description = $data->description ?? '';
        $session->total_questions = $data->total_questions ?? 0;
        $session->duration = $data->duration ?? 60;
        $session->difficulty = $data->difficulty ?? 'medium';
        
        if ($session->update()) {
            http_response_code(200);
            echo json_encode(['success' => true, 'message' => 'Question session updated successfully']);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to update question session']);
        }
    } else {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'ID, name, and test category ID are required']);
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

