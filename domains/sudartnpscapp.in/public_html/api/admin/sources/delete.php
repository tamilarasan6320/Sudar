<?php
/**
 * Delete a source and all its pages
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

if (empty($data->id)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Source ID is required']);
    exit;
}

$sourceId = intval($data->id);

try {
    $database = new Database();
    $db = $database->getConnection();
    
    // Delete pages first (in case CASCADE doesn't work)
    $stmt = $db->prepare("DELETE FROM html_source_pages WHERE source_id = ?");
    $stmt->execute([$sourceId]);
    
    // Delete the source
    $stmt = $db->prepare("DELETE FROM html_sources WHERE id = ?");
    $stmt->execute([$sourceId]);
    
    if ($stmt->rowCount() > 0) {
        echo json_encode([
            'success' => true,
            'message' => 'Source deleted successfully'
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'Source not found'
        ]);
    }
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
