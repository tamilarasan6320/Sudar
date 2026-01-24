<?php
/**
 * //test
 * Tcaller Login API Endpoin
 * =============================
 * Verify Truecaller access token and authenticate user
 * 
 * Method: POST
 * Content-Type: application/json
 * 
 * Request Body:
 * {
 *   "access_token": "truecaller_oauth_access_token",
 *   "device_id": "a1b2c3..."
 * }
 * 
 * Response (Existing User):
 * {
 *   "success": true,
 *   "message": "Login successful",
 *   "is_new_user": false,
 *   "user": { ... },
 *   "token": "...",
 *   "mobile": "9876543210"
 * }
 * 
 * Response (New User):
 * {
 *   "success": true,
 *   "message": "Truecaller verified. Please complete registration.",
 *   "is_new_user": true,
 *   "mobile": "9876543210",
 *   "token": "..."
 * }
 */

// Ensure clean JSON output
ob_start();
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

// Catch any fatal errors
register_shutdown_function(function() {
    $error = error_get_last();
    if ($error !== null && in_array($error['type'], [E_ERROR, E_PARSE, E_CORE_ERROR, E_COMPILE_ERROR])) {
        ob_clean();
        header('Content-Type: application/json');
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Server error occurred',
            'error_code' => 'FATAL_ERROR',
            'debug' => $error['message']
        ]);
    }
});

// Set JSON header early
header('Content-Type: application/json');

// Debug: Log that the script started
error_log("Truecaller Login: Script started");

try {
    require_once '../config/cors.php';
    require_once '../config/database.php';
    require_once '../models/User.php';
} catch (Exception $e) {
    error_log("Truecaller Login: Include error - " . $e->getMessage());
    ob_clean();
    echo json_encode([
        'success' => false,
        'message' => 'Server configuration error',
        'error_code' => 'CONFIG_ERROR'
    ]);
    exit;
}

// Clean any output from includes
ob_clean();

// Only allow POST requests
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed. Use POST request.',
        'error_code' => 'METHOD_NOT_ALLOWED'
    ]);
    exit;
}

// Parse request body
$data = json_decode(file_get_contents('php://input'));

// Validate request
if (empty($data->access_token) || empty($data->device_id)) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Access token and device_id are required.',
        'error_code' => 'MISSING_FIELDS'
    ]);
    exit;
}

try {
    // Verify access token with Truecaller userinfo endpoint
    $userInfo = verifyTruecallerToken($data->access_token);
    
    if ($userInfo === null) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Invalid or expired Truecaller token.',
            'error_code' => 'INVALID_TOKEN'
        ]);
        exit;
    }
    
    // Check if phone number is verified
    if (!isset($userInfo['phone_number_verified']) || $userInfo['phone_number_verified'] !== true) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Phone number not verified by Truecaller.',
            'error_code' => 'PHONE_NOT_VERIFIED'
        ]);
        exit;
    }
    
    // Extract and normalize phone number
    $phoneNumber = $userInfo['phone_number'] ?? null;
    if (empty($phoneNumber)) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Phone number not available from Truecaller.',
            'error_code' => 'NO_PHONE_NUMBER'
        ]);
        exit;
    }
    
    $mobile = normalizePhoneNumber($phoneNumber);
    if (strlen($mobile) !== 10) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Invalid phone number format.',
            'error_code' => 'INVALID_PHONE_FORMAT'
        ]);
        exit;
    }
    
    // Normalize device_id
    $deviceId = trim((string)$data->device_id);
    if (strlen($deviceId) < 8 || strlen($deviceId) > 64) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Invalid device_id.',
            'error_code' => 'INVALID_DEVICE_ID'
        ]);
        exit;
    }
    
    // Initialize database connection
    $database = new Database();
    $db = $database->getConnection();
    
    // Check if user exists
    $user = new User($db);
    $user->mobile = $mobile;
    $userStmt = $user->getUserByMobile();
    
    if ($userStmt->rowCount() > 0) {
        // Existing user - update last login and create session
        $userData = $userStmt->fetch(PDO::FETCH_ASSOC);
        $user->id = $userData['id'];
        
        // Rotate session token and bind it to this device (single-device login)
        $sessionToken = bin2hex(random_bytes(32)); // 64 chars
        $user->updateDeviceSession($deviceId, $sessionToken);
        
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => 'Login successful',
            'is_new_user' => false,
            'user' => [
                'id' => $userData['id'],
                'name' => $userData['name'],
                'mobile' => $userData['mobile'],
                'email' => $userData['email'] ?? null,
                'age' => $userData['age'] ?? null,
                'district' => $userData['district'] ?? null,
                'education' => $userData['education'] ?? null,
                'language' => $userData['language'] ?? 'en',
                'profile_pic' => $userData['profile_pic'] ?? null
            ],
            'token' => $sessionToken,
            'mobile' => $mobile
        ]);
    } else {
        // New user - registration required
        // Generate temporary token for profile creation
        $token = generateAuthToken($mobile);
        
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => 'Truecaller verified. Please complete registration.',
            'is_new_user' => true,
            'mobile' => $mobile,
            'token' => $token
        ]);
    }
    
} catch (Exception $e) {
    error_log("Truecaller Login Error: " . $e->getMessage() . " in " . $e->getFile() . ":" . $e->getLine());
    ob_clean();
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'An unexpected error occurred. Please try again.',
        'error_code' => 'SERVER_ERROR',
        'debug' => $e->getMessage()
    ]);
}

