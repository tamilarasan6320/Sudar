<?php
/**
 * Save pages for a source (replaces all pages)
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

if (empty($data->source_id)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Source ID is required']);
    exit;
}

$sourceId = intval($data->source_id);
$pages = $data->pages ?? [];

// Log request info
error_log("save_pages.php: Source ID=$sourceId, Pages=" . count($pages));

if (empty($pages)) {
    echo json_encode(['success' => false, 'message' => 'No pages to save']);
    exit;
}

try {
    $database = new Database();
    $db = $database->getConnection();
    
    $db->beginTransaction();
    
    // Delete existing pages
    $stmt = $db->prepare("DELETE FROM html_source_pages WHERE source_id = ?");
    $stmt->execute([$sourceId]);
    
    // Insert new pages
    $stmt = $db->prepare("
        INSERT INTO html_source_pages (source_id, title, content, page_order) 
        VALUES (?, ?, ?, ?)
    ");
    
    foreach ($pages as $index => $page) {
        $pageOrder = $page->page_order ?? ($index + 1);
        $stmt->execute([
            $sourceId,
            $page->title ?? "Page " . $pageOrder,
            $page->content ?? '',
            $pageOrder
        ]);
    }
    
    // Update source's updated_at timestamp
    $stmt = $db->prepare("UPDATE html_sources SET updated_at = NOW() WHERE id = ?");
    $stmt->execute([$sourceId]);
    
    $db->commit();
    
    echo json_encode([
        'success' => true,
        'message' => 'Pages saved successfully',
        'page_count' => count($pages)
    ]);
    
} catch (Exception $e) {
    if (isset($db)) {
        $db->rollBack();
    }
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
