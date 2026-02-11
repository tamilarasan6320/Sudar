<?php
/**
 * Track first-open install attribution for referral links.
 *
 * This endpoint is called by the mobile app on FIRST app open after install.
 * It records one install per device_id and stores the referral CODE.
 *
 * POST /api/referrals/track_install.php
 * Body (JSON):
 * {
 *   "device_id": "abc123...",
 *   "code": "TG001",
 *   "install_referrer": "ref=TG001" (optional)
 * }
 */

require_once '../config/cors.php';
require_once '../config/database.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

$data = json_decode(file_get_contents('php://input'), true);
if (!is_array($data)) $data = [];

function normalize_code($code) {
    $code = trim((string)$code);
    $code = preg_replace('/\s+/', '', $code);
    return $code;
}

function validate_code($code) {
    if ($code === '') return false;
    if (strlen($code) < 2 || strlen($code) > 64) return false;
    return preg_match('/^[A-Za-z0-9_-]+$/', $code) === 1;
}

function extract_param($query, $key) {
    $query = (string)$query;
    $key = (string)$key;
    if ($query === '' || $key === '') return null;
    $parts = explode('&', $query);
    foreach ($parts as $p) {
        $kv = explode('=', $p, 2);
        if (count($kv) === 2 && strcasecmp(trim($kv[0]), $key) === 0) {
            $v = trim($kv[1]);
            return $v !== '' ? $v : null;
        }
    }
    return null;
}

$deviceId = trim((string)($data['device_id'] ?? ''));
$installReferrer = trim((string)($data['install_referrer'] ?? ''));
$code = normalize_code($data['code'] ?? '');

// Allow sending only install_referrer; backend will extract code from ref=CODE
if ($code === '' && $installReferrer !== '') {
    $c = extract_param($installReferrer, 'ref');
    if ($c) $code = normalize_code($c);
}

if ($deviceId === '' || strlen($deviceId) < 8 || strlen($deviceId) > 64) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Missing or invalid device_id']);
    exit;
}

if (!validate_code($code)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Missing or invalid code']);
    exit;
}

$database = new Database();
$db = $database->getConnection();

if (!$db) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database connection failed']);
    exit;
}

try {
    // Ensure tables exist
    $db->exec("CREATE TABLE IF NOT EXISTS referral_links (
        id INT(11) AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(191) NOT NULL,
        code VARCHAR(64) NOT NULL,
        is_active TINYINT(1) NOT NULL DEFAULT 1,
        click_count INT(11) NOT NULL DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY uniq_code (code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    $db->exec("CREATE TABLE IF NOT EXISTS referral_installs (
        id INT(11) AUTO_INCREMENT PRIMARY KEY,
        code VARCHAR(64) NOT NULL,
        device_id VARCHAR(64) NOT NULL,
        user_id INT(11) NULL,
        install_referrer TEXT NULL,
        first_open_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY uniq_device (device_id),
        KEY idx_code (code),
        KEY idx_user (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    // Validate code exists and is active
    $stmt = $db->prepare("SELECT id, is_active FROM referral_links WHERE code = ? LIMIT 1");
    $stmt->execute([$code]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    if (!$row || (int)($row['is_active'] ?? 0) !== 1) {
        http_response_code(404);
        echo json_encode(['success' => false, 'message' => 'Referral code not found or inactive']);
        exit;
    }

    // Idempotent insert: one per device_id
    $ins = $db->prepare("INSERT INTO referral_installs (code, device_id, install_referrer, first_open_at)
                         VALUES (?, ?, ?, NOW())
                         ON DUPLICATE KEY UPDATE
                            code = IF(code IS NULL OR code = '', VALUES(code), code),
                            install_referrer = IF(install_referrer IS NULL OR install_referrer = '', VALUES(install_referrer), install_referrer)");
    $ins->execute([$code, $deviceId, $installReferrer !== '' ? $installReferrer : null]);

    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => 'Install tracked',
    ]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error tracking install',
        'error' => $e->getMessage(),
    ]);
}
?>