/**
 * Verify Truecaller access token by calling userinfo endpoint
 * 
 * @param string $accessToken - Truecaller OAuth access token
 * @return array|null - User info array or null if invalid
 */
function verifyTruecallerToken($accessToken) {
    // Truecaller userinfo endpoint (non-EU region)
    $url = 'https://oauth-account-noneu.truecaller.com/v1/userinfo';
    
    // Check if cURL is available
    if (!function_exists('curl_init')) {
        error_log("Truecaller API: cURL extension not available, trying file_get_contents");
        return verifyTruecallerTokenFallback($accessToken);
    }
    
    $ch = curl_init();
    curl_setopt_array($ch, [
        CURLOPT_URL => $url,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => [
            'Authorization: Bearer ' . $accessToken,
            'Accept: application/json'
        ],
        CURLOPT_TIMEOUT => 15,
        CURLOPT_SSL_VERIFYPEER => false, // Disable SSL verification for shared hosting
        CURLOPT_SSL_VERIFYHOST => 0,
        CURLOPT_FOLLOWLOCATION => true,
    ]);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);
    
    if ($curlError) {
        error_log("Truecaller API cURL error: " . $curlError);
        // Try fallback method
        return verifyTruecallerTokenFallback($accessToken);
    }
    
    if ($httpCode !== 200) {
        error_log("Truecaller API error: HTTP $httpCode - $response");
        return null;
    }
    
    $data = json_decode($response, true);
    if (json_last_error() !== JSON_ERROR_NONE) {
        error_log("Truecaller API JSON error: " . json_last_error_msg());
        return null;
    }
    
    return $data;
}

/**
 * Fallback method using file_get_contents if cURL fails
 */
function verifyTruecallerTokenFallback($accessToken) {
    $url = 'https://oauth-account-noneu.truecaller.com/v1/userinfo';
    
    $opts = [
        'http' => [
            'method' => 'GET',
            'header' => "Authorization: Bearer $accessToken\r\nAccept: application/json\r\n",
            'timeout' => 15,
            'ignore_errors' => true
        ],
        'ssl' => [
            'verify_peer' => false,
            'verify_peer_name' => false
        ]
    ];
    
    $context = stream_context_create($opts);
    $response = @file_get_contents($url, false, $context);
    
    if ($response === false) {
        error_log("Truecaller API fallback error: file_get_contents failed");
        return null;
    }
    
    // Check HTTP response code from headers
    if (isset($http_response_header)) {
        $statusLine = $http_response_header[0];
        if (strpos($statusLine, '200') === false) {
            error_log("Truecaller API fallback error: $statusLine - $response");
            return null;
        }
    }
    
    $data = json_decode($response, true);
    if (json_last_error() !== JSON_ERROR_NONE) {
        error_log("Truecaller API fallback JSON error: " . json_last_error_msg());
        return null;
    }
    
    return $data;
}

/**
 * Normalize phone number to 10 digits (Indian format)
 * 
 * @param string $phone - Phone number (may include country code)
 * @return string - 10-digit phone number
 */
function normalizePhoneNumber($phone) {
    // Remove all non-digit characters
    $cleaned = preg_replace('/[^\d]/', '', $phone);
    
    // Remove +91 or 91 prefix if present
    if (substr($cleaned, 0, 2) === '91' && strlen($cleaned) > 10) {
        $cleaned = substr($cleaned, 2);
    }
    
    // Take last 10 digits if still longer
    if (strlen($cleaned) > 10) {
        $cleaned = substr($cleaned, -10);
    }
    
    return $cleaned;
}

/**
 * Generate Authentication Token
 * 
 * @param mixed $identifier - User ID or Mobile number
 * @return string - JWT-like token
 */
function generateAuthToken($identifier) {
    $payload = [
        'sub' => $identifier,
        'iat' => time(),
        'exp' => time() + (30 * 24 * 60 * 60), // 30 days expiry
        'jti' => bin2hex(random_bytes(16))
    ];
    
    // Simple token encoding (consider using proper JWT in production)
    return base64_encode(json_encode($payload)) . '.' . 
           hash_hmac('sha256', json_encode($payload), 'your-secret-key-here');
}
?>

