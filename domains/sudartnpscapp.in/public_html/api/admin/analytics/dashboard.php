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
            'recent_activity' => [],
            'referrals' => []
        ];

        // Date range calculation
        $date_condition = '';
        $date_condition_where = '';
        $start_date = null;
        if ($period === 'week') {
            $start_date = date('Y-m-d H:i:s', strtotime('-7 days'));
            $date_condition = "AND submitted_at >= '$start_date'";
            $date_condition_where = "WHERE submitted_at >= '$start_date'";
        } elseif ($period === 'month') {
            $start_date = date('Y-m-d H:i:s', strtotime('-1 month'));
            $date_condition = "AND submitted_at >= '$start_date'";
            $date_condition_where = "WHERE submitted_at >= '$start_date'";
        } elseif ($period === 'year') {
            $start_date = date('Y-m-d H:i:s', strtotime('-1 year'));
            $date_condition = "AND submitted_at >= '$start_date'";
            $date_condition_where = "WHERE submitted_at >= '$start_date'";
        }

        // --- Overview Stats ---
        $total_users = $db->query("SELECT COUNT(*) FROM users")->fetchColumn();
        $active_users = $db->query("SELECT COUNT(DISTINCT user_id) FROM test_results $date_condition_where")->fetchColumn();
        $total_tests = $db->query("SELECT COUNT(*) FROM test_results $date_condition_where")->fetchColumn();
        $total_questions = $db->query("SELECT COUNT(*) FROM questions")->fetchColumn();
        $avg_score = $db->query("SELECT AVG(percentage) FROM test_results $date_condition_where")->fetch(PDO::FETCH_COLUMN);
        $pass_rate = $db->query("SELECT COUNT(*) FROM test_results WHERE percentage >= 50 $date_condition")->fetchColumn();
        $pass_rate = $total_tests > 0 ? round(($pass_rate / $total_tests) * 100, 1) : 0;

        // --- Today's Registrations (IST) ---
        $today_registrations = $db->query("SELECT COUNT(*) FROM users WHERE DATE(CONVERT_TZ(created_at,'+00:00','+05:30')) = DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+05:30'))")->fetchColumn();

        // --- Yesterday's Registrations (IST) ---
        $yesterday_registrations = $db->query("SELECT COUNT(*) FROM users WHERE DATE(CONVERT_TZ(created_at,'+00:00','+05:30')) = DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+05:30') - INTERVAL 1 DAY)")->fetchColumn();

        // --- Subscription Stats (with safe fallback if table doesn't exist) ---
        $trial_started_today = 0;
        $active_trial_count = 0;
        $paid_active_299_count = 0;
        $monthly_revenue = 0;
        $total_payments = 0;
        $total_revenue = 0;
        $today_received_amount = 0;
        $today_received_count = 0;
        $yesterday_received_amount = 0;
        $yesterday_received_count = 0;
        
        try {
            // Check if subscriptions table exists
            $table_check = $db->query("SHOW TABLES LIKE 'subscriptions'");
            if ($table_check->rowCount() > 0) {
                // ₹5 trial authenticated today (IST)
                $trial_started_today = $db->query("SELECT COUNT(*) FROM subscriptions WHERE trial_start IS NOT NULL AND DATE(CONVERT_TZ(trial_start,'+00:00','+05:30')) = DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+05:30'))")->fetchColumn();
                
                // ALL active trial users (authenticated status = trial users)
                $active_trial_count = $db->query("SELECT COUNT(*) FROM subscriptions WHERE LOWER(status) = 'authenticated'")->fetchColumn();
                
                // Active ₹299 paid users (not in trial, subscription active)
                $paid_active_299_count = $db->query("SELECT COUNT(*) FROM subscriptions WHERE status = 'active' AND is_trial = 0")->fetchColumn();
                
                // Monthly Revenue (current month) - sum of last_payment_amount where payment was this month
                $monthly_revenue = $db->query("
                    SELECT COALESCE(SUM(last_payment_amount), 0) 
                    FROM subscriptions 
                    WHERE last_payment_amount > 0 
                    AND MONTH(CONVERT_TZ(last_payment_date,'+00:00','+05:30')) = MONTH(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+05:30'))
                    AND YEAR(CONVERT_TZ(last_payment_date,'+00:00','+05:30')) = YEAR(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+05:30'))
                ")->fetchColumn();
                
                // Total Payments count (sum of total_payments field)
                $total_payments = $db->query("SELECT COALESCE(SUM(total_payments), 0) FROM subscriptions WHERE total_payments > 0")->fetchColumn();
                
                // Total Revenue (all time) - sum of all last_payment_amount
                $total_revenue = $db->query("SELECT COALESCE(SUM(last_payment_amount), 0) FROM subscriptions WHERE last_payment_amount > 0")->fetchColumn();
                
                // Today's Received Amount (IST) - payments received today
                $today_received = $db->query("
                    SELECT 
                        COALESCE(SUM(last_payment_amount), 0) as amount,
                        COUNT(*) as count
                    FROM subscriptions 
                    WHERE last_payment_amount > 0 
                    AND DATE(CONVERT_TZ(last_payment_date,'+00:00','+05:30')) = DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+05:30'))
                ")->fetch(PDO::FETCH_ASSOC);
                $today_received_amount = $today_received['amount'] ?? 0;
                $today_received_count = $today_received['count'] ?? 0;
                
                // Yesterday's Received Amount (IST) - payments received yesterday
                $yesterday_received = $db->query("
                    SELECT 
                        COALESCE(SUM(last_payment_amount), 0) as amount,
                        COUNT(*) as count
                    FROM subscriptions 
                    WHERE last_payment_amount > 0 
                    AND DATE(CONVERT_TZ(last_payment_date,'+00:00','+05:30')) = DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+05:30') - INTERVAL 1 DAY)
                ")->fetch(PDO::FETCH_ASSOC);
                $yesterday_received_amount = $yesterday_received['amount'] ?? 0;
                $yesterday_received_count = $yesterday_received['count'] ?? 0;
            }
        } catch (PDOException $subEx) {
            // Subscriptions table may not exist - fail silently with 0 counts
            $trial_started_today = 0;
            $active_trial_count = 0;
            $paid_active_299_count = 0;
            $monthly_revenue = 0;
            $total_payments = 0;
            $total_revenue = 0;
            $today_received_amount = 0;
            $today_received_count = 0;
            $yesterday_received_amount = 0;
            $yesterday_received_count = 0;
        }

        $response_data['overview'] = [
            'total_users' => (int)$total_users,
            'active_users' => (int)$active_users,
            'total_tests' => (int)$total_tests,
            'total_questions' => (int)$total_questions,
            'avg_score' => round($avg_score ?? 0, 1),
            'pass_rate' => $pass_rate,
            'today_registrations' => (int)$today_registrations,
            'yesterday_registrations' => (int)$yesterday_registrations,
            'trial_started_today' => (int)$trial_started_today,
            'active_trial_count' => (int)$active_trial_count,
            'paid_active_299_count' => (int)$paid_active_299_count,
            'monthly_revenue' => (float)$monthly_revenue,
            'total_payments' => (int)$total_payments,
            'total_revenue' => (float)$total_revenue,
            'today_received_amount' => (float)$today_received_amount,
            'today_received_count' => (int)$today_received_count,
            'yesterday_received_amount' => (float)$yesterday_received_amount,
            'yesterday_received_count' => (int)$yesterday_received_count
        ];

        // Referral system removed (will be redesigned later)
        $response_data['referrals'] = [];

        // --- User Activity Trend (Last 7/30/365 days in IST) ---
        $days = $period === 'week' ? 7 : ($period === 'month' ? 30 : ($period === 'year' ? 365 : 7));
        $user_activity = [];
        
        // Get IST today date
        $istToday = $db->query("SELECT DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+05:30'))")->fetchColumn();
        
        for ($i = $days - 1; $i >= 0; $i--) {
            // Calculate IST date for this day
            $istDate = $db->query("SELECT DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+05:30') - INTERVAL $i DAY)")->fetchColumn();
            
            $new_users = $db->query("SELECT COUNT(*) FROM users WHERE DATE(CONVERT_TZ(created_at,'+00:00','+05:30')) = '$istDate'")->fetchColumn();
            $tests_taken = $db->query("SELECT COUNT(*) FROM test_results WHERE DATE(CONVERT_TZ(submitted_at,'+00:00','+05:30')) = '$istDate'")->fetchColumn();
            
            $user_activity[] = [
                'date' => $istDate,
                'new_users' => (int)$new_users,
                'tests_taken' => (int)$tests_taken
            ];
        }
        $response_data['user_activity'] = $user_activity;

        // --- Test Performance Trends (IST) ---
        $performance_trend = [];
        for ($i = $days - 1; $i >= 0; $i--) {
            // Calculate IST date for this day
            $istDate = $db->query("SELECT DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+05:30') - INTERVAL $i DAY)")->fetchColumn();
            
            $avg_score_day = $db->query("SELECT AVG(percentage) FROM test_results WHERE DATE(CONVERT_TZ(submitted_at,'+00:00','+05:30')) = '$istDate'")->fetch(PDO::FETCH_COLUMN);
            $total_tests_day = $db->query("SELECT COUNT(*) FROM test_results WHERE DATE(CONVERT_TZ(submitted_at,'+00:00','+05:30')) = '$istDate'")->fetchColumn();
            
            $performance_trend[] = [
                'date' => $istDate,
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

