<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

try {
    $date = isset($_GET['date']) ? trim((string)$_GET['date']) : '';
    $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 100;
    $offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;

    if ($limit < 1) $limit = 100;
    if ($limit > 500) $limit = 500;
    if ($offset < 0) $offset = 0;

    // Default: IST today
    if ($date === '') {
        $date = (string)$db->query("SELECT DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+05:30'))")->fetchColumn();
    }

    if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $date)) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Invalid date. Use YYYY-MM-DD']);
        exit;
    }

    // Interpret date as IST date and compare with UTC stored created_at
    $countStmt = $db->prepare("SELECT COUNT(*) FROM users WHERE DATE(CONVERT_TZ(created_at,'+00:00','+05:30')) = :date");
    $countStmt->bindValue(':date', $date, PDO::PARAM_STR);
    $countStmt->execute();
    $total = (int)$countStmt->fetchColumn();

    $stmt = $db->prepare("
        SELECT id, name, mobile, email, created_at, is_active
        FROM users
        WHERE DATE(CONVERT_TZ(created_at,'+00:00','+05:30')) = :date
        ORDER BY created_at DESC
        LIMIT :limit OFFSET :offset
    ");
    $stmt->bindValue(':date', $date, PDO::PARAM_STR);
    $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
    $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
    $stmt->execute();
    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);

    http_response_code(200);
    echo json_encode([
        'success' => true,
        'date' => $date,
        'total' => $total,
        'count' => count($users),
        'limit' => $limit,
        'offset' => $offset,
        'users' => $users
    ]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error fetching registrations',
        'error' => $e->getMessage()
    ]);
}

