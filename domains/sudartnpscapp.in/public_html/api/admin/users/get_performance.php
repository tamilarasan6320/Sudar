<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication
require_once '../../models/TestResult.php';
require_once '../../models/User.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : null;

    if (!$user_id) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'User ID is required.']);
        exit();
    }

    try {
        // Get user details
        $user = new User($db);
        $user->id = $user_id;
        $user_stmt = $user->readOne();
        $user_data = $user_stmt->fetch(PDO::FETCH_ASSOC);

        if (!$user_data) {
            http_response_code(404);
            echo json_encode(['success' => false, 'message' => 'User not found.']);
            exit();
        }

        // Get test results for this user
        $testResult = new TestResult($db);
        $testResult->user_id = $user_id;
        $history_stmt = $testResult->getUserHistory(1000);
        $history = $history_stmt->fetchAll(PDO::FETCH_ASSOC);

        // Calculate statistics
        $total_tests = count($history);
        $total_score = 0;
        $passed_tests = 0;
        $best_score = 0;
        $worst_score = 100;
        $total_time = 0;
        $category_stats = [];

        foreach ($history as $res) {
            $percentage = $res['percentage'] ?? 0;
            $total_score += $percentage;
            $total_time += $res['time_taken'] ?? 0;
            
            if ($percentage >= 50) {
                $passed_tests++;
            }
            if ($percentage > $best_score) {
                $best_score = $percentage;
            }
            if ($percentage < $worst_score) {
                $worst_score = $percentage;
            }

            // Category-wise stats
            $cat_id = $res['test_category_id'];
            $cat_name = $res['category_name'] ?? 'Unknown';
            
            if (!isset($category_stats[$cat_id])) {
                $category_stats[$cat_id] = [
                    'name' => $cat_name,
                    'total_tests' => 0,
                    'total_score' => 0,
                    'passed' => 0,
                    'best_score' => 0,
                    'worst_score' => 100
                ];
            }
            
            $category_stats[$cat_id]['total_tests']++;
            $category_stats[$cat_id]['total_score'] += $percentage;
            if ($percentage >= 50) {
                $category_stats[$cat_id]['passed']++;
            }
            if ($percentage > $category_stats[$cat_id]['best_score']) {
                $category_stats[$cat_id]['best_score'] = $percentage;
            }
            if ($percentage < $category_stats[$cat_id]['worst_score']) {
                $category_stats[$cat_id]['worst_score'] = $percentage;
            }
        }

        $avg_score = $total_tests > 0 ? round($total_score / $total_tests, 1) : 0;
        $pass_rate = $total_tests > 0 ? round(($passed_tests / $total_tests) * 100, 1) : 0;
        $avg_time = $total_tests > 0 ? round($total_time / $total_tests / 60, 1) : 0; // in minutes

        // Format category stats
        $category_performance = [];
        foreach ($category_stats as $cat_id => $stats) {
            $category_performance[] = [
                'category_id' => $cat_id,
                'category_name' => $stats['name'],
                'total_tests' => $stats['total_tests'],
                'avg_score' => round($stats['total_score'] / $stats['total_tests'], 1),
                'pass_rate' => round(($stats['passed'] / $stats['total_tests']) * 100, 1),
                'best_score' => round($stats['best_score'], 1),
                'worst_score' => round($stats['worst_score'], 1)
            ];
        }

        // Get rank
        $rank_stmt = $testResult->getRankings();
        $rankings = $rank_stmt->fetchAll(PDO::FETCH_ASSOC);
        $user_rank = 0;
        foreach ($rankings as $index => $rank_entry) {
            if ($rank_entry['user_id'] == $user_id) {
                $user_rank = $index + 1;
                break;
            }
        }

        // Recent activity (last 10 tests)
        $recent_tests = array_slice($history, 0, 10);

        $response_data = [
            'success' => true,
            'user' => [
                'id' => $user_data['id'],
                'name' => $user_data['name'],
                'mobile' => $user_data['mobile'],
                'email' => $user_data['email'] ?? '',
                'created_at' => $user_data['created_at'] ?? ''
            ],
            'overall_stats' => [
                'total_tests' => $total_tests,
                'avg_score' => $avg_score,
                'pass_rate' => $pass_rate,
                'best_score' => round($best_score, 1),
                'worst_score' => round($worst_score, 1),
                'passed_tests' => $passed_tests,
                'failed_tests' => $total_tests - $passed_tests,
                'avg_time_per_test' => $avg_time,
                'total_time_spent' => round($total_time / 60, 1), // in minutes
                'rank' => $user_rank
            ],
            'category_performance' => $category_performance,
            'recent_tests' => array_map(function($test) {
                return [
                    'id' => $test['id'],
                    'test_name' => $test['category_name'] ?? 'Unknown',
                    'session_name' => $test['session_name'] ?? 'Unknown',
                    'score' => round($test['percentage'] ?? 0, 1),
                    'correct' => $test['correct_answers'] ?? 0,
                    'total' => $test['total_questions'] ?? 0,
                    'time_taken' => round(($test['time_taken'] ?? 0) / 60, 1), // in minutes
                    'submitted_at' => $test['submitted_at']
                ];
            }, $recent_tests)
        ];

        http_response_code(200);
        echo json_encode($response_data);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
    }

} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed.']);
}
?>

