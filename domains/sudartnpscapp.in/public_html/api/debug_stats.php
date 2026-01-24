<?php
// Simple debug - no authentication
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

require_once 'config/database.php';

$database = new Database();
$db = $database->getConnection();

$result = [];

// Check server time
$result['server_time'] = date('Y-m-d H:i:s');
$result['server_timezone'] = date_default_timezone_get();

// MySQL time
$mysql_time = $db->query("SELECT NOW() as now, CURDATE() as today")->fetch(PDO::FETCH_ASSOC);
$result['mysql_now'] = $mysql_time['now'];
$result['mysql_today'] = $mysql_time['today'];

// Total users
$result['total_users'] = (int)$db->query("SELECT COUNT(*) FROM users")->fetchColumn();

// Users registered today (CURDATE)
$result['users_today_curdate'] = (int)$db->query("SELECT COUNT(*) FROM users WHERE DATE(created_at) = CURDATE()")->fetchColumn();

// Users last 24 hours
$result['users_last_24h'] = (int)$db->query("SELECT COUNT(*) FROM users WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)")->fetchColumn();

// Latest 5 users with created_at
$latest = $db->query("SELECT id, name, mobile, created_at FROM users ORDER BY created_at DESC LIMIT 5")->fetchAll(PDO::FETCH_ASSOC);
$result['latest_users'] = $latest;

// Subscriptions check
try {
    $table_check = $db->query("SHOW TABLES LIKE 'subscriptions'");
    if ($table_check->rowCount() > 0) {
        $result['subscriptions_table_exists'] = true;
        $result['total_subscriptions'] = (int)$db->query("SELECT COUNT(*) FROM subscriptions")->fetchColumn();
        $result['trial_today'] = (int)$db->query("SELECT COUNT(*) FROM subscriptions WHERE DATE(trial_start) = CURDATE()")->fetchColumn();
        $result['trial_last_24h'] = (int)$db->query("SELECT COUNT(*) FROM subscriptions WHERE trial_start >= DATE_SUB(NOW(), INTERVAL 24 HOUR)")->fetchColumn();
        $result['active_paid'] = (int)$db->query("SELECT COUNT(*) FROM subscriptions WHERE status = 'active' AND is_trial = 0")->fetchColumn();
    } else {
        $result['subscriptions_table_exists'] = false;
    }
} catch (Exception $e) {
    $result['subscriptions_error'] = $e->getMessage();
}

echo json_encode($result, JSON_PRETTY_PRINT);
?>
