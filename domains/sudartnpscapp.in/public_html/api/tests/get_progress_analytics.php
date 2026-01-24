<?php
// Disable error display to prevent HTML in JSON response
ini_set('display_errors', 0);
error_reporting(E_ALL);

require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/TestResult.php';
require_once '../models/TestCategory.php';

try {
    $database = new Database();
    $db = $database->getConnection();

    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : null;

        if (!$user_id) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'User ID is required.']);
            exit();
        }

        $testResult = new TestResult($db);
        $testResult->user_id = $user_id;

    $response_data = [
        'success' => true,
        'overall_stats' => [],
        'rank' => 0,
        'streak' => 0,
        'performance_trend' => [],
        'strengths' => [],
        'weaknesses' => []
    ];

    // --- Overall Stats ---
    $history_stmt = $testResult->getUserHistory(1000);
    $history = $history_stmt->fetchAll(PDO::FETCH_ASSOC);

    $total_tests = count($history);
    $total_score = 0;
    $passed_tests = 0;
    $best_score = 0;
    $worst_score = 100;

    foreach ($history as $res) {
        $percentage = $res['percentage'] ?? 0;
        $total_score += $percentage;
        if ($percentage >= 50) {
            $passed_tests++;
        }
        if ($percentage > $best_score) {
            $best_score = $percentage;
        }
        if ($percentage < $worst_score) {
            $worst_score = $percentage;
        }
    }

    $avg_score = $total_tests > 0 ? round($total_score / $total_tests, 1) : 0;
    $pass_rate = $total_tests > 0 ? round(($passed_tests / $total_tests) * 100, 1) : 0;

    $response_data['overall_stats'] = [
        'total_tests' => $total_tests,
        'avg_score' => $avg_score,
        'pass_rate' => $pass_rate,
        'best_score' => round($best_score, 1),
        'worst_score' => round($worst_score, 1),
        'passed_tests' => $passed_tests,
        'failed_tests' => $total_tests - $passed_tests
    ];

    // --- Rank Calculation ---
    $rank_stmt = $testResult->getRankings();
    $rankings = $rank_stmt->fetchAll(PDO::FETCH_ASSOC);
    $user_rank = 0;
    foreach ($rankings as $index => $rank_entry) {
        if ($rank_entry['user_id'] == $user_id) {
            $user_rank = $index + 1;
            break;
        }
    }
    $response_data['rank'] = $user_rank;

    // --- Streak Calculation (consecutive days with at least one test) ---
    $streak = 0;
    if (!empty($history)) {
        // Get unique dates from test history (one test per day counts)
        $test_dates_set = [];
        foreach ($history as $res) {
            $test_date = date('Y-m-d', strtotime($res['submitted_at']));
            $test_dates_set[$test_date] = true;
        }
        
        if (!empty($test_dates_set)) {
            $current_date = date('Y-m-d');
            $test_dates = array_keys($test_dates_set);
            rsort($test_dates); // Sort descending (most recent first)
            
            $most_recent_date = $test_dates[0];
            
            // Check if most recent test was today or yesterday
            // If it's more than 1 day old, streak is broken (0)
            $days_diff = (strtotime($current_date) - strtotime($most_recent_date)) / (60 * 60 * 24);
            
            if ($days_diff <= 1) {
                // Start counting from most recent date
                $streak = 1;
                $check_date = $most_recent_date;
                
                // Count backwards day by day for consecutive days
                while (true) {
                    $previous_day = date('Y-m-d', strtotime('-1 day', strtotime($check_date)));
                    
                    // Check if previous day has a test
                    if (isset($test_dates_set[$previous_day])) {
                        $streak++;
                        $check_date = $previous_day;
                    } else {
                        // Gap found - streak broken
                        break;
                    }
                }
            }
        }
    }
    $response_data['streak'] = $streak;

    // --- Performance Trend (last 10 tests) ---
    $recent_tests = array_slice($history, 0, 10);
    $response_data['performance_trend'] = array_map(function($test) {
        return [
            'date' => date('Y-m-d', strtotime($test['submitted_at'])),
            'score' => round($test['percentage'] ?? 0, 1),
            'test_name' => $test['category_name'] ?? 'Unknown'
        ];
    }, $recent_tests);

    // --- Strengths & Weaknesses (Subject-wise) ---
    $subject_scores = [];
    foreach ($history as $res) {
        $test_category_id = $res['test_category_id'];
        $category_name = $res['category_name'] ?? 'Unknown';
        
        if (!isset($subject_scores[$test_category_id])) {
            $subject_scores[$test_category_id] = [
                'name' => $category_name,
                'total_percentage' => 0,
                'count' => 0
            ];
        }
        $subject_scores[$test_category_id]['total_percentage'] += $res['percentage'];
        $subject_scores[$test_category_id]['count']++;
    }

    $subject_avg = [];
    foreach ($subject_scores as $id => $data) {
        $avg = $data['count'] > 0 ? round($data['total_percentage'] / $data['count'], 1) : 0;
        $subject_avg[] = [
            'id' => $id,
            'name' => $data['name'],
            'avg_score' => $avg,
            'test_count' => $data['count']
        ];
    }

    // Sort by avg_score descending
    usort($subject_avg, function($a, $b) {
        return $b['avg_score'] <=> $a['avg_score'];
    });

    // Strengths: Top 3 subjects with score >= 70%
    $response_data['strengths'] = array_values(array_filter($subject_avg, function($s) {
        return $s['avg_score'] >= 70;
    }));
    $response_data['strengths'] = array_slice($response_data['strengths'], 0, 3);

    // Weaknesses: Bottom 3 subjects with score < 50%
    $weaknesses = array_filter($subject_avg, function($s) {
        return $s['avg_score'] < 50;
    });
    usort($weaknesses, function($a, $b) {
        return $a['avg_score'] <=> $b['avg_score'];
    });
    $response_data['weaknesses'] = array_slice(array_values($weaknesses), 0, 3);

        http_response_code(200);
        echo json_encode($response_data);

    } else {
        http_response_code(405);
        echo json_encode(['success' => false, 'message' => 'Method not allowed.']);
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

