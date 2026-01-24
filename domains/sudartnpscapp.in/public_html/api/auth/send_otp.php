<?php
/**
 * Send OTP API Endpoint - Production
 * ===================================
 * Using AuthKey.io SMS Gateway
 * API: https://api.authkey.io/request?authkey=dc0b07c812ca4934&mobile=...&country_code=91&sid=25451&otp=...
 */

require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../config/otp.php';

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
if (empty($data->mobile)) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Mobile number is required.',
        'error_code' => 'MISSING_MOBILE'
    ]);
    exit;
}

try {
    // Validate mobile number
    $mobile = OTPConfig::validateMobile($data->mobile);
    if (!$mobile) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Invalid mobile number. Please enter a valid 10-digit number.',
            'error_code' => 'INVALID_MOBILE'
        ]);
        exit;
    }

    // Connect to database
    $database = new Database();
    $db = $database->getConnection();

    // Generate OTP
    $otp = OTPConfig::generateOTP();
    $expiresAt = OTPConfig::getExpiryTime();

    // Delete old OTPs for this mobile
    $deleteQuery = "DELETE FROM otp_verifications WHERE mobile = ?";
    $deleteStmt = $db->prepare($deleteQuery);
    $deleteStmt->execute([$mobile]);

    // Insert new OTP
    $insertQuery = "INSERT INTO otp_verifications (mobile, otp, expires_at) VALUES (?, ?, ?)";
    $insertStmt = $db->prepare($insertQuery);
    $insertStmt->execute([$mobile, $otp, $expiresAt]);

    // Send SMS via AuthKey.io (Production Mode)
    if (!OTPConfig::isDevelopmentMode()) {
        $apiUrl = OTPConfig::getApiUrl($mobile, $otp);
        
        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL => $apiUrl,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_SSL_VERIFYHOST => 0,
            CURLOPT_TIMEOUT => 30,
            CURLOPT_HTTPHEADER => ['Accept: application/json']
        ]);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlError = curl_error($ch);
        curl_close($ch);
        
        if ($curlError) {
            throw new Exception("SMS service error: $curlError");
        }
        
        $result = json_decode($response, true);
        
        if ($httpCode != 200 || !isset($result['Message']) || stripos($result['Message'], 'success') === false) {
            throw new Exception("SMS sending failed: " . ($result['Message'] ?? 'Unknown error'));
        }
        
        // Success - SMS sent
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => 'OTP sent successfully to your mobile number.',
            'mobile' => substr($mobile, 0, 2) . '****' . substr($mobile, -4),
            'expires_in' => OTPConfig::OTP_EXPIRY_MINUTES * 60,
            'resend_in' => OTPConfig::RESEND_COOLDOWN_SECONDS
        ]);
    } else {
        // Development mode - return OTP
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => 'OTP sent successfully (Development Mode)',
            'mobile' => substr($mobile, 0, 2) . '****' . substr($mobile, -4),
            'otp' => $otp,
            'expires_in' => OTPConfig::OTP_EXPIRY_MINUTES * 60,
            'resend_in' => OTPConfig::RESEND_COOLDOWN_SECONDS
        ]);
    }

} catch (Exception $e) {
    error_log("Send OTP Error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage(),
        'error_code' => 'SERVER_ERROR'
    ]);
}
?>
