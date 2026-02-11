<?php
/**
 * Verify OTP API Endpoint - Production
 * =====================================
 */

require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../config/otp.php';
require_once '../models/User.php';

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
if (empty($data->mobile) || empty($data->otp)) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Mobile number and OTP are required.',
        'error_code' => 'MISSING_FIELDS'
    ]);
    exit;
}

// Get device_id from request body or header
$deviceId = null;
if (!empty($data->device_id)) {
    $deviceId = trim($data->device_id);
}
// Also check header as fallback
if (!$deviceId) {
    $headers = function_exists('getallheaders') ? getallheaders() : [];
    if (isset($headers['X-Device-Id'])) $deviceId = trim($headers['X-Device-Id']);
    if (isset($headers['x-device-id'])) $deviceId = trim($headers['x-device-id']);
    if (isset($_SERVER['HTTP_X_DEVICE_ID'])) $deviceId = trim($_SERVER['HTTP_X_DEVICE_ID']);
}

try {
    // Validate mobile number
    $mobile = OTPConfig::validateMobile($data->mobile);
    if (!$mobile) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Invalid mobile number format.',
            'error_code' => 'INVALID_MOBILE'
        ]);
        exit;
    }
    
    $otp = trim($data->otp);
    
    if (strlen($otp) != 6) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Invalid OTP format. Please enter 6-digit OTP.',
            'error_code' => 'INVALID_OTP_FORMAT'
        ]);
        exit;
    }

    // Connect to database
    $database = new Database();
    $db = $database->getConnection();

    // Find valid OTP
    $query = "SELECT * FROM otp_verifications 
              WHERE mobile = ? AND is_verified = 0 AND expires_at > NOW()
              ORDER BY created_at DESC LIMIT 1";
    $stmt = $db->prepare($query);
    $stmt->execute([$mobile]);

    if ($stmt->rowCount() === 0) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'No active OTP found. Please request a new OTP.',
            'error_code' => 'NO_ACTIVE_OTP'
        ]);
        exit;
    }

    $otpRecord = $stmt->fetch(PDO::FETCH_ASSOC);

    // Check attempts
    if ($otpRecord['attempts'] >= 5) {
        http_response_code(429);
        echo json_encode([
            'success' => false,
            'message' => 'Too many failed attempts. Please request a new OTP.',
            'error_code' => 'MAX_ATTEMPTS_EXCEEDED'
        ]);
        exit;
    }

    // Verify OTP
    if ($otpRecord['otp'] === $otp) {
        // Mark as verified
        $updateQuery = "UPDATE otp_verifications SET is_verified = 1 WHERE id = ?";
        $updateStmt = $db->prepare($updateQuery);
        $updateStmt->execute([$otpRecord['id']]);

        // Check if user exists
        $user = new User($db);
        $user->mobile = $mobile;
        $userStmt = $user->getUserByMobile();

        if ($userStmt->rowCount() > 0) {
            // Existing user
            $userData = $userStmt->fetch(PDO::FETCH_ASSOC);
            $user->id = $userData['id'];

            // Generate secure session token
            $sessionToken = bin2hex(random_bytes(32));
            
            // Save device_id and session_token to database for single-device auth
            if ($deviceId) {
                $user->updateDeviceSession($deviceId, $sessionToken);
            } else {
                $user->updateLastLogin();
            }

            // Link install attribution (if any) for this device to this user
            if ($deviceId) {
                try {
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

                    $linkStmt = $db->prepare("UPDATE referral_installs
                                              SET user_id = ?
                                              WHERE device_id = ?
                                                AND (user_id IS NULL OR user_id = 0)
                                              LIMIT 1");
                    $linkStmt->execute([$userData['id'], $deviceId]);
                } catch (Exception $e) {
                    // best-effort
                }
            }

            // Use session token for auth (not the JSON token)
            $token = $sessionToken;

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
                'token' => $token
            ]);
        } else {
            // New user - generate a temporary token
            // Full session will be created when registration is complete
            $tempToken = bin2hex(random_bytes(32));

            http_response_code(200);
            echo json_encode([
                'success' => true,
                'message' => 'OTP verified. Please complete registration.',
                'is_new_user' => true,
                'mobile' => $mobile,
                'token' => $tempToken,
                'device_id' => $deviceId // Pass back so app can use it during registration
            ]);
        }
    } else {
        // Wrong OTP - increment attempts
        $updateQuery = "UPDATE otp_verifications SET attempts = attempts + 1 WHERE id = ?";
        $updateStmt = $db->prepare($updateQuery);
        $updateStmt->execute([$otpRecord['id']]);

        $remainingAttempts = 5 - ($otpRecord['attempts'] + 1);

        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => "Invalid OTP. $remainingAttempts attempts remaining.",
            'error_code' => 'INVALID_OTP',
            'remaining_attempts' => $remainingAttempts
        ]);
    }

} catch (Exception $e) {
    error_log("Verify OTP Error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage(),
        'error_code' => 'SERVER_ERROR'
    ]);
}
?>
