<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication

$database = new Database();
$db = $database->getConnection();

if (!$db) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database connection failed']);
    exit;
}

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

$code = normalize_code($_GET['code'] ?? '');
if (!validate_code($code)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Missing or invalid code']);
    exit;
}

// Ensure tables exist (safe)
try {
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

    $db->exec("CREATE TABLE IF NOT EXISTS referral_trials (
        id INT(11) AUTO_INCREMENT PRIMARY KEY,
        code VARCHAR(64) NOT NULL,
        user_id INT(11) NOT NULL,
        subscription_id INT(11) NOT NULL,
        razorpay_subscription_id VARCHAR(100) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY uniq_rzp_sub (razorpay_subscription_id),
        KEY idx_code (code),
        KEY idx_user (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

    $db->exec("CREATE TABLE IF NOT EXISTS referral_purchases (
        id INT(11) AUTO_INCREMENT PRIMARY KEY,
        code VARCHAR(64) NOT NULL,
        user_id INT(11) NOT NULL,
        subscription_id INT(11) NOT NULL,
        razorpay_payment_id VARCHAR(100) NOT NULL,
        amount DECIMAL(10,2) NOT NULL,
        currency VARCHAR(10) NOT NULL DEFAULT 'INR',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY uniq_payment (razorpay_payment_id),
        KEY idx_code (code),
        KEY idx_user (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
} catch (Exception $e) {
    // ignore - still try best-effort queries
}

try {
    // Link record
    $linkStmt = $db->prepare("SELECT id, name, code, is_active, click_count, created_at, updated_at
                              FROM referral_links
                              WHERE code = ?
                              LIMIT 1");
    $linkStmt->execute([$code]);
    $link = $linkStmt->fetch(PDO::FETCH_ASSOC);
    if (!$link) {
        http_response_code(404);
        echo json_encode(['success' => false, 'message' => 'Referral code not found']);
        exit;
    }

    $limit = 250;

    // Installs (first open)
    $installsStmt = $db->prepare("
        SELECT
            ri.id,
            ri.code,
            ri.device_id,
            ri.user_id,
            ri.first_open_at,
            ri.install_referrer,
            u.name AS user_name,
            u.mobile AS user_mobile,
            u.created_at AS user_created_at
        FROM referral_installs ri
        LEFT JOIN users u ON u.id = ri.user_id
        WHERE ri.code = ?
        ORDER BY ri.first_open_at DESC
        LIMIT $limit
    ");
    $installsStmt->execute([$code]);
    $installs = $installsStmt->fetchAll(PDO::FETCH_ASSOC);

    // Trials (trial activated)
    $trialsStmt = $db->prepare("
        SELECT
            rt.id,
            rt.code,
            rt.user_id,
            rt.subscription_id,
            rt.razorpay_subscription_id,
            rt.created_at,
            u.name AS user_name,
            u.mobile AS user_mobile,
            u.created_at AS user_created_at
        FROM referral_trials rt
        LEFT JOIN users u ON u.id = rt.user_id
        WHERE rt.code = ?
        ORDER BY rt.created_at DESC
        LIMIT $limit
    ");
    $trialsStmt->execute([$code]);
    $trials = $trialsStmt->fetchAll(PDO::FETCH_ASSOC);

    // Purchases (subscription.charged)
    $purchasesStmt = $db->prepare("
        SELECT
            rp.id,
            rp.code,
            rp.user_id,
            rp.subscription_id,
            rp.razorpay_payment_id,
            rp.amount,
            rp.currency,
            rp.created_at,
            u.name AS user_name,
            u.mobile AS user_mobile,
            u.created_at AS user_created_at
        FROM referral_purchases rp
        LEFT JOIN users u ON u.id = rp.user_id
        WHERE rp.code = ?
        ORDER BY rp.created_at DESC
        LIMIT $limit
    ");
    $purchasesStmt->execute([$code]);
    $purchases = $purchasesStmt->fetchAll(PDO::FETCH_ASSOC);

    // Summary counts
    $installCount = 0;
    $trialCount = 0;
    $purchaseCount = 0;
    $revenue = 0.0;
    $uniqueUsers = [];

    foreach ($installs as $r) {
        $installCount++;
        $uid = (int)($r['user_id'] ?? 0);
        if ($uid > 0) $uniqueUsers[$uid] = 1;
    }
    foreach ($trials as $r) {
        $trialCount++;
        $uid = (int)($r['user_id'] ?? 0);
        if ($uid > 0) $uniqueUsers[$uid] = 1;
    }
    foreach ($purchases as $r) {
        $purchaseCount++;
        $revenue += (float)($r['amount'] ?? 0);
        $uid = (int)($r['user_id'] ?? 0);
        if ($uid > 0) $uniqueUsers[$uid] = 1;
    }

    http_response_code(200);
    echo json_encode([
        'success' => true,
        'link' => $link,
        'summary' => [
            'clicks' => (int)($link['click_count'] ?? 0),
            'installs' => (int)$installCount,
            'trials' => (int)$trialCount,
            'purchases' => (int)$purchaseCount,
            'revenue' => (float)$revenue,
            'unique_users' => (int)count($uniqueUsers),
            'limit' => (int)$limit,
        ],
        'installs' => $installs,
        'trials' => $trials,
        'purchases' => $purchases,
    ]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error loading referral details',
        'error' => $e->getMessage(),
    ]);
}
?>

