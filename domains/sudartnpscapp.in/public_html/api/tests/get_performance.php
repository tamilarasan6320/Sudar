<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/TestResult.php';
require_once '../models/TestCategory.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : null;
    $period = isset($_GET['period']) ? $_GET['period'] : 'all'; // week, month, year, all
    
    if (empty($user_id)) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'User ID is required'
        ]);
        exit;
    }
    
    try {
        // Calculate date range based on period
        $dateFilter = '';
        if ($period === 'week') {
            $dateFilter = " AND tr.submitted_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
        } elseif ($period === 'month') {
            $dateFilter = " AND tr.submitted_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        } elseif ($period === 'year') {
            $dateFilter = " AND tr.submitted_at >= DATE_SUB(NOW(), INTERVAL 365 DAY)";
        }
        
        // Get overall performance stats
        $overallQuery = "SELECT 
                            COUNT(*) as total_tests,
                            AVG(tr.percentage) as avg_score,
                            MAX(tr.percentage) as best_score,
                            MIN(tr.percentage) as worst_score,
                            SUM(CASE WHEN tr.percentage >= 50 THEN 1 ELSE 0 END) as passed,
                            SUM(CASE WHEN tr.percentage < 50 THEN 1 ELSE 0 END) as failed,
                            AVG(tr.time_taken) as avg_time_taken
                        FROM test_results tr
                        WHERE tr.user_id = :user_id $dateFilter";
        
        $stmt = $db->prepare($overallQuery);
        $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
        $stmt->execute();
        $overallStats = $stmt->fetch(PDO::FETCH_ASSOC);
        
        // Get subject-wise performance
        $subjectQuery = "SELECT 
                            tc.id,
                            tc.name as category_name,
                            COUNT(tr.id) as test_count,
                            AVG(tr.percentage) as avg_score,
                            MAX(tr.percentage) as best_score,
                            SUM(CASE WHEN tr.percentage >= 50 THEN 1 ELSE 0 END) as passed,
                            SUM(CASE WHEN tr.percentage < 50 THEN 1 ELSE 0 END) as failed
                        FROM test_results tr
                        INNER JOIN question_sessions qs ON tr.session_id = qs.id
                        INNER JOIN test_categories tc ON qs.test_category_id = tc.id
                        WHERE tr.user_id = :user_id $dateFilter
                        GROUP BY tc.id, tc.name
                        ORDER BY avg_score DESC";
        
        $stmt = $db->prepare($subjectQuery);
        $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
        $stmt->execute();
        $subjectPerformance = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $subjectPerformance[] = [
                'category_id' => intval($row['id']),
                'category_name' => $row['category_name'],
                'test_count' => intval($row['test_count']),
                'avg_score' => round(floatval($row['avg_score']), 2),
                'best_score' => round(floatval($row['best_score']), 2),
                'passed' => intval($row['passed']),
                'failed' => intval($row['failed']),
                'progress' => $row['test_count'] > 0 ? round(($row['passed'] / $row['test_count']) * 100, 2) : 0
            ];
        }
        
        // Get performance trends (last 10 tests)
        $trendsQuery = "SELECT 
                            tr.id,
                            tr.percentage,
                            tr.submitted_at,
                            qs.name as test_name,
                            tc.name as category_name
                        FROM test_results tr
                        INNER JOIN question_sessions qs ON tr.session_id = qs.id
                        LEFT JOIN test_categories tc ON qs.test_category_id = tc.id
                        WHERE tr.user_id = :user_id $dateFilter
                        ORDER BY tr.submitted_at DESC
                        LIMIT 10";
        
        $stmt = $db->prepare($trendsQuery);
        $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
        $stmt->execute();
        $trends = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $trends[] = [
                'test_id' => intval($row['id']),
                'test_name' => $row['test_name'],
                'category_name' => $row['category_name'],
                'score' => round(floatval($row['percentage']), 2),
                'date' => $row['submitted_at']
            ];
        }
        
        // Calculate strengths and weaknesses
        $strengths = [];
        $weaknesses = [];
        
        foreach ($subjectPerformance as $subject) {
            if ($subject['avg_score'] >= 70) {
                $strengths[] = [
                    'category' => $subject['category_name'],
                    'score' => $subject['avg_score'],
                    'tests' => $subject['test_count']
                ];
            } elseif ($subject['avg_score'] < 50 && $subject['test_count'] >= 2) {
                $weaknesses[] = [
                    'category' => $subject['category_name'],
                    'score' => $subject['avg_score'],
                    'tests' => $subject['test_count'],
                    'improvement_needed' => round(50 - $subject['avg_score'], 2)
                ];
            }
        }
        
        // Sort strengths and weaknesses
        usort($strengths, function($a, $b) { return $b['score'] <=> $a['score']; });
        usort($weaknesses, function($a, $b) { return $a['score'] <=> $b['score']; });
        
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'period' => $period,
            'overall' => [
                'total_tests' => intval($overallStats['total_tests'] ?? 0),
                'avg_score' => round(floatval($overallStats['avg_score'] ?? 0), 2),
                'best_score' => round(floatval($overallStats['best_score'] ?? 0), 2),
                'worst_score' => round(floatval($overallStats['worst_score'] ?? 0), 2),
                'passed' => intval($overallStats['passed'] ?? 0),
                'failed' => intval($overallStats['failed'] ?? 0),
                'pass_rate' => $overallStats['total_tests'] > 0 
                    ? round(($overallStats['passed'] / $overallStats['total_tests']) * 100, 2) 
                    : 0,
                'avg_time_taken' => round(floatval($overallStats['avg_time_taken'] ?? 0), 2)
            ],
            'subjects' => $subjectPerformance,
            'trends' => array_reverse($trends), // Reverse to show oldest first
            'strengths' => array_slice($strengths, 0, 5), // Top 5
            'weaknesses' => array_slice($weaknesses, 0, 5) // Top 5
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

