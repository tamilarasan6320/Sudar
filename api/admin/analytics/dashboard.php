<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    try {
        $period = isset($_GET['period']) ? $_GET['period'] : 'week'; // week, month, year, all
        
        $response_data = [
            'success' => true,
            'overview' => [],
            'user_activity' => [],
            'test_performance' => [],
            'category_performance' => [],
            'revenue_trends' => [],
            'top_performers' => [],
            'recent_activity' => []
        ];

        // Date range calculation
        $date_condition = '';
        $start_date = null;
        if ($period === 'week') {
            $start_date = date('Y-m-d H:i:s', strtotime('-7 days'));
            $date_condition = "AND submitted_at >= '$start_date'";
        } elseif ($period === 'month') {
            $start_date = date('Y-m-d H:i:s', strtotime('-1 month'));
            $date_condition = "AND submitted_at >= '$start_date'";
        } elseif ($period === 'year') {
            $start_date = date('Y-m-d H:i:s', strtotime('-1 year'));
            $date_condition = "AND submitted_at >= '$start_date'";
        }

        // --- Overview Stats ---
        $total_users = $db->query("SELECT COUNT(*) FROM users")->fetchColumn();
        $active_users = $db->query("SELECT COUNT(DISTINCT user_id) FROM test_results $date_condition")->fetchColumn();
        $total_tests = $db->query("SELECT COUNT(*) FROM test_results $date_condition")->fetchColumn();
        $total_questions = $db->query("SELECT COUNT(*) FROM questions")->fetchColumn();
        $avg_score = $db->query("SELECT AVG(percentage) FROM test_results $date_condition")->fetch(PDO::FETCH_COLUMN);
        $pass_rate = $db->query("SELECT COUNT(*) FROM test_results WHERE percentage >= 50 $date_condition")->fetchColumn();
        $pass_rate = $total_tests > 0 ? round(($pass_rate / $total_tests) * 100, 1) : 0;

        $response_data['overview'] = [
            'total_users' => (int)$total_users,
            'active_users' => (int)$active_users,
            'total_tests' => (int)$total_tests,
            'total_questions' => (int)$total_questions,
            'avg_score' => round($avg_score ?? 0, 1),
            'pass_rate' => $pass_rate
        ];

        // --- User Activity Trend (Last 7/30/365 days) ---
        $days = $period === 'week' ? 7 : ($period === 'month' ? 30 : ($period === 'year' ? 365 : 7));
        $user_activity = [];
        
        for ($i = $days - 1; $i >= 0; $i--) {
            $date = date('Y-m-d', strtotime("-$i days"));
            $next_date = date('Y-m-d', strtotime("-$i days +1 day"));
            
            $new_users = $db->query("SELECT COUNT(*) FROM users WHERE DATE(created_at) = '$date'")->fetchColumn();
            $tests_taken = $db->query("SELECT COUNT(*) FROM test_results WHERE DATE(submitted_at) = '$date'")->fetchColumn();
            
            $user_activity[] = [
                'date' => $date,
                'new_users' => (int)$new_users,
                'tests_taken' => (int)$tests_taken
            ];
        }
        $response_data['user_activity'] = $user_activity;

        // --- Test Performance Trends ---
        $performance_trend = [];
        for ($i = $days - 1; $i >= 0; $i--) {
            $date = date('Y-m-d', strtotime("-$i days"));
            
            $avg_score_day = $db->query("SELECT AVG(percentage) FROM test_results WHERE DATE(submitted_at) = '$date'")->fetch(PDO::FETCH_COLUMN);
            $total_tests_day = $db->query("SELECT COUNT(*) FROM test_results WHERE DATE(submitted_at) = '$date'")->fetchColumn();
            
            $performance_trend[] = [
                'date' => $date,
                'avg_score' => round($avg_score_day ?? 0, 1),
                'total_tests' => (int)$total_tests_day
            ];
        }
        $response_data['test_performance'] = $performance_trend;

        // --- Category Performance ---
        $category_query = "SELECT tc.id, tc.name, 
                                 COUNT(tr.id) as test_count,
                                 AVG(tr.percentage) as avg_score,
                                 COUNT(CASE WHEN tr.percentage >= 50 THEN 1 END) as passed_count
                          FROM test_categories tc
                          LEFT JOIN question_sessions qs ON tc.id = qs.test_category_id
                          LEFT JOIN test_results tr ON qs.id = tr.session_id $date_condition
                          WHERE tc.is_active = 1
                          GROUP BY tc.id, tc.name
                          ORDER BY test_count DESC
                          LIMIT 10";
        
        $category_stmt = $db->query($category_query);
        $categories = $category_stmt->fetchAll(PDO::FETCH_ASSOC);
        
        $response_data['category_performance'] = array_map(function($cat) {
            return [
                'category_id' => $cat['id'],
                'category_name' => $cat['name'],
                'test_count' => (int)$cat['test_count'],
                'avg_score' => round($cat['avg_score'] ?? 0, 1),
                'pass_rate' => $cat['test_count'] > 0 ? round(($cat['passed_count'] / $cat['test_count']) * 100, 1) : 0
            ];
        }, $categories);

        // --- Top Performers ---
        $top_performers_query = "SELECT u.id, u.name, u.mobile,
                                        COUNT(tr.id) as test_count,
                                        AVG(tr.percentage) as avg_score,
                                        MAX(tr.percentage) as best_score
                                 FROM users u
                                 LEFT JOIN test_results tr ON u.id = tr.user_id $date_condition
                                 WHERE tr.id IS NOT NULL
                                 GROUP BY u.id, u.name, u.mobile
                                 HAVING test_count > 0
                                 ORDER BY avg_score DESC, test_count DESC
                                 LIMIT 10";
        
        $top_performers_stmt = $db->query($top_performers_query);
        $top_performers = $top_performers_stmt->fetchAll(PDO::FETCH_ASSOC);
        
        $response_data['top_performers'] = array_map(function($user) {
            return [
                'user_id' => $user['id'],
                'user_name' => $user['name'],
                'user_mobile' => $user['mobile'],
                'test_count' => (int)$user['test_count'],
                'avg_score' => round($user['avg_score'] ?? 0, 1),
                'best_score' => round($user['best_score'] ?? 0, 1)
            ];
        }, $top_performers);

        // --- Recent Activity ---
        $recent_activity_query = "SELECT tr.*, u.name as user_name, qs.name as test_name, tc.name as category_name
                                  FROM test_results tr
                                  LEFT JOIN users u ON tr.user_id = u.id
                                  LEFT JOIN question_sessions qs ON tr.session_id = qs.id
                                  LEFT JOIN test_categories tc ON qs.test_category_id = tc.id
                                  ORDER BY tr.submitted_at DESC
                                  LIMIT 10";
        
        $recent_activity_stmt = $db->query($recent_activity_query);
        $recent_activity = $recent_activity_stmt->fetchAll(PDO::FETCH_ASSOC);
        
        $response_data['recent_activity'] = array_map(function($activity) {
            return [
                'result_id' => $activity['id'],
                'user_name' => $activity['user_name'],
                'test_name' => $activity['test_name'],
                'category_name' => $activity['category_name'],
                'score' => round($activity['percentage'], 1),
                'submitted_at' => $activity['submitted_at']
            ];
        }, $recent_activity);

        http_response_code(200);
        echo json_encode($response_data);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Error fetching analytics',
            'error' => $e->getMessage()
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed.']);
}
?>

