<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/TestResult.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $user_id = isset($_GET['user_id']) ? $_GET['user_id'] : null;
    $limit = isset($_GET['limit']) ? intval($_GET['limit']) : 50;

    if (!empty($user_id)) {
        $result = new TestResult($db);
        $result->user_id = $user_id;
        $stmt = $result->getUserHistory($limit);

        $history = [];

        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $history[] = [
                'id' => $row['id'],
                'test_name' => $row['test_name'],
                'category_name' => $row['category_name'],
                'total_questions' => $row['total_questions'],
                'attempted' => $row['attempted_questions'],
                'correct' => $row['correct_answers'],
                'wrong' => $row['wrong_answers'],
                'unanswered' => $row['unanswered'],
                'score' => $row['score'],
                'percentage' => round($row['percentage'], 2),
                'rank' => $row['rank'],
                'time_taken' => $row['time_taken'],
                'submitted_at' => $row['submitted_at']
            ];
        }

        http_response_code(200);
        echo json_encode([
            'success' => true,
            'count' => count($history),
            'history' => $history
        ]);
    } else {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'User ID is required'
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed'
    ]);
}
?>
