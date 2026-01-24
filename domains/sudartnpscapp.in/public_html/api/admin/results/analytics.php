<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    try {
        // Overall statistics
        $overallQuery = "SELECT 
                            COUNT(*) as total_results,
                            COUNT(DISTINCT user_id) as unique_users,
                            COUNT(DISTINCT session_id) as unique_tests,
                            AVG(percentage) as avg_score,
                            MAX(percentage) as max_score,
                            MIN(percentage) as min_score,
                            SUM(CASE WHEN percentage >= 50 THEN 1 ELSE 0 END) as passed,
                            SUM(CASE WHEN percentage < 50 THEN 1 ELSE 0 END) as failed,
                            AVG(time_taken) as avg_time_taken
                        FROM test_results";
        
        $stmt = $db->query($overallQuery);
        $overall = $stmt->fetch(PDO::FETCH_ASSOC);
        
        // Category-wise performance
        $categoryQuery = "SELECT 
                            tc.id,
                            tc.name as category_name,
                            COUNT(tr.id) as test_count,
                            AVG(tr.percentage) as avg_score,
                            MAX(tr.percentage) as max_score,
                            MIN(tr.percentage) as min_score,
                            SUM(CASE WHEN tr.percentage >= 50 THEN 1 ELSE 0 END) as passed,
                            SUM(CASE WHEN tr.percentage < 50 THEN 1 ELSE 0 END) as failed
                        FROM test_results tr
                        INNER JOIN question_sessions qs ON tr.session_id = qs.id
                        INNER JOIN test_categories tc ON qs.test_category_id = tc.id
                        GROUP BY tc.id, tc.name
                        ORDER BY test_count DESC";
        
        $stmt = $db->query($categoryQuery);
        $categories = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $categories[] = [
                'category_id' => intval($row['id']),
                'category_name' => $row['category_name'],
                'test_count' => intval($row['test_count']),
                'avg_score' => round(floatval($row['avg_score']), 2),
                'max_score' => round(floatval($row['max_score']), 2),
                'min_score' => round(floatval($row['min_score']), 2),
                'passed' => intval($row['passed']),
                'failed' => intval($row['failed']),
                'pass_rate' => $row['test_count'] > 0 
                    ? round(($row['passed'] / $row['test_count']) * 100, 2) 
                    : 0
            ];
        }
        
        // Recent activity (last 7 days)
        $recentQuery = "SELECT 
                            DATE(tr.submitted_at) as date,
                            COUNT(*) as count,
                            AVG(tr.percentage) as avg_score
                        FROM test_results tr
                        WHERE tr.submitted_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                        GROUP BY DATE(tr.submitted_at)
                        ORDER BY date DESC";
        
        $stmt = $db->query($recentQuery);
        $recentActivity = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $recentActivity[] = [
                'date' => $row['date'],
                'count' => intval($row['count']),
                'avg_score' => round(floatval($row['avg_score']), 2)
            ];
        }
        
        // Top performers
        $topPerformersQuery = "SELECT 
                                    u.id,
                                    u.name,
                                    u.mobile,
                                    COUNT(tr.id) as test_count,
                                    AVG(tr.percentage) as avg_score,
                                    MAX(tr.percentage) as best_score
                                FROM test_results tr
                                INNER JOIN users u ON tr.user_id = u.id
                                GROUP BY u.id, u.name, u.mobile
                                HAVING test_count >= 3
                                ORDER BY avg_score DESC
                                LIMIT 10";
        
        $stmt = $db->query($topPerformersQuery);
        $topPerformers = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $topPerformers[] = [
                'user_id' => intval($row['id']),
                'user_name' => $row['name'],
                'user_mobile' => $row['mobile'],
                'test_count' => intval($row['test_count']),
                'avg_score' => round(floatval($row['avg_score']), 2),
                'best_score' => round(floatval($row['best_score']), 2)
            ];
        }
        
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'overall' => [
                'total_results' => intval($overall['total_results']),
                'unique_users' => intval($overall['unique_users']),
                'unique_tests' => intval($overall['unique_tests']),
                'avg_score' => round(floatval($overall['avg_score']), 2),
                'max_score' => round(floatval($overall['max_score']), 2),
                'min_score' => round(floatval($overall['min_score']), 2),
                'passed' => intval($overall['passed']),
                'failed' => intval($overall['failed']),
                'pass_rate' => $overall['total_results'] > 0 
                    ? round(($overall['passed'] / $overall['total_results']) * 100, 2) 
                    : 0,
                'avg_time_taken' => round(floatval($overall['avg_time_taken']), 2)
            ],
            'categories' => $categories,
            'recent_activity' => $recentActivity,
            'top_performers' => $topPerformers
        ]);
        
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Database error: ' . $e->getMessage()
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

