<?php
/**
 * Create a new source
 */

// Explicit CORS headers
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
header('Content-Type: application/json; charset=UTF-8');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../../config/database.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

$data = json_decode(file_get_contents('php://input'));

if (empty($data->name)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Source name is required']);
    exit;
}

try {
    $database = new Database();
    $db = $database->getConnection();
    
    $stmt = $db->prepare("INSERT INTO html_sources (name) VALUES (?)");
    $stmt->execute([$data->name]);
    
    $sourceId = $db->lastInsertId();
    
    echo json_encode([
        'success' => true,
        'message' => 'Source created successfully',
        'id' => intval($sourceId),
        'source_id' => intval($sourceId)
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
