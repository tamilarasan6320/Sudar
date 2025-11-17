<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication
require_once '../../models/TestCategory.php';

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    // Get all test categories
    $exam_id = isset($_GET['exam_id']) ? $_GET['exam_id'] : null;
    $testCategory = new TestCategory($db);
    $stmt = $testCategory->getAll($exam_id);
    $categories = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $categories[] = $row;
    }
    http_response_code(200);
    echo json_encode(['success' => true, 'categories' => $categories]);

} elseif ($method === 'POST') {
    // Create new test category
    $data = json_decode(file_get_contents('php://input'));
    
    if (!empty($data->name) && !empty($data->exam_category_id)) {
        $testCategory = new TestCategory($db);
        $testCategory->exam_category_id = $data->exam_category_id;
        $testCategory->name = $data->name;
        $testCategory->description = $data->description ?? '';
        $testCategory->icon = $data->icon ?? '';
        $testCategory->color = $data->color ?? '';
        $testCategory->is_active = isset($data->is_active) ? $data->is_active : true;
        $testCategory->display_order = $data->display_order ?? 0;
        
        if ($testCategory->create()) {
            http_response_code(201);
            echo json_encode([
                'success' => true,
                'message' => 'Test category created successfully',
                'id' => $testCategory->id
            ]);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to create test category']);
        }
    } else {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Name and exam category ID are required']);
    }

} elseif ($method === 'PUT') {
    // Update test category
    $data = json_decode(file_get_contents('php://input'));
    
    if (!empty($data->id) && !empty($data->name) && !empty($data->exam_category_id)) {
        $testCategory = new TestCategory($db);
        $testCategory->id = $data->id;
        $testCategory->exam_category_id = $data->exam_category_id;
        $testCategory->name = $data->name;
        $testCategory->description = $data->description ?? '';
        $testCategory->icon = $data->icon ?? '';
        $testCategory->color = $data->color ?? '';
        $testCategory->is_active = isset($data->is_active) ? $data->is_active : true;
        $testCategory->display_order = $data->display_order ?? 0;
        
        if ($testCategory->update()) {
            http_response_code(200);
            echo json_encode(['success' => true, 'message' => 'Test category updated successfully']);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to update test category']);
        }
    } else {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'ID, name, and exam category ID are required']);
    }

} elseif ($method === 'DELETE') {
    // Delete test category
    $data = json_decode(file_get_contents('php://input'));
    
    if (!empty($data->id)) {
        $testCategory = new TestCategory($db);
        $testCategory->id = $data->id;
        
        if ($testCategory->delete()) {
            http_response_code(200);
            echo json_encode(['success' => true, 'message' => 'Test category deleted successfully']);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to delete test category']);
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

