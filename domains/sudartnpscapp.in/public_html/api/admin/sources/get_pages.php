<?php
/**
 * Get pages for a specific source
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

if (empty($_GET['source_id'])) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Source ID is required']);
    exit;
}

$sourceId = intval($_GET['source_id']);

try {
    $database = new Database();
    $db = $database->getConnection();
    
    $stmt = $db->prepare("
        SELECT id, title, content, page_order, created_at, updated_at 
        FROM html_source_pages 
        WHERE source_id = ? 
        ORDER BY page_order ASC, id ASC
    ");
    $stmt->execute([$sourceId]);
    
    $pages = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $pages[] = [
            'id' => intval($row['id']),
            'title' => $row['title'],
            'content' => $row['content'],
            'page_order' => intval($row['page_order']),
            'created_at' => $row['created_at'],
            'updated_at' => $row['updated_at']
        ];
    }
    
    echo json_encode([
        'success' => true,
        'pages' => $pages
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
