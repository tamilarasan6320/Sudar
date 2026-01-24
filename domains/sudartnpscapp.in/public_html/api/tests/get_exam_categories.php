<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/ExamCategory.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $examCategory = new ExamCategory($db);
    $stmt = $examCategory->getAll();

    $categories = [];

    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $categories[] = $row;
    }

    http_response_code(200);
    echo json_encode([
        'success' => true,
        'count' => count($categories),
        'categories' => $categories,
    ]);
} else {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed',
    ]);
}
?>
