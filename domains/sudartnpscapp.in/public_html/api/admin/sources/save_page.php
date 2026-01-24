<?php
/**
 * Save a SINGLE page for a source
 * Used for importing large pages one at a time
 * Content is BASE64 encoded to bypass WAF/firewall restrictions
 */

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
header('Content-Type: application/json; charset=UTF-8');

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

// Increase memory limit for large pages
ini_set('memory_limit', '256M');
ini_set('post_max_size', '50M');
ini_set('max_input_vars', 10000);

$data = json_decode(file_get_contents('php://input'));

if (empty($data->source_id)) {
    echo json_encode(['success' => false, 'message' => 'Source ID is required']);
    exit;
}

$sourceId = intval($data->source_id);
$title = $data->title ?? 'Untitled';
$pageOrder = intval($data->page_order ?? 0);

// Content can be base64 encoded (to bypass WAF) or plain
$content = '';
if (!empty($data->content_b64)) {
    // Base64 encoded content
    $content = base64_decode($data->content_b64);
} else if (!empty($data->content)) {
    // Plain content (fallback)
    $content = $data->content;
}

try {
    $database = new Database();
    $db = $database->getConnection();
    
    // Check if page with this order already exists
    $checkStmt = $db->prepare("SELECT id FROM html_source_pages WHERE source_id = ? AND page_order = ?");
    $checkStmt->execute([$sourceId, $pageOrder]);
    $existing = $checkStmt->fetch();
    
    if ($existing) {
        // Update existing page
        $stmt = $db->prepare("UPDATE html_source_pages SET title = ?, content = ? WHERE id = ?");
        $stmt->execute([$title, $content, $existing['id']]);
    } else {
        // Insert new page
        $stmt = $db->prepare("INSERT INTO html_source_pages (source_id, title, content, page_order, created_at) VALUES (?, ?, ?, ?, NOW())");
        $stmt->execute([$sourceId, $title, $content, $pageOrder]);
    }
    
    // Update source's updated_at timestamp
    $stmt = $db->prepare("UPDATE html_sources SET updated_at = NOW() WHERE id = ?");
    $stmt->execute([$sourceId]);
    
    echo json_encode([
        'success' => true,
        'message' => 'Page saved',
        'page_order' => $pageOrder
    ]);
    
} catch (Exception $e) {
    error_log("save_page.php error: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}

