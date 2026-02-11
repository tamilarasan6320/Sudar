<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $type = isset($_GET['type']) ? $_GET['type'] : 'test_results'; // test_results, users, analytics
    $format = isset($_GET['format']) ? $_GET['format'] : 'csv'; // csv, json, excel
    $start_date = isset($_GET['start_date']) ? $_GET['start_date'] : null;
    $end_date = isset($_GET['end_date']) ? $_GET['end_date'] : null;
    $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : null;
    $category_id = isset($_GET['category_id']) ? intval($_GET['category_id']) : null;

    try {
        switch ($type) {
            case 'test_results':
                $where_conditions = ["1=1"];
                $params = [];

                if ($start_date) {
                    $where_conditions[] = "DATE(tr.submitted_at) >= ?";
                    $params[] = $start_date;
                }
                if ($end_date) {
                    $where_conditions[] = "DATE(tr.submitted_at) <= ?";
                    $params[] = $end_date;
                }
                if ($user_id) {
                    $where_conditions[] = "tr.user_id = ?";
                    $params[] = $user_id;
                }
                if ($category_id) {
                    $where_conditions[] = "qs.test_category_id = ?";
                    $params[] = $category_id;
                }

                $where_clause = implode(" AND ", $where_conditions);

                $query = "SELECT tr.id,
                                 u.name as user_name,
                                 u.mobile as user_mobile,
                                 ec.name as exam_name,
                                 tc.name as category_name,
                                 qs.name as session_name,
                                 tr.total_questions,
                                 tr.correct_answers,
                                 tr.wrong_answers,
                                 tr.percentage,
                                 tr.time_taken,
                                 tr.submitted_at
                          FROM test_results tr
                          LEFT JOIN users u ON tr.user_id = u.id
                          LEFT JOIN question_sessions qs ON tr.session_id = qs.id
                          LEFT JOIN test_categories tc ON qs.test_category_id = tc.id
                          LEFT JOIN exam_categories ec ON tc.exam_category_id = ec.id
                          WHERE $where_clause
                          ORDER BY tr.submitted_at DESC";
                
                $stmt = $db->prepare($query);
                $stmt->execute($params);
                $data = $stmt->fetchAll(PDO::FETCH_ASSOC);

                if ($format === 'csv') {
                    header('Content-Type: text/csv');
                    header('Content-Disposition: attachment; filename="test_results_' . date('Y-m-d') . '.csv"');
                    
                    $output = fopen('php://output', 'w');
                    fputcsv($output, ['ID', 'User Name', 'Mobile', 'Exam', 'Category', 'Session', 'Total Questions', 'Correct', 'Wrong', 'Percentage', 'Time Taken (sec)', 'Submitted At']);
                    
                    foreach ($data as $row) {
                        fputcsv($output, [
                            $row['id'],
                            $row['user_name'],
                            $row['user_mobile'],
                            $row['exam_name'],
                            $row['category_name'],
                            $row['session_name'],
                            $row['total_questions'],
                            $row['correct_answers'],
                            $row['wrong_answers'],
                            $row['percentage'],
                            $row['time_taken'],
                            $row['submitted_at']
                        ]);
                    }
                    fclose($output);
                    exit;
                } else {
                    http_response_code(200);
                    echo json_encode([
                        'success' => true,
                        'type' => $type,
                        'format' => $format,
                        'count' => count($data),
                        'data' => $data
                    ]);
                }
                break;

            case 'users':
                $query = "SELECT u.id,
                                 u.name,
                                 u.mobile,
                                 u.email,
                                 u.created_at,
                                 u.last_login,
                                 COUNT(tr.id) as total_tests,
                                 AVG(tr.percentage) as avg_score
                          FROM users u
                          LEFT JOIN test_results tr ON u.id = tr.user_id
                          GROUP BY u.id, u.name, u.mobile, u.email, u.created_at, u.last_login
                          ORDER BY u.created_at DESC";
                
                $stmt = $db->query($query);
                $data = $stmt->fetchAll(PDO::FETCH_ASSOC);

                if ($format === 'csv') {
                    header('Content-Type: text/csv');
                    header('Content-Disposition: attachment; filename="users_' . date('Y-m-d') . '.csv"');
                    
                    $output = fopen('php://output', 'w');
                    fputcsv($output, ['ID', 'Name', 'Mobile', 'Email', 'Total Tests', 'Avg Score', 'Created At', 'Last Login']);
                    
                    foreach ($data as $row) {
                        fputcsv($output, [
                            $row['id'],
                            $row['name'],
                            $row['mobile'],
                            $row['email'] ?? '',
                            $row['total_tests'],
                            round($row['avg_score'] ?? 0, 1),
                            $row['created_at'],
                            $row['last_login'] ?? ''
                        ]);
                    }
                    fclose($output);
                    exit;
                } else {
                    http_response_code(200);
                    echo json_encode([
                        'success' => true,
                        'type' => $type,
                        'format' => $format,
                        'count' => count($data),
                        'data' => $data
                    ]);
                }
                break;

            case 'daily_user_metrics':
                // Export daily user metrics for Google Sheets
                // Columns: Date, Registrations, Trial Users, Active Users, Total Users

                // Defaults (last 30 days)
                if (!$start_date) {
                    $start_date = date('Y-m-d', strtotime('-30 days'));
                }
                if (!$end_date) {
                    $end_date = date('Y-m-d');
                }

                // Generate date range
                $dates = [];
                $current = new DateTime($start_date);
                $end = new DateTime($end_date);
                while ($current <= $end) {
                    $dates[] = $current->format('Y-m-d');
                    $current->modify('+1 day');
                }

                // Get daily registrations
                $regStmt = $db->prepare("
                    SELECT DATE(created_at) as day, COUNT(*) as count 
                    FROM users 
                    WHERE DATE(created_at) BETWEEN ? AND ?
                    GROUP BY DATE(created_at)
                ");
                $regStmt->execute([$start_date, $end_date]);
                $registrations = [];
                while ($row = $regStmt->fetch(PDO::FETCH_ASSOC)) {
                    $registrations[$row['day']] = (int)$row['count'];
                }

                // Get daily trial starts (check if subscriptions table exists)
                $trialUsers = [];
                $tableCheck = $db->query("SHOW TABLES LIKE 'subscriptions'");
                if ($tableCheck->rowCount() > 0) {
                    $trialStmt = $db->prepare("
                        SELECT DATE(trial_start) as day, COUNT(DISTINCT user_id) as count 
                        FROM subscriptions 
                        WHERE trial_start IS NOT NULL 
                        AND DATE(trial_start) BETWEEN ? AND ?
                        GROUP BY DATE(trial_start)
                    ");
                    $trialStmt->execute([$start_date, $end_date]);
                    while ($row = $trialStmt->fetch(PDO::FETCH_ASSOC)) {
                        $trialUsers[$row['day']] = (int)$row['count'];
                    }
                }

                // Get daily active users (login or test submission)
                $activeStmt = $db->prepare("
                    SELECT day, COUNT(DISTINCT user_id) as count FROM (
                        SELECT DATE(last_login) as day, id as user_id FROM users 
                        WHERE last_login IS NOT NULL AND DATE(last_login) BETWEEN ? AND ?
                        UNION ALL
                        SELECT DATE(submitted_at) as day, user_id FROM test_results 
                        WHERE DATE(submitted_at) BETWEEN ? AND ?
                    ) combined
                    GROUP BY day
                ");
                $activeStmt->execute([$start_date, $end_date, $start_date, $end_date]);
                $activeUsers = [];
                while ($row = $activeStmt->fetch(PDO::FETCH_ASSOC)) {
                    $activeUsers[$row['day']] = (int)$row['count'];
                }

                // Get cumulative total users per day
                $cumulativeUsers = [];
                foreach ($dates as $date) {
                    $totalStmt = $db->prepare("SELECT COUNT(*) FROM users WHERE DATE(created_at) <= ?");
                    $totalStmt->execute([$date]);
                    $cumulativeUsers[$date] = (int)$totalStmt->fetchColumn();
                }

                if ($format === 'csv') {
                    header('Content-Type: text/csv');
                    header('Content-Disposition: attachment; filename="daily_user_metrics_' . $start_date . '_to_' . $end_date . '.csv"');

                    $output = fopen('php://output', 'w');
                    fputcsv($output, ['Date', 'Registrations', '₹5 Trial Starts', 'Active Users', 'Total Users']);

                    foreach ($dates as $date) {
                        fputcsv($output, [
                            $date,
                            $registrations[$date] ?? 0,
                            $trialUsers[$date] ?? 0,
                            $activeUsers[$date] ?? 0,
                            $cumulativeUsers[$date] ?? 0
                        ]);
                    }
                    fclose($output);
                    exit;
                }

                // JSON output
                $data = [];
                foreach ($dates as $date) {
                    $data[] = [
                        'date' => $date,
                        'registrations' => $registrations[$date] ?? 0,
                        'trial_users' => $trialUsers[$date] ?? 0,
                        'active_users' => $activeUsers[$date] ?? 0,
                        'total_users' => $cumulativeUsers[$date] ?? 0
                    ];
                }

                http_response_code(200);
                echo json_encode([
                    'success' => true,
                    'type' => $type,
                    'start_date' => $start_date,
                    'end_date' => $end_date,
                    'count' => count($data),
                    'data' => $data
                ]);
                break;

            case 'combined_daily_report':
                // Combined Daily Report: Single date per row with Installs & Purchases separated

                if (!$start_date) {
                    $start_date = date('Y-m-d', strtotime('-30 days'));
                }
                if (!$end_date) {
                    $end_date = date('Y-m-d');
                }

                // Generate date range
                $dates = [];
                $current = new DateTime($start_date);
                $end = new DateTime($end_date);
                while ($current <= $end) {
                    $dates[] = $current->format('Y-m-d');
                    $current->modify('+1 day');
                }

                // Get daily registrations
                $regStmt = $db->prepare("
                    SELECT DATE(created_at) as day, COUNT(*) as count 
                    FROM users 
                    WHERE DATE(created_at) BETWEEN ? AND ?
                    GROUP BY DATE(created_at)
                ");
                $regStmt->execute([$start_date, $end_date]);
                $registrations = [];
                while ($row = $regStmt->fetch(PDO::FETCH_ASSOC)) {
                    $registrations[$row['day']] = (int)$row['count'];
                }

                // Get daily trial starts (₹5 users who started trial on that date - regardless of current status)
                $trialUsers = [];
                $tableCheck = $db->query("SHOW TABLES LIKE 'subscriptions'");
                if ($tableCheck->rowCount() > 0) {
                    $trialStmt = $db->prepare("
                        SELECT DATE(trial_start) as day, COUNT(DISTINCT user_id) as count 
                        FROM subscriptions 
                        WHERE trial_start IS NOT NULL 
                        AND DATE(trial_start) BETWEEN ? AND ?
                        GROUP BY DATE(trial_start)
                    ");
                    $trialStmt->execute([$start_date, $end_date]);
                    while ($row = $trialStmt->fetch(PDO::FETCH_ASSOC)) {
                        $trialUsers[$row['day']] = (int)$row['count'];
                    }
                }

                // Get Meta Ads data grouped by date with installs and purchases separated
                $dailyAds = [];
                $metaTableCheck = $db->query("SHOW TABLES LIKE 'meta_ads_insights_daily'");
                if ($metaTableCheck->rowCount() > 0) {
                    $metaStmt = $db->prepare("
                        SELECT day, spend, results, cost_per_result, result_action_type, clicks, impressions
                        FROM meta_ads_insights_daily 
                        WHERE day BETWEEN ? AND ? AND level = 'campaign'
                    ");
                    $metaStmt->execute([$start_date, $end_date]);
                    
                    while ($row = $metaStmt->fetch(PDO::FETCH_ASSOC)) {
                        $day = $row['day'];
                        if (!isset($dailyAds[$day])) {
                            $dailyAds[$day] = [
                                'spend' => 0,
                                'installs' => 0,
                                'install_cost' => null,
                                'install_spend' => 0,
                                'purchases' => 0,
                                'purchase_cost' => null,
                                'purchase_spend' => 0,
                                'clicks' => 0,
                                'impressions' => 0
                            ];
                        }
                        
                        $dailyAds[$day]['spend'] += (float)$row['spend'];
                        $dailyAds[$day]['clicks'] += (int)$row['clicks'];
                        $dailyAds[$day]['impressions'] += (int)$row['impressions'];
                        
                        $actionType = $row['result_action_type'] ?? '';
                        if (strpos($actionType, 'install') !== false) {
                            $dailyAds[$day]['installs'] += (int)$row['results'];
                            $dailyAds[$day]['install_spend'] += (float)$row['spend'];
                            // Use exact cost from API
                            if ($row['cost_per_result']) {
                                $dailyAds[$day]['install_cost'] = (float)$row['cost_per_result'];
                            }
                        } elseif (strpos($actionType, 'purchase') !== false) {
                            $dailyAds[$day]['purchases'] += (int)$row['results'];
                            $dailyAds[$day]['purchase_spend'] += (float)$row['spend'];
                            // Use exact cost from API
                            if ($row['cost_per_result']) {
                                $dailyAds[$day]['purchase_cost'] = (float)$row['cost_per_result'];
                            }
                        }
                    }
                }

                // Calculate totals
                $totalRegistrations = array_sum($registrations);
                $totalTrialUsers = array_sum($trialUsers);
                $totalSpend = 0;
                $totalInstalls = 0;
                $totalPurchases = 0;
                $totalInstallSpend = 0;
                $totalPurchaseSpend = 0;
                foreach ($dailyAds as $dayData) {
                    $totalSpend += $dayData['spend'];
                    $totalInstalls += $dayData['installs'];
                    $totalPurchases += $dayData['purchases'];
                    $totalInstallSpend += $dayData['install_spend'];
                    $totalPurchaseSpend += $dayData['purchase_spend'];
                }
                $avgCostPerInstall = $totalInstalls > 0 ? round($totalInstallSpend / $totalInstalls, 2) : null;
                $avgCostPerPurchase = $totalPurchases > 0 ? round($totalPurchaseSpend / $totalPurchases, 2) : null;
                $costPerRegistration = $totalRegistrations > 0 ? round($totalSpend / $totalRegistrations, 2) : 0;

                if ($format === 'csv') {
                    header('Content-Type: text/csv');
                    header('Content-Disposition: attachment; filename="combined_daily_report_' . $start_date . '_to_' . $end_date . '.csv"');

                    $output = fopen('php://output', 'w');
                    
                    // Header row
                    fputcsv($output, [
                        'Date', 
                        'Registrations', 
                        '₹5 Trial Starts',
                        'Ad Spend',
                        'Installs',
                        'Cost/Install',
                        'Purchases',
                        'Cost/Purchase',
                        'Clicks'
                    ]);

                    // Data rows (descending date)
                    $sortedDates = array_reverse($dates);
                    foreach ($sortedDates as $date) {
                        $ads = $dailyAds[$date] ?? null;
                        
                        fputcsv($output, [
                            $date,
                            $registrations[$date] ?? 0,
                            $trialUsers[$date] ?? 0,
                            $ads ? '₹' . round($ads['spend'], 2) : '₹0',
                            $ads ? $ads['installs'] : 0,
                            ($ads && $ads['install_cost']) ? '₹' . round($ads['install_cost'], 2) : '-',
                            $ads ? $ads['purchases'] : 0,
                            ($ads && $ads['purchase_cost']) ? '₹' . round($ads['purchase_cost'], 2) : '-',
                            $ads ? $ads['clicks'] : 0
                        ]);
                    }

                    // Empty row
                    fputcsv($output, []);

                    // Totals row
                    fputcsv($output, [
                        'TOTAL',
                        $totalRegistrations,
                        $totalTrialUsers,
                        '₹' . round($totalSpend, 2),
                        $totalInstalls,
                        $avgCostPerInstall ? '₹' . $avgCostPerInstall : '-',
                        $totalPurchases,
                        $avgCostPerPurchase ? '₹' . $avgCostPerPurchase : '-',
                        array_sum(array_column($dailyAds, 'clicks'))
                    ]);

                    // Cost per registration
                    fputcsv($output, []);
                    fputcsv($output, ['Cost Per Registration', '₹' . $costPerRegistration]);

                    fclose($output);
                    exit;
                }

                // JSON output
                $data = [];
                $sortedDates = array_reverse($dates);
                foreach ($sortedDates as $date) {
                    $ads = $dailyAds[$date] ?? null;
                    $data[] = [
                        'date' => $date,
                        'registrations' => $registrations[$date] ?? 0,
                        'trial_users' => $trialUsers[$date] ?? 0,
                        'ad_spend' => $ads ? round($ads['spend'], 2) : 0,
                        'installs' => $ads ? $ads['installs'] : 0,
                        'cost_per_install' => ($ads && $ads['install_cost']) ? round($ads['install_cost'], 2) : null,
                        'purchases' => $ads ? $ads['purchases'] : 0,
                        'cost_per_purchase' => ($ads && $ads['purchase_cost']) ? round($ads['purchase_cost'], 2) : null,
                        'clicks' => $ads ? $ads['clicks'] : 0
                    ];
                }

                http_response_code(200);
                echo json_encode([
                    'success' => true,
                    'type' => $type,
                    'start_date' => $start_date,
                    'end_date' => $end_date,
                    'count' => count($data),
                    'totals' => [
                        'registrations' => $totalRegistrations,
                        'trial_users' => $totalTrialUsers,
                        'ad_spend' => round($totalSpend, 2),
                        'installs' => $totalInstalls,
                        'cost_per_install' => $avgCostPerInstall,
                        'purchases' => $totalPurchases,
                        'cost_per_purchase' => $avgCostPerPurchase,
                        'cost_per_registration' => $costPerRegistration
                    ],
                    'data' => $data
                ]);
                break;

            case 'analytics':
                // Export analytics summary
                $analytics = [
                    'total_users' => $db->query("SELECT COUNT(*) FROM users")->fetchColumn(),
                    'total_tests' => $db->query("SELECT COUNT(*) FROM test_results")->fetchColumn(),
                    'avg_score' => round($db->query("SELECT AVG(percentage) FROM test_results")->fetchColumn(), 1),
                    'total_questions' => $db->query("SELECT COUNT(*) FROM questions")->fetchColumn(),
                    'export_date' => date('Y-m-d H:i:s')
                ];

                if ($format === 'json') {
                    http_response_code(200);
                    echo json_encode([
                        'success' => true,
                        'type' => $type,
                        'format' => $format,
                        'data' => $analytics
                    ]);
                } else {
                    http_response_code(400);
                    echo json_encode(['success' => false, 'message' => 'Analytics export only supports JSON format']);
                }
                break;

            case 'meta_ads_insights':
                // Export Meta Ads insights for Google Sheets
                // Columns: Date, Campaign, Spend, Results, Cost/Result, Clicks, CPC, Impressions, CTR

                // Defaults (last 30 days)
                if (!$start_date) {
                    $start_date = date('Y-m-d', strtotime('-30 days'));
                }
                if (!$end_date) {
                    $end_date = date('Y-m-d');
                }

                $level = isset($_GET['level']) ? $_GET['level'] : 'campaign';
                $validLevels = ['campaign', 'adset', 'ad'];
                if (!in_array($level, $validLevels)) {
                    throw new Exception('Invalid level. Must be one of: ' . implode(', ', $validLevels));
                }

                // Check if meta_ads_insights_daily table exists
                $tableCheck = $db->query("SHOW TABLES LIKE 'meta_ads_insights_daily'");
                if ($tableCheck->rowCount() === 0) {
                    throw new Exception('Meta Ads not configured. Run /api/install/create_meta_ads_table.php first.');
                }

                // Build query based on level
                $entityIdCol = $level . '_id';
                $entityNameCol = $level . '_name';

                $query = "SELECT 
                    day,
                    {$entityNameCol} as entity_name,
                    spend,
                    results,
                    cost_per_result,
                    clicks,
                    cpc,
                    impressions,
                    ctr,
                    result_action_type
                FROM meta_ads_insights_daily
                WHERE day BETWEEN ? AND ? AND level = ?
                ORDER BY day DESC, spend DESC";

                $stmt = $db->prepare($query);
                $stmt->execute([$start_date, $end_date, $level]);
                $data = $stmt->fetchAll(PDO::FETCH_ASSOC);

                if ($format === 'csv') {
                    header('Content-Type: text/csv');
                    header('Content-Disposition: attachment; filename="meta_ads_' . $level . '_' . $start_date . '_to_' . $end_date . '.csv"');

                    $output = fopen('php://output', 'w');
                    fputcsv($output, ['Date', ucfirst($level), 'Spend (INR)', 'Results', 'Cost Per Result', 'Clicks', 'CPC', 'Impressions', 'CTR (%)', 'Result Type']);

                    foreach ($data as $row) {
                        fputcsv($output, [
                            $row['day'],
                            $row['entity_name'] ?? '',
                            round($row['spend'] ?? 0, 2),
                            $row['results'] ?? 0,
                            $row['cost_per_result'] ? round($row['cost_per_result'], 2) : '',
                            $row['clicks'] ?? 0,
                            $row['cpc'] ? round($row['cpc'], 2) : '',
                            $row['impressions'] ?? 0,
                            $row['ctr'] ? round($row['ctr'], 2) : '',
                            $row['result_action_type'] ?? ''
                        ]);
                    }
                    fclose($output);
                    exit;
                }

                http_response_code(200);
                echo json_encode([
                    'success' => true,
                    'type' => $type,
                    'format' => $format,
                    'level' => $level,
                    'start_date' => $start_date,
                    'end_date' => $end_date,
                    'count' => count($data),
                    'data' => $data
                ]);
                break;

            default:
                throw new Exception('Invalid export type');
        }

    } catch (Exception $e) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => $e->getMessage()
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed.']);
}
?>

