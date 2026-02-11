<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication

$database = new Database();
$db = $database->getConnection();

if (!$db) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed'
    ]);
    exit;
}

// Ensure table exists
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

    // Conversion tables (installs/trials/purchases) - safe to call repeatedly
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
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Failed to ensure referral_links table',
        'error' => $e->getMessage(),
    ]);
    exit;
}

function normalize_code($code) {
    $code = trim((string)$code);
    // Remove spaces inside
    $code = preg_replace('/\s+/', '', $code);
    return $code;
}

function validate_code($code) {
    if ($code === '') return false;
    if (strlen($code) < 2 || strlen($code) > 64) return false;
    return preg_match('/^[A-Za-z0-9_-]+$/', $code) === 1;
}

$method = $_SERVER['REQUEST_METHOD'];

try {
    if ($method === 'GET') {
        $stmt = $db->query("SELECT id, name, code, is_active, click_count, created_at, updated_at
                            FROM referral_links
                            ORDER BY id DESC");
        $rows = $stmt ? $stmt->fetchAll(PDO::FETCH_ASSOC) : [];

        // Aggregate conversion counts
        $installsByCode = [];
        $trialsByCode = [];
        $purchasesByCode = [];
        $revenueByCode = [];

        try {
            $iStmt = $db->query("SELECT code, COUNT(*) AS installs FROM referral_installs GROUP BY code");
            $iRows = $iStmt ? $iStmt->fetchAll(PDO::FETCH_ASSOC) : [];
            foreach ($iRows as $r) {
                $c = (string)($r['code'] ?? '');
                if ($c !== '') $installsByCode[$c] = (int)($r['installs'] ?? 0);
            }
        } catch (Exception $e) {}

        try {
            $tStmt = $db->query("SELECT code, COUNT(*) AS trials FROM referral_trials GROUP BY code");
            $tRows = $tStmt ? $tStmt->fetchAll(PDO::FETCH_ASSOC) : [];
            foreach ($tRows as $r) {
                $c = (string)($r['code'] ?? '');
                if ($c !== '') $trialsByCode[$c] = (int)($r['trials'] ?? 0);
            }
        } catch (Exception $e) {}

        try {
            $pStmt = $db->query("SELECT code, COUNT(*) AS purchases, COALESCE(SUM(amount), 0) AS revenue FROM referral_purchases GROUP BY code");
            $pRows = $pStmt ? $pStmt->fetchAll(PDO::FETCH_ASSOC) : [];
            foreach ($pRows as $r) {
                $c = (string)($r['code'] ?? '');
                if ($c !== '') {
                    $purchasesByCode[$c] = (int)($r['purchases'] ?? 0);
                    $revenueByCode[$c] = (float)($r['revenue'] ?? 0);
                }
            }
        } catch (Exception $e) {}

        // Attach counts to each referral link
        $rows = array_map(function($row) use ($installsByCode, $trialsByCode, $purchasesByCode, $revenueByCode) {
            $code = (string)($row['code'] ?? '');
            $row['installs'] = (int)($installsByCode[$code] ?? 0);
            $row['trials'] = (int)($trialsByCode[$code] ?? 0);
            $row['purchases'] = (int)($purchasesByCode[$code] ?? 0);
            $row['revenue'] = (float)($revenueByCode[$code] ?? 0);
            return $row;
        }, $rows);

        http_response_code(200);
        echo json_encode([
            'success' => true,
            'data' => $rows,
            'count' => count($rows),
        ]);
        exit;
    }

    $data = json_decode(file_get_contents('php://input'), true);
    if (!is_array($data)) $data = [];

    if ($method === 'POST') {
        $name = trim((string)($data['name'] ?? ''));
        $code = normalize_code($data['code'] ?? '');

        if ($name === '' || $code === '') {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Name and code are required'
            ]);
            exit;
        }

        if (!validate_code($code)) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Invalid code. Allowed: letters, numbers, _ and - (2-64 chars)'
            ]);
            exit;
        }

        $stmt = $db->prepare("INSERT INTO referral_links (name, code, is_active) VALUES (?, ?, 1)");
        $stmt->execute([$name, $code]);

        http_response_code(201);
        echo json_encode([
            'success' => true,
            'message' => 'Referral link created',
            'id' => (int)$db->lastInsertId(),
        ]);
        exit;
    }

    if ($method === 'PUT') {
        $id = (int)($data['id'] ?? 0);
        if ($id <= 0) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'ID is required']);
            exit;
        }

        $updates = [];
        $params = [];

        if (isset($data['name'])) {
            $name = trim((string)$data['name']);
            if ($name === '') {
                http_response_code(400);
                echo json_encode(['success' => false, 'message' => 'Name cannot be empty']);
                exit;
            }
            $updates[] = "name = ?";
            $params[] = $name;
        }

        if (isset($data['is_active'])) {
            $isActive = (int)$data['is_active'] ? 1 : 0;
            $updates[] = "is_active = ?";
            $params[] = $isActive;
        }

        if (empty($updates)) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'No fields to update']);
            exit;
        }

        $params[] = $id;
        $sql = "UPDATE referral_links SET " . implode(", ", $updates) . " WHERE id = ?";
        $stmt = $db->prepare($sql);
        $stmt->execute($params);

        http_response_code(200);
        echo json_encode(['success' => true, 'message' => 'Updated']);
        exit;
    }

    if ($method === 'DELETE') {
        $id = (int)($data['id'] ?? 0);
        if ($id <= 0) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'ID is required']);
            exit;
        }

        $stmt = $db->prepare("DELETE FROM referral_links WHERE id = ?");
        $stmt->execute([$id]);

        http_response_code(200);
        echo json_encode(['success' => true, 'message' => 'Deleted']);
        exit;
    }

    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
} catch (PDOException $e) {
    // Duplicate code
    $msg = $e->getMessage();
    if (strpos($msg, 'Duplicate') !== false || strpos($msg, 'duplicate') !== false) {
        http_response_code(409);
        echo json_encode(['success' => false, 'message' => 'Code already exists']);
        exit;
    }

    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Database error', 'error' => $e->getMessage()]);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Server error', 'error' => $e->getMessage()]);
}
?>

