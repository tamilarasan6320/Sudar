<?php
/**
 * List all sources
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

try {
    $database = new Database();
    $db = $database->getConnection();
    
    // Check if table exists, create if not
    $db->exec("
        CREATE TABLE IF NOT EXISTS html_sources (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        )
    ");
    
    $db->exec("
        CREATE TABLE IF NOT EXISTS html_source_pages (
            id INT AUTO_INCREMENT PRIMARY KEY,
            source_id INT NOT NULL,
            title VARCHAR(255),
            content LONGTEXT,
            page_order INT DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            FOREIGN KEY (source_id) REFERENCES html_sources(id) ON DELETE CASCADE
        )
    ");
    
    // Get all sources with page count
    $stmt = $db->query("
        SELECT s.*, COUNT(p.id) as page_count 
        FROM html_sources s 
        LEFT JOIN html_source_pages p ON s.id = p.source_id 
        GROUP BY s.id 
        ORDER BY s.created_at DESC
    ");
    
    $sources = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $sources[] = [
            'id' => intval($row['id']),
            'name' => $row['name'],
            'page_count' => intval($row['page_count']),
            'created_at' => $row['created_at'],
            'updated_at' => $row['updated_at'],
            'pages' => []
        ];
    }
    
    echo json_encode([
        'success' => true,
        'sources' => $sources
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
