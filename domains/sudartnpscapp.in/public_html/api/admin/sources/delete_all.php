<?php
/**
 * Delete ALL sources and their pages
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

try {
    $database = new Database();
    $db = $database->getConnection();
    
    // Get count before delete
    $countStmt = $db->query("SELECT COUNT(*) FROM html_sources");
    $totalSources = $countStmt->fetchColumn();
    
    // Delete all pages first
    $db->exec("DELETE FROM html_source_pages");
    
    // Delete all sources
    $db->exec("DELETE FROM html_sources");
    
    echo json_encode([
        'success' => true,
        'deleted_count' => $totalSources,
        'message' => "Deleted $totalSources sources successfully"
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}

