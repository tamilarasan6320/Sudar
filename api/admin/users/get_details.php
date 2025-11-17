<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication
require_once '../../models/User.php';
require_once '../../models/TestResult.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
    
    if ($user_id <= 0) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'User ID is required'
        ]);
        exit;
    }
    
    try {
        // Get user basic info and stats
        $user = new User($db);
        $user->id = $user_id;
        $stmt = $user->getUserStats();
        
        if ($stmt->rowCount() === 0) {
            http_response_code(404);
            echo json_encode([
                'success' => false,
                'message' => 'User not found'
            ]);
            exit;
        }
        
        $user_data = $stmt->fetch(PDO::FETCH_ASSOC);
        
        // Get detailed performance
        $result = new TestResult($db);
        $result->user_id = $user_id;
        $perf_stmt = $result->getUserPerformance();
        $performance = $perf_stmt->fetch(PDO::FETCH_ASSOC);
        
        // Get test history
        $history_stmt = $result->getUserHistory(50);
        $test_history = [];
        while ($row = $history_stmt->fetch(PDO::FETCH_ASSOC)) {
            $test_history[] = $row;
        }
        
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'user' => $user_data,
            'performance' => $performance,
            'test_history' => $test_history
        ]);
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Error fetching user details',
            'error' => $e->getMessage()
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

