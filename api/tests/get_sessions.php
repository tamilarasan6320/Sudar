<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/QuestionSession.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $category_id = isset($_GET['category_id']) ? $_GET['category_id'] : null;

    $session = new QuestionSession($db);
    $stmt = $session->getAll($category_id);

    $sessions = [];

    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $sessions[] = $row;
    }

    http_response_code(200);
    echo json_encode([
        'success' => true,
        'count' => count($sessions),
        'sessions' => $sessions
    ]);
} else {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed'
    ]);
}
?>
