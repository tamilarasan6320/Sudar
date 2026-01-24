<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication
require_once '../../models/ExamCategory.php';

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    // Get all exam categories
    $examCategory = new ExamCategory($db);
    $stmt = $examCategory->getAll();
    $categories = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $categories[] = $row;
    }
    http_response_code(200);
    echo json_encode(['success' => true, 'categories' => $categories]);

} elseif ($method === 'POST') {
    // Create new exam category
    $data = json_decode(file_get_contents('php://input'));
    
    if (!empty($data->name)) {
        $examCategory = new ExamCategory($db);
        $examCategory->name = $data->name;
        $examCategory->description = $data->description ?? '';
        $examCategory->icon = $data->icon ?? '';
        $examCategory->is_active = isset($data->is_active) ? $data->is_active : true;
        $examCategory->display_order = $data->display_order ?? 0;
        
        if ($examCategory->create()) {
            http_response_code(201);
            echo json_encode([
                'success' => true,
                'message' => 'Exam category created successfully',
                'id' => $examCategory->id
            ]);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to create exam category']);
        }
    } else {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Name is required']);
    }

} elseif ($method === 'PUT') {
    // Update exam category
    $data = json_decode(file_get_contents('php://input'));
    
    if (!empty($data->id) && !empty($data->name)) {
        $examCategory = new ExamCategory($db);
        $examCategory->id = $data->id;
        $examCategory->name = $data->name;
        $examCategory->description = $data->description ?? '';
        $examCategory->icon = $data->icon ?? '';
        $examCategory->is_active = isset($data->is_active) ? $data->is_active : true;
        $examCategory->display_order = $data->display_order ?? 0;
        
        if ($examCategory->update()) {
            http_response_code(200);
            echo json_encode(['success' => true, 'message' => 'Exam category updated successfully']);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to update exam category']);
        }
    } else {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'ID and name are required']);
    }

} elseif ($method === 'DELETE') {
    // Delete exam category
    $data = json_decode(file_get_contents('php://input'));
    
    if (!empty($data->id)) {
        $examCategory = new ExamCategory($db);
        $examCategory->id = $data->id;
        
        if ($examCategory->delete()) {
            http_response_code(200);
            echo json_encode(['success' => true, 'message' => 'Exam category deleted successfully']);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to delete exam category']);
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

