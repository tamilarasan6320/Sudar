<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/TestResult.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $session_id = isset($_GET['session_id']) ? intval($_GET['session_id']) : null;
    $category_id = isset($_GET['category_id']) ? intval($_GET['category_id']) : null;
    $limit = isset($_GET['limit']) ? intval($_GET['limit']) : 100;
    $period = isset($_GET['period']) ? $_GET['period'] : 'all'; // week, month, year, all

    try {
        $date_condition = '';
        if ($period === 'week') {
            $date_condition = "AND tr.submitted_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
        } elseif ($period === 'month') {
            $date_condition = "AND tr.submitted_at >= DATE_SUB(NOW(), INTERVAL 1 MONTH)";
        } elseif ($period === 'year') {
            $date_condition = "AND tr.submitted_at >= DATE_SUB(NOW(), INTERVAL 1 YEAR)";
        }

        $where_conditions = ["1=1"];
        $params = [];

        if ($session_id) {
            $where_conditions[] = "tr.session_id = ?";
            $params[] = $session_id;
        }

        if ($category_id) {
            $where_conditions[] = "qs.test_category_id = ?";
            $params[] = $category_id;
        }

        $where_clause = implode(" AND ", $where_conditions);

        // Get leaderboard with aggregated stats per user
        $query = "SELECT u.id as user_id,
                         u.name as user_name,
                         u.mobile as user_mobile,
                         COUNT(tr.id) as total_tests,
                         AVG(tr.percentage) as avg_score,
                         MAX(tr.percentage) as best_score,
                         SUM(tr.correct_answers) as total_correct,
                         SUM(tr.total_questions) as total_questions_attempted,
                         SUM(tr.time_taken) as total_time_spent
                  FROM users u
                  INNER JOIN test_results tr ON u.id = tr.user_id
                  LEFT JOIN question_sessions qs ON tr.session_id = qs.id
                  WHERE $where_clause $date_condition
                  GROUP BY u.id, u.name, u.mobile
                  HAVING total_tests > 0
                  ORDER BY avg_score DESC, total_tests DESC, best_score DESC
                  LIMIT ?";
        
        $params[] = $limit;
        $stmt = $db->prepare($query);
        $stmt->execute($params);
        $leaderboard = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Calculate ranks
        $ranked_leaderboard = [];
        $rank = 1;
        $prev_score = null;
        $prev_tests = null;

        foreach ($leaderboard as $index => $entry) {
            $current_score = round($entry['avg_score'], 2);
            $current_tests = $entry['total_tests'];

            // If score or test count changes, update rank
            if ($prev_score !== null && ($prev_score != $current_score || $prev_tests != $current_tests)) {
                $rank = $index + 1;
            }

            $ranked_leaderboard[] = [
                'rank' => $rank,
                'user_id' => $entry['user_id'],
                'user_name' => $entry['user_name'],
                'user_mobile' => $entry['user_mobile'],
                'total_tests' => (int)$entry['total_tests'],
                'avg_score' => round($entry['avg_score'], 1),
                'best_score' => round($entry['best_score'], 1),
                'total_correct' => (int)$entry['total_correct'],
                'total_questions_attempted' => (int)$entry['total_questions_attempted'],
                'total_time_spent' => (int)$entry['total_time_spent'],
                'accuracy' => $entry['total_questions_attempted'] > 0 
                    ? round(($entry['total_correct'] / $entry['total_questions_attempted']) * 100, 1) 
                    : 0
            ];

            $prev_score = $current_score;
            $prev_tests = $current_tests;
        }

        http_response_code(200);
        echo json_encode([
            'success' => true,
            'leaderboard' => $ranked_leaderboard,
            'count' => count($ranked_leaderboard),
            'filters' => [
                'session_id' => $session_id,
                'category_id' => $category_id,
                'period' => $period,
                'limit' => $limit
            ]
        ]);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Error fetching leaderboard',
            'error' => $e->getMessage()
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed.']);
}
?>

