<?php
/**
 * OTP Service Class
 * =================
 * Professional OTP Management Service using AuthKey.io
 * 
 * Features:
 * - Send OTP via SMS
 * - Verify OTP
 * - Rate limiting
 * - Resend cooldown
 * - Attempt tracking
 */

require_once __DIR__ . '/../config/otp.php';

class OtpService {
    private $db;
    private $table = 'otp_verifications';
    
    public function __construct($db) {
        $this->db = $db;
        $this->ensureTableExists();
    }
    
    /**
     * Ensure OTP table exists
     */
    private function ensureTableExists() {
        $sql = "CREATE TABLE IF NOT EXISTS {$this->table} (
            id INT AUTO_INCREMENT PRIMARY KEY,
            mobile VARCHAR(15) NOT NULL,
            otp VARCHAR(10) NOT NULL,
            is_verified TINYINT(1) DEFAULT 0,
            attempts INT DEFAULT 0,
            resend_count INT DEFAULT 0,
            ip_address VARCHAR(45) DEFAULT NULL,
            expires_at DATETIME NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            INDEX idx_mobile (mobile),
            INDEX idx_expires (expires_at),
            INDEX idx_created (created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        try {
            $this->db->exec($sql);
        } catch (PDOException $e) {
            error_log("OTP Table creation error: " . $e->getMessage());
        }
    }
    
    /**
     * Send OTP to Mobile Number
     * 
     * @param string $mobile - 10-digit mobile number
     * @return array - Response with success status
     */
    public function sendOtp($mobile) {
        // Validate mobile number
        $mobile = OTPConfig::validateMobile($mobile);
        if (!$mobile) {
            return [
                'success' => false,
                'message' => 'Invalid mobile number. Please enter a valid 10-digit number.',
                'error_code' => 'INVALID_MOBILE'
            ];
        }
        
        // Check rate limiting
        $rateLimitCheck = $this->checkRateLimit($mobile);
        if (!$rateLimitCheck['allowed']) {
            return [
                'success' => false,
                'message' => $rateLimitCheck['message'],
                'error_code' => 'RATE_LIMITED',
                'retry_after' => $rateLimitCheck['retry_after'] ?? 60
            ];
        }
        
        // Check resend cooldown
        $cooldownCheck = $this->checkResendCooldown($mobile);
        if (!$cooldownCheck['allowed']) {
            return [
                'success' => false,
                'message' => $cooldownCheck['message'],
                'error_code' => 'COOLDOWN_ACTIVE',
                'retry_after' => $cooldownCheck['retry_after']
            ];
        }
        
        // Generate OTP
        // In development mode, always use the fixed test OTP for easy testing.
        $otp = OTPConfig::isDevelopmentMode() ? OTPConfig::TEST_OTP : OTPConfig::generateOTP();
        $expiresAt = OTPConfig::getExpiryTime();
        $ipAddress = $this->getClientIP();
        
        try {
            // Delete any existing unverified OTPs for this mobile
            $this->cleanupOldOtps($mobile);
            
            // Get resend count from last OTP (if any)
            $resendCount = $this->getResendCount($mobile);
            
            // Store OTP in database
            $query = "INSERT INTO {$this->table} (mobile, otp, expires_at, ip_address, resend_count) 
                      VALUES (?, ?, ?, ?, ?)";
            $stmt = $this->db->prepare($query);
            $stmt->execute([$mobile, $otp, $expiresAt, $ipAddress, $resendCount]);
            
            // Send SMS via AuthKey.io (in production mode)
            if (!OTPConfig::isDevelopmentMode()) {
                $smsResult = $this->sendSmsViaAuthKey($mobile, $otp);
                
                if (!$smsResult['success']) {
                    // Log the error but still return success (OTP is stored)
                    error_log("AuthKey.io SMS Error: " . json_encode($smsResult));
                    
                    // In case of SMS failure, return the error
                    return [
                        'success' => false,
                        'message' => 'Failed to send SMS. Please try again.',
                        'error_code' => 'SMS_FAILED',
                        'debug' => OTPConfig::isDevelopmentMode() ? $smsResult : null
                    ];
                }
                
                return [
                    'success' => true,
                    'message' => 'OTP sent successfully to your mobile number.',
                    'mobile' => $this->maskMobile($mobile),
                    'expires_in' => OTPConfig::OTP_EXPIRY_MINUTES * 60,
                    'resend_in' => OTPConfig::RESEND_COOLDOWN_SECONDS,
                    'log_id' => $smsResult['log_id'] ?? null
                ];
            }
            
            // Development mode - return OTP in response
            return [
                'success' => true,
                'message' => 'OTP sent successfully (Development Mode)',
                'mobile' => $this->maskMobile($mobile),
                'expires_in' => OTPConfig::OTP_EXPIRY_MINUTES * 60,
                'resend_in' => OTPConfig::RESEND_COOLDOWN_SECONDS,
                'otp' => $otp, // Only in development mode
                'mode' => 'development'
            ];
            
        } catch (PDOException $e) {
            error_log("OTP Send Error: " . $e->getMessage());
            return [
                'success' => false,
                'message' => 'Failed to generate OTP. Please try again.',
                'error_code' => 'DATABASE_ERROR'
            ];
        }
    }
    
    /**
     * Resend OTP
     */
    public function resendOtp($mobile) {
        $mobile = OTPConfig::validateMobile($mobile);
        if (!$mobile) {
            return [
                'success' => false,
                'message' => 'Invalid mobile number.',
                'error_code' => 'INVALID_MOBILE'
            ];
        }
        
        // Check resend count
        $resendCount = $this->getResendCount($mobile);
        if ($resendCount >= OTPConfig::MAX_RESEND_COUNT) {
            return [
                'success' => false,
                'message' => 'Maximum resend limit reached. Please try again after some time.',
                'error_code' => 'RESEND_LIMIT_EXCEEDED'
            ];
        }
        
        // Increment resend count and send new OTP
        $this->incrementResendCount($mobile);
        return $this->sendOtp($mobile);
    }
    
    /**
     * Verify OTP
     * 
     * @param string $mobile - 10-digit mobile number
     * @param string $otp - 6-digit OTP
     * @return array - Response with success status
     */
    public function verifyOtp($mobile, $otp) {
        $mobile = OTPConfig::validateMobile($mobile);
        if (!$mobile) {
            return [
                'success' => false,
                'message' => 'Invalid mobile number.',
                'error_code' => 'INVALID_MOBILE'
            ];
        }
        
        if (empty($otp) || strlen($otp) != OTPConfig::OTP_LENGTH) {
            return [
                'success' => false,
                'message' => 'Invalid OTP format. Please enter ' . OTPConfig::OTP_LENGTH . '-digit OTP.',
                'error_code' => 'INVALID_OTP_FORMAT'
            ];
        }
        
        try {
            // Development mode bypass
            if (OTPConfig::isDevelopmentMode() && $otp == OTPConfig::TEST_OTP) {
                return [
                    'success' => true,
                    'message' => 'OTP verified successfully (Test Mode)',
                    'mobile' => $mobile,
                    'verified' => true
                ];
            }
            
            // Find valid OTP
            $query = "SELECT * FROM {$this->table} 
                      WHERE mobile = ? AND is_verified = 0 AND expires_at > NOW()
                      ORDER BY created_at DESC LIMIT 1";
            $stmt = $this->db->prepare($query);
            $stmt->execute([$mobile]);
            
            if ($stmt->rowCount() === 0) {
                return [
                    'success' => false,
                    'message' => 'No active OTP found. Please request a new OTP.',
                    'error_code' => 'NO_ACTIVE_OTP'
                ];
            }
            
            $otpRecord = $stmt->fetch(PDO::FETCH_ASSOC);
            
            // Check attempts
            if ($otpRecord['attempts'] >= OTPConfig::MAX_ATTEMPTS) {
                return [
                    'success' => false,
                    'message' => 'Too many failed attempts. Please request a new OTP.',
                    'error_code' => 'MAX_ATTEMPTS_EXCEEDED'
                ];
            }
            
            // Verify OTP
            if ($otpRecord['otp'] === $otp) {
                // Mark as verified
                $updateQuery = "UPDATE {$this->table} SET is_verified = 1, updated_at = NOW() WHERE id = ?";
                $updateStmt = $this->db->prepare($updateQuery);
                $updateStmt->execute([$otpRecord['id']]);
                
                return [
                    'success' => true,
                    'message' => 'OTP verified successfully',
                    'mobile' => $mobile,
                    'verified' => true
                ];
            } else {
                // Increment attempts
                $updateQuery = "UPDATE {$this->table} SET attempts = attempts + 1, updated_at = NOW() WHERE id = ?";
                $updateStmt = $this->db->prepare($updateQuery);
                $updateStmt->execute([$otpRecord['id']]);
                
                $remainingAttempts = OTPConfig::MAX_ATTEMPTS - ($otpRecord['attempts'] + 1);
                
                return [
                    'success' => false,
                    'message' => "Invalid OTP. {$remainingAttempts} attempts remaining.",
                    'error_code' => 'INVALID_OTP',
                    'remaining_attempts' => $remainingAttempts
                ];
            }
            
        } catch (PDOException $e) {
            error_log("OTP Verify Error: " . $e->getMessage());
            return [
                'success' => false,
                'message' => 'Verification failed. Please try again.',
                'error_code' => 'DATABASE_ERROR'
            ];
        }
    }
    
    /**
     * Send SMS via AuthKey.io API
     */
    private function sendSmsViaAuthKey($mobile, $otp) {
        $url = OTPConfig::getApiUrl($mobile, $otp);
        
        // Use cURL to make the API call
        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_TIMEOUT => 30,
            CURLOPT_HTTPHEADER => [
                'Accept: application/json'
            ]
        ]);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);
        
        if ($error) {
            return [
                'success' => false,
                'message' => 'SMS API connection failed',
                'error' => $error
            ];
        }
        
        $result = json_decode($response, true);
        
        // AuthKey.io returns {"LogID": "...", "Message": "Submitted Successfully"}
        if ($httpCode == 200 && isset($result['Message']) && 
            strpos(strtolower($result['Message']), 'success') !== false) {
            return [
                'success' => true,
                'message' => 'SMS sent successfully',
                'log_id' => $result['LogID'] ?? null
            ];
        }
        
        return [
            'success' => false,
            'message' => $result['Message'] ?? 'SMS sending failed',
            'response' => $result
        ];
    }
    
    /**
     * Check Rate Limiting
     */
    private function checkRateLimit($mobile) {
        $oneHourAgo = date('Y-m-d H:i:s', strtotime('-1 hour'));
        $ipAddress = $this->getClientIP();
        
        // Check per-mobile rate limit
        $query = "SELECT COUNT(*) as count FROM {$this->table} 
                  WHERE mobile = ? AND created_at > ?";
        $stmt = $this->db->prepare($query);
        $stmt->execute([$mobile, $oneHourAgo]);
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($result['count'] >= OTPConfig::RATE_LIMIT_PER_MOBILE) {
            return [
                'allowed' => false,
                'message' => 'Too many OTP requests. Please try again after 1 hour.',
                'retry_after' => 3600
            ];
        }
        
        // Check per-IP rate limit
        $query = "SELECT COUNT(*) as count FROM {$this->table} 
                  WHERE ip_address = ? AND created_at > ?";
        $stmt = $this->db->prepare($query);
        $stmt->execute([$ipAddress, $oneHourAgo]);
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($result['count'] >= OTPConfig::RATE_LIMIT_PER_IP) {
            return [
                'allowed' => false,
                'message' => 'Too many requests from your network. Please try again later.',
                'retry_after' => 3600
            ];
        }
        
        return ['allowed' => true];
    }
    
    /**
     * Check Resend Cooldown
     */
    private function checkResendCooldown($mobile) {
        $cooldownTime = date('Y-m-d H:i:s', time() - OTPConfig::RESEND_COOLDOWN_SECONDS);
        
        $query = "SELECT created_at FROM {$this->table} 
                  WHERE mobile = ? AND created_at > ? 
                  ORDER BY created_at DESC LIMIT 1";
        $stmt = $this->db->prepare($query);
        $stmt->execute([$mobile, $cooldownTime]);
        
        if ($stmt->rowCount() > 0) {
            $lastOtp = $stmt->fetch(PDO::FETCH_ASSOC);
            $lastSentTime = strtotime($lastOtp['created_at']);
            $retryAfter = OTPConfig::RESEND_COOLDOWN_SECONDS - (time() - $lastSentTime);
            
            return [
                'allowed' => false,
                'message' => "Please wait {$retryAfter} seconds before requesting a new OTP.",
                'retry_after' => $retryAfter
            ];
        }
        
        return ['allowed' => true];
    }
    
    /**
     * Get Resend Count for Mobile
     */
    private function getResendCount($mobile) {
        $query = "SELECT MAX(resend_count) as max_resend FROM {$this->table} 
                  WHERE mobile = ? AND DATE(created_at) = CURDATE()";
        $stmt = $this->db->prepare($query);
        $stmt->execute([$mobile]);
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        return $result['max_resend'] ?? 0;
    }
    
    /**
     * Increment Resend Count
     */
    private function incrementResendCount($mobile) {
        // This is handled in sendOtp method
    }
    
    /**
     * Cleanup Old OTPs
     */
    private function cleanupOldOtps($mobile) {
        $query = "DELETE FROM {$this->table} WHERE mobile = ? AND is_verified = 0";
        $stmt = $this->db->prepare($query);
        $stmt->execute([$mobile]);
    }
    
    /**
     * Mask Mobile Number for Display
     */
    private function maskMobile($mobile) {
        return substr($mobile, 0, 2) . '****' . substr($mobile, -4);
    }
    
    /**
     * Get Client IP Address
     */
    private function getClientIP() {
        $headers = ['HTTP_CLIENT_IP', 'HTTP_X_FORWARDED_FOR', 'HTTP_X_FORWARDED', 
                    'HTTP_X_CLUSTER_CLIENT_IP', 'HTTP_FORWARDED_FOR', 'HTTP_FORWARDED', 'REMOTE_ADDR'];
        
        foreach ($headers as $header) {
            if (!empty($_SERVER[$header])) {
                $ip = $_SERVER[$header];
                if (strpos($ip, ',') !== false) {
                    $ip = trim(explode(',', $ip)[0]);
                }
                if (filter_var($ip, FILTER_VALIDATE_IP)) {
                    return $ip;
                }
            }
        }
        
        return $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
    }
    
    /**
     * Get OTP Statistics (for admin)
     */
    public function getStats() {
        $today = date('Y-m-d');
        
        $query = "SELECT 
                    COUNT(*) as total_requests,
                    SUM(CASE WHEN is_verified = 1 THEN 1 ELSE 0 END) as verified_count,
                    SUM(CASE WHEN expires_at < NOW() AND is_verified = 0 THEN 1 ELSE 0 END) as expired_count,
                    COUNT(DISTINCT mobile) as unique_mobiles
                  FROM {$this->table} 
                  WHERE DATE(created_at) = ?";
        $stmt = $this->db->prepare($query);
        $stmt->execute([$today]);
        
        return $stmt->fetch(PDO::FETCH_ASSOC);
    }
}
?>

