<?php
/**
 * Debug Send OTP - Shows exact error
 */
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

error_reporting(E_ALL);
ini_set('display_errors', 0);

// Get mobile from POST or GET for testing
$input = json_decode(file_get_contents('php://input'), true);
$mobile = $input['mobile'] ?? $_GET['mobile'] ?? '9384938434';

$debug = [
    'step' => 'start',
    'mobile' => $mobile,
    'errors' => []
];

try {
    // Step 1: Include files
    $debug['step'] = 'including files';
    
    require_once '../config/cors.php';
    $debug['cors'] = 'OK';
    
    require_once '../config/database.php';
    $debug['database_config'] = 'OK';
    
    require_once '../config/otp.php';
    $debug['otp_config'] = 'OK';
    
    require_once '../services/OtpService.php';
    $debug['otp_service'] = 'OK';

    // Step 2: Validate mobile
    $debug['step'] = 'validating mobile';
    $cleanMobile = OTPConfig::validateMobile($mobile);
    $debug['clean_mobile'] = $cleanMobile;
    
    if (!$cleanMobile) {
        throw new Exception("Invalid mobile number: $mobile");
    }

    // Step 3: Database connection
    $debug['step'] = 'connecting to database';
    $database = new Database();
    $db = $database->getConnection();
    
    if (!$db) {
        throw new Exception("Database connection failed");
    }
    $debug['db_connected'] = 'OK';

    // Step 4: Initialize OTP Service
    $debug['step'] = 'initializing OTP service';
    $otpService = new OtpService($db);
    $debug['otp_service_init'] = 'OK';

    // Step 5: Generate OTP
    $debug['step'] = 'generating OTP';
    $otp = OTPConfig::generateOTP();
    $debug['generated_otp'] = $otp;
    
    $expiresAt = OTPConfig::getExpiryTime();
    $debug['expires_at'] = $expiresAt;

    // Step 6: Store in database
    $debug['step'] = 'storing in database';
    
    // First, delete old OTPs
    $deleteQuery = "DELETE FROM otp_verifications WHERE mobile = ?";
    $deleteStmt = $db->prepare($deleteQuery);
    $deleteStmt->execute([$cleanMobile]);
    $debug['deleted_old'] = 'OK';
    
    // Insert new OTP
    $insertQuery = "INSERT INTO otp_verifications (mobile, otp, expires_at) VALUES (?, ?, ?)";
    $insertStmt = $db->prepare($insertQuery);
    $insertStmt->execute([$cleanMobile, $otp, $expiresAt]);
    $debug['inserted'] = 'OK';

    // Step 7: Check if production mode - send SMS
    $debug['step'] = 'checking environment mode';
    $debug['env_mode'] = OTPConfig::ENV_MODE;
    
    if (!OTPConfig::isDevelopmentMode()) {
        // Step 8: Send SMS via AuthKey.io
        $debug['step'] = 'sending SMS via AuthKey.io';
        
        $apiUrl = OTPConfig::getApiUrl($cleanMobile, $otp);
        $debug['api_url'] = $apiUrl;
        
        // cURL call
        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL => $apiUrl,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_SSL_VERIFYPEER => false, // Try without SSL verification first
            CURLOPT_SSL_VERIFYHOST => 0,
            CURLOPT_TIMEOUT => 30,
            CURLOPT_CONNECTTIMEOUT => 10,
            CURLOPT_HTTPHEADER => [
                'Accept: application/json',
                'User-Agent: SUDAR-TNPSC-App/1.0'
            ]
        ]);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlError = curl_error($ch);
        $curlErrno = curl_errno($ch);
        curl_close($ch);
        
        $debug['curl_http_code'] = $httpCode;
        $debug['curl_error'] = $curlError;
        $debug['curl_errno'] = $curlErrno;
        $debug['authkey_response'] = $response;
        
        if ($curlError) {
            throw new Exception("cURL Error: $curlError (errno: $curlErrno)");
        }
        
        $result = json_decode($response, true);
        $debug['authkey_parsed'] = $result;
        
        if ($httpCode == 200 && isset($result['Message']) && stripos($result['Message'], 'success') !== false) {
            $debug['sms_sent'] = 'SUCCESS';
            $debug['log_id'] = $result['LogID'] ?? null;
        } else {
            $debug['sms_sent'] = 'FAILED';
            throw new Exception("SMS sending failed: " . ($result['Message'] ?? 'Unknown error'));
        }
    } else {
        $debug['sms_sent'] = 'SKIPPED (development mode)';
    }

    // Success!
    $debug['step'] = 'completed';
    
    echo json_encode([
        'success' => true,
        'message' => 'OTP sent successfully!',
        'mobile' => substr($cleanMobile, 0, 2) . '****' . substr($cleanMobile, -4),
        'otp' => OTPConfig::isDevelopmentMode() ? $otp : '[hidden in production]',
        'expires_in' => 600,
        'debug' => $debug
    ], JSON_PRETTY_PRINT);

} catch (Exception $e) {
    $debug['error'] = $e->getMessage();
    $debug['error_file'] = $e->getFile();
    $debug['error_line'] = $e->getLine();
    $debug['error_trace'] = $e->getTraceAsString();
    
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage(),
        'debug' => $debug
    ], JSON_PRETTY_PRINT);
}
?>

