<?php
/**
 * Resend OTP API Endpoint
 * =======================
 * Resend OTP with cooldown and rate limiting
 * 
 * Method: POST
 * Content-Type: application/json
 * 
 * Request Body:
 * {
 *   "mobile": "9876543210"
 * }
 * 
 * Response:
 * {
 *   "success": true,
 *   "message": "OTP resent successfully",
 *   "mobile": "98****3210",
 *   "expires_in": 600,
 *   "resend_in": 60,
 *   "remaining_resends": 2
 * }
 */

require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../config/otp.php';
require_once '../services/OtpService.php';

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
    // Initialize database connection
    $database = new Database();
    $db = $database->getConnection();
    
    // Initialize OTP Service
    $otpService = new OtpService($db);
    
    // Resend OTP
    $result = $otpService->resendOtp($data->mobile);
    
    // Set appropriate HTTP status code
    if ($result['success']) {
        http_response_code(200);
    } else {
        $errorCode = $result['error_code'] ?? 'UNKNOWN_ERROR';
        
        switch ($errorCode) {
            case 'INVALID_MOBILE':
                http_response_code(400);
                break;
            case 'RATE_LIMITED':
            case 'COOLDOWN_ACTIVE':
            case 'RESEND_LIMIT_EXCEEDED':
                http_response_code(429);
                break;
            case 'SMS_FAILED':
                http_response_code(503);
                break;
            default:
                http_response_code(500);
        }
    }
    
    echo json_encode($result);
    
} catch (Exception $e) {
    error_log("Resend OTP Error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'An unexpected error occurred. Please try again.',
        'error_code' => 'SERVER_ERROR'
    ]);
}
?>

