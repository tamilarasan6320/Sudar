<?php
/**
 * Admin - List Trial Starts by Date
 *
 * GET /api/admin/subscriptions/trial_starts.php?date=YYYY-MM-DD&limit=200&offset=0
 */
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php';

header('Content-Type: application/json');

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

try {
    $date = isset($_GET['date']) ? trim((string)$_GET['date']) : '';
    $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 200;
    $offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;

    if ($limit < 1) $limit = 200;
    if ($limit > 500) $limit = 500;
    if ($offset < 0) $offset = 0;

    // Default to IST today
    if ($date === '') {
        $date = (string)$db->query("SELECT DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+05:30'))")->fetchColumn();
    }

    if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $date)) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Invalid date. Use YYYY-MM-DD']);
        exit;
    }

    // Safe fallback if table doesn't exist
    $table_check = $db->query("SHOW TABLES LIKE 'subscriptions'");
    if ($table_check->rowCount() === 0) {
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'date' => $date,
            'total' => 0,
            'count' => 0,
            'limit' => $limit,
            'offset' => $offset,
            'trials' => []
        ]);
        exit;
    }

    // Interpret date as IST date and compare with UTC stored trial_start
    $countStmt = $db->prepare("SELECT COUNT(*) FROM subscriptions WHERE trial_start IS NOT NULL AND DATE(CONVERT_TZ(trial_start,'+00:00','+05:30')) = :date");
    $countStmt->bindValue(':date', $date, PDO::PARAM_STR);
    $countStmt->execute();
    $total = (int)$countStmt->fetchColumn();

    $stmt = $db->prepare("
        SELECT
            s.id AS subscription_id,
            s.user_id,
            s.status,
            s.is_trial,
            s.plan_name,
            s.amount,
            s.trial_start,
            s.trial_end,
            s.created_at,
            u.name AS user_name,
            u.mobile AS user_mobile,
            u.email AS user_email
        FROM subscriptions s
        LEFT JOIN users u ON s.user_id = u.id
        WHERE s.trial_start IS NOT NULL AND DATE(CONVERT_TZ(s.trial_start,'+00:00','+05:30')) = :date
        ORDER BY s.trial_start DESC, s.id DESC
        LIMIT :limit OFFSET :offset
    ");
    $stmt->bindValue(':date', $date, PDO::PARAM_STR);
    $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
    $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
    $stmt->execute();

    $trials = $stmt->fetchAll(PDO::FETCH_ASSOC);

    http_response_code(200);
    echo json_encode([
        'success' => true,
        'date' => $date,
        'total' => $total,
        'count' => count($trials),
        'limit' => $limit,
        'offset' => $offset,
        'trials' => $trials
    ]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error loading trial users',
        'error' => $e->getMessage()
    ]);
}

