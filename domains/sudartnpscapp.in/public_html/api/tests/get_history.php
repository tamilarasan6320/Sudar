<?php
// Disable error display to prevent HTML in JSON response
ini_set('display_errors', 0);
error_reporting(E_ALL);

require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/TestResult.php';

try {
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
                'id' => (int)$row['id'],
                'session_id' => (int)$row['session_id'],
                'test_name' => $row['test_name'],
                'category_name' => $row['category_name'],
                'total_questions' => (int)$row['total_questions'],
                'attempted' => (int)$row['attempted_questions'],
                'correct' => (int)$row['correct_answers'],
                'wrong' => (int)$row['wrong_answers'],
                'unanswered' => (int)$row['unanswered'],
                'score' => (float)$row['score'],
                'percentage' => round((float)$row['percentage'], 2),
                'rank' => $row['rank'] ? (int)$row['rank'] : null,
                'time_taken' => (int)$row['time_taken'],
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
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ]);
}
?>
