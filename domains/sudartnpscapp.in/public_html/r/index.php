<?php
/**
 * Public referral redirect: /r/CODE
 *
 * Current scope (Admin-first):
 * - Count clicks for active codes in referral_links.click_count
 * - Redirect to Play Store
 * - App deep-link/install attribution will be added later
 */

// Default fallback
$fallbackPlayStoreUrl = 'https://play.google.com/store/apps/details?id=com.sudar.tnpscapp';

function is_valid_ref_code($code) {
    if (!$code) return false;
    if (strlen($code) < 2 || strlen($code) > 64) return false;
    return preg_match('/^[A-Za-z0-9_-]+$/', $code) === 1;
}

function safe_redirect($url) {
    if (!$url) $url = '';
    // Basic header injection protection
    $url = str_replace(["\r", "\n"], '', $url);
    if ($url === '') $url = '/';
    header('Location: ' . $url, true, 302);
    exit;
}

function append_query_param($url, $key, $value) {
    $url = (string)$url;
    $key = (string)$key;

    $parts = @parse_url($url);
    if (!$parts || empty($key)) return $url;

    $query = $parts['query'] ?? '';
    $params = [];
    if ($query !== '') {
        @parse_str($query, $params);
    }
    $params[$key] = $value;

    $newQuery = http_build_query($params, '', '&', PHP_QUERY_RFC3986);

    $scheme = $parts['scheme'] ?? '';
    $host = $parts['host'] ?? '';
    $port = isset($parts['port']) ? (':' . $parts['port']) : '';
    $path = $parts['path'] ?? '';
    $fragment = isset($parts['fragment']) ? ('#' . $parts['fragment']) : '';

    // If parse_url couldn't find host, fallback to original
    if ($host === '' && $scheme !== '') return $url;

    $base = '';
    if ($scheme !== '' && $host !== '') {
        $base = $scheme . '://' . $host . $port . $path;
    } else if ($host !== '') {
        $base = '//' . $host . $port . $path;
    } else {
        // Relative URL
        $base = $path;
    }

    $q = $newQuery !== '' ? ('?' . $newQuery) : '';
    return $base . $q . $fragment;
}

// Parse code from rewrite (?code=CODE)
$code = isset($_GET['code']) ? trim((string)$_GET['code']) : '';

// Connect DB (best-effort; do not block redirect if DB fails)
$db = null;
$playStoreUrl = $fallbackPlayStoreUrl;

try {
    require_once __DIR__ . '/../api/config/database.php';
    $database = new Database();
    $db = $database->getConnection();
} catch (Exception $e) {
    $db = null;
}

// Try to use app_update_url if it points to Play Store
try {
    if ($db) {
        $stmt = $db->prepare("SELECT setting_value FROM app_settings WHERE setting_key = ? LIMIT 1");
        $stmt->execute(['app_update_url']);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        $v = isset($row['setting_value']) ? trim((string)$row['setting_value']) : '';
        if ($v !== '' && stripos($v, 'play.google.com') !== false) {
            $playStoreUrl = $v;
        }
    }
} catch (Exception $e) {
    // ignore
}

// If code invalid, just redirect (no count)
if (!is_valid_ref_code($code)) {
    safe_redirect($playStoreUrl);
}

// Best-effort: ensure table exists (in case link is opened before Admin ever created it)
try {
    if ($db) {
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
    }
} catch (Exception $e) {
    // ignore
}

// Increment click count if code exists + is active
$codeActive = false;
try {
    if ($db) {
        $stmt = $db->prepare("SELECT id, is_active FROM referral_links WHERE code = ? LIMIT 1");
        $stmt->execute([$code]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($row && (int)($row['is_active'] ?? 0) === 1) {
            $codeActive = true;
            $upd = $db->prepare("UPDATE referral_links SET click_count = click_count + 1 WHERE id = ?");
            $upd->execute([(int)$row['id']]);
        }
    }
} catch (Exception $e) {
    // ignore
}

// Attach Play Store install referrer so the app can attribute installs later.
// This will NOT affect existing users; it only helps new installs from this link.
if ($codeActive) {
    $playStoreUrl = append_query_param($playStoreUrl, 'referrer', 'ref=' . $code);
}

safe_redirect($playStoreUrl);

