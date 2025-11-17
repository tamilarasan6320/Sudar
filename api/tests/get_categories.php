<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/TestCategory.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $exam_id = isset($_GET['exam_id']) ? intval($_GET['exam_id']) : null;
    $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : null;

    $testCategory = new TestCategory($db);
    $stmt = $testCategory->getAll($exam_id, $user_id);

    $categories = [];

    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        // Ensure counts are integers
        $row['sessions_count'] = intval($row['sessions_count'] ?? 0);
        $row['completed_count'] = intval($row['completed_count'] ?? 0);
        $categories[] = $row;
    }

    http_response_code(200);
    echo json_encode([
        'success' => true,
        'count' => count($categories),
        'categories' => $categories
    ]);
} else {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed'
    ]);
}
?>
