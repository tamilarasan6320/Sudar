<?php
/**
 * Fetch pages from external URL (with login support)
 * This acts as a server-side proxy to avoid CORS issues
 */

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
header('Content-Type: application/json; charset=UTF-8');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../../config/database.php';
require_once 'config_premium.php';

// Get POST data
$input = json_decode(file_get_contents('php://input'), true);

$baseUrl = $input['base_url'] ?? '';
$sourceName = $input['source_name'] ?? '';
$totalPages = intval($input['total_pages'] ?? 20);

// Use stored credentials if not provided
$email = $input['email'] ?? '';
$password = $input['password'] ?? '';

if (empty($email) && defined('PREMIUM_EMAIL')) {
    $email = PREMIUM_EMAIL;
}
if (empty($password) && defined('PREMIUM_PASSWORD')) {
    $password = PREMIUM_PASSWORD;
}

if (empty($baseUrl) || empty($sourceName)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Base URL and source name are required']);
    exit;
}

// Initialize cURL session for cookies
$cookieFile = sys_get_temp_dir() . '/curl_cookies_' . md5($email) . '.txt';

// Function to make HTTP request with cookies
function fetchPage($url, $cookieFile) {
    $ch = curl_init();
    curl_setopt_array($ch, [
        CURLOPT_URL => $url,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_COOKIEFILE => $cookieFile,
        CURLOPT_COOKIEJAR => $cookieFile,
        CURLOPT_USERAGENT => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        CURLOPT_TIMEOUT => 30,
        CURLOPT_SSL_VERIFYPEER => false
    ]);
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return ['html' => $response, 'code' => $httpCode];
}

// Function to login
function loginToSite($email, $password, $cookieFile) {
    // First, get the login page to get any CSRF tokens
    $loginPageUrl = 'https://civilserviceaspirants.in/auth/login/';
    
    $ch = curl_init();
    curl_setopt_array($ch, [
        CURLOPT_URL => $loginPageUrl,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_COOKIEFILE => $cookieFile,
        CURLOPT_COOKIEJAR => $cookieFile,
        CURLOPT_USERAGENT => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        CURLOPT_SSL_VERIFYPEER => false
    ]);
    curl_exec($ch);
    curl_close($ch);
    
    // Now submit login form
    $loginUrl = 'https://civilserviceaspirants.in/auth/login/';
    $postData = http_build_query([
        'email' => $email,
        'password' => $password,
        'submit' => 'Login'
    ]);
    
    $ch = curl_init();
    curl_setopt_array($ch, [
        CURLOPT_URL => $loginUrl,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => $postData,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_COOKIEFILE => $cookieFile,
        CURLOPT_COOKIEJAR => $cookieFile,
        CURLOPT_USERAGENT => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        CURLOPT_SSL_VERIFYPEER => false,
        CURLOPT_HTTPHEADER => [
            'Content-Type: application/x-www-form-urlencoded'
        ]
    ]);
    $response = curl_exec($ch);
    curl_close($ch);
    
    // Check if login was successful (no login form in response)
    return strpos($response, 'Login if you are already a Premium Member') === false;
}

try {
    $database = new Database();
    $db = $database->getConnection();
    
    // Login if credentials provided
    if (!empty($email) && !empty($password)) {
        $loggedIn = loginToSite($email, $password, $cookieFile);
        if (!$loggedIn) {
            echo json_encode(['success' => false, 'message' => 'Login failed. Check credentials.']);
            exit;
        }
    }
    
    // Create source
    $stmt = $db->prepare("INSERT INTO html_sources (name, created_at) VALUES (?, NOW())");
    $stmt->execute([$sourceName]);
    $sourceId = $db->lastInsertId();
    
    // Fetch all pages
    $pages = [];
    $errors = [];
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SMART URL PATTERN DETECTION
    // Pattern 1: URL ends with -N.php (e.g., Group-4-2022-july-1.php)
    // Pattern 2: URL has ?page=N parameter
    // ═══════════════════════════════════════════════════════════════════════════
    
    $usePhpFilePattern = false;
    $baseUrlForPattern = '';
    
    // Check if URL matches pattern like "...-1.php" or "...-2.php" etc.
    // Use greedy match to get the full path before the number
    if (preg_match('/^(.+)-(\d+)\.php/', $baseUrl, $matches)) {
        $usePhpFilePattern = true;
        $baseUrlForPattern = $matches[1]; // e.g., ".../Group-4-2022-july"
        error_log("URL Pattern detected: {$baseUrlForPattern}-N.php");
    } else {
        error_log("Using ?page=N pattern for: $baseUrl");
    }
    
    $loginFailCount = 0;
    
    for ($i = 1; $i <= $totalPages; $i++) {
        // Generate URL based on pattern
        if ($usePhpFilePattern) {
            // Pattern: Group-4-2022-july-1.php, Group-4-2022-july-2.php, etc.
            $pageUrl = $baseUrlForPattern . '-' . $i . '.php';
        } else {
            // Pattern: ?page=1, ?page=2, etc.
            $pageUrl = $baseUrl . (strpos($baseUrl, '?') !== false ? '&' : '?') . 'page=' . $i;
        }
        
        error_log("Fetching page $i: $pageUrl");
        $result = fetchPage($pageUrl, $cookieFile);
        
        if ($result['code'] === 200 && !empty($result['html'])) {
            // Check if we got actual content (not login page)
            if (strpos($result['html'], 'Login if you are already a Premium Member') !== false) {
                $loginFailCount++;
                $errors[] = "Page $i: Requires login";
                
                // Try to re-login if we get multiple login failures
                if ($loginFailCount <= 2 && !empty($email) && !empty($password)) {
                    error_log("Re-attempting login...");
                    loginToSite($email, $password, $cookieFile);
                    // Retry this page
                    $result = fetchPage($pageUrl, $cookieFile);
                    if ($result['code'] === 200 && strpos($result['html'], 'Login if you are already a Premium Member') === false) {
                        $pages[] = [
                            'page_order' => $i,
                            'title' => "Page $i",
                            'content' => $result['html']
                        ];
                        error_log("Page $i: SUCCESS after re-login");
                        continue;
                    }
                }
                continue;
            }
            
            $pages[] = [
                'page_order' => $i,
                'title' => "Page $i",
                'content' => $result['html']
            ];
            error_log("Page $i: SUCCESS");
        } else {
            $errors[] = "Page $i: HTTP " . $result['code'];
            error_log("Page $i: FAILED - HTTP " . $result['code']);
        }
        
        // Small delay to be nice to the server
        usleep(500000); // 500ms (increased from 300ms)
    }
    
    // Save pages to database
    if (!empty($pages)) {
        $stmt = $db->prepare("
            INSERT INTO html_source_pages (source_id, title, content, page_order, created_at) 
            VALUES (?, ?, ?, ?, NOW())
        ");
        
        foreach ($pages as $page) {
            $stmt->execute([$sourceId, $page['title'], $page['content'], $page['page_order']]);
        }
    }
    
    // Clean up cookie file
    if (file_exists($cookieFile)) {
        unlink($cookieFile);
    }
    
    echo json_encode([
        'success' => true,
        'source_id' => $sourceId,
        'pages_fetched' => count($pages),
        'total_requested' => $totalPages,
        'url_pattern' => $usePhpFilePattern ? 'PHP files (-N.php)' : 'Query param (?page=N)',
        'base_url_detected' => $usePhpFilePattern ? $baseUrlForPattern : $baseUrl,
        'errors' => $errors,
        'message' => "Fetched " . count($pages) . " of $totalPages pages. " . (count($errors) > 0 ? count($errors) . " failed." : "")
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}

