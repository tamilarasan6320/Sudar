<?php
require_once '../config/cors.php';
require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    try {
        $users_query = "SELECT COUNT(*) AS total FROM users";
        $users_stmt = $db->query($users_query);
        $total_users = $users_stmt->fetch(PDO::FETCH_ASSOC)['total'];

        $exams_query = "SELECT COUNT(*) AS total FROM exam_categories WHERE is_active = 1";
        $exams_stmt = $db->query($exams_query);
        $total_exams = $exams_stmt->fetch(PDO::FETCH_ASSOC)['total'];

        $categories_query = "SELECT COUNT(*) AS total FROM test_categories WHERE is_active = 1";
        $categories_stmt = $db->query($categories_query);
        $total_categories = $categories_stmt->fetch(PDO::FETCH_ASSOC)['total'];

        $sessions_query = "SELECT COUNT(*) AS total FROM question_sessions WHERE is_active = 1";
        $sessions_stmt = $db->query($sessions_query);
        $total_sessions = $sessions_stmt->fetch(PDO::FETCH_ASSOC)['total'];

        $questions_query = "SELECT COUNT(*) AS total FROM questions";
        $questions_stmt = $db->query($questions_query);
        $total_questions = $questions_stmt->fetch(PDO::FETCH_ASSOC)['total'];

        $results_query = "SELECT COUNT(*) AS total FROM test_results";
        $results_stmt = $db->query($results_query);
        $total_results = $results_stmt->fetch(PDO::FETCH_ASSOC)['total'];

        $active_users_query = "SELECT COUNT(*) AS total FROM users WHERE last_login >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
        $active_users_stmt = $db->query($active_users_query);
        $active_users = $active_users_stmt->fetch(PDO::FETCH_ASSOC)['total'];

        $tests_today_query = "SELECT COUNT(*) AS total FROM test_results WHERE DATE(submitted_at) = CURDATE()";
        $tests_today_stmt = $db->query($tests_today_query);
        $tests_today = $tests_today_stmt->fetch(PDO::FETCH_ASSOC)['total'];

        $avg_score_query = "SELECT AVG(percentage) AS avg_score FROM test_results";
        $avg_score_stmt = $db->query($avg_score_query);
        $avg_score = round($avg_score_stmt->fetch(PDO::FETCH_ASSOC)['avg_score'], 2);

        $recent_users_query = "SELECT id, name, mobile, created_at FROM users ORDER BY created_at DESC LIMIT 5";
        $recent_users_stmt = $db->query($recent_users_query);
        $recent_users = $recent_users_stmt->fetchAll(PDO::FETCH_ASSOC);

        $recent_results_query = "SELECT tr.*, u.name AS user_name, qs.name AS test_name
                                 FROM test_results tr
                                 LEFT JOIN users u ON tr.user_id = u.id
                                 LEFT JOIN question_sessions qs ON tr.session_id = qs.id
                                 ORDER BY tr.submitted_at DESC LIMIT 5";
        $recent_results_stmt = $db->query($recent_results_query);
        $recent_results = $recent_results_stmt->fetchAll(PDO::FETCH_ASSOC);

        http_response_code(200);
        echo json_encode([
            'success' => true,
            'stats' => [
                'total_users' => $total_users,
                'total_exams' => $total_exams,
                'total_categories' => $total_categories,
                'total_sessions' => $total_sessions,
                'total_questions' => $total_questions,
                'total_results' => $total_results,
                'active_users' => $active_users,
                'tests_today' => $tests_today,
                'avg_score' => $avg_score
            ],
            'recent_users' => $recent_users,
            'recent_results' => $recent_results
        ]);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Error fetching dashboard stats',
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
