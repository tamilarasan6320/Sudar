<?php
/**
 * OTP Configuration - AuthKey.io
 * ==============================
 * Professional SMS OTP Service Configuration
 * 
 * API Documentation: https://authkey.io/docs
 * 
 * ═══════════════════════════════════════════════════════════════════
 * SMS RETRIEVER - IMPORTANT SETUP
 * ═══════════════════════════════════════════════════════════════════
 * 
 * For Android SMS Retriever (auto-read OTP) to work, your SMS template
 * MUST include the app signature hash at the END of the message.
 * 
 * Required SMS Format:
 *   <#> Your OTP is {OTP}
 *   {APP_HASH}
 * 
 * Example:
 *   <#> Your OTP is 123456
 *   abc123XYZ99
 * 
 * How to get APP_HASH:
 *   1. Run the Flutter app in DEBUG mode on Android
 *   2. Go to OTP Verification screen
 *   3. Check console logs for "App signature for SMS Retriever: XXXXXXXXXXX"
 *   4. Copy that 11-character hash
 * 
 * IMPORTANT NOTES:
 *   - The <#> at the start is REQUIRED
 *   - The hash MUST be on its own line at the END
 *   - DEBUG and RELEASE builds have DIFFERENT hashes!
 *   - For production, use the RELEASE build hash
 *   - Update your AuthKey.io SMS template (SID) accordingly
 * 
 * ═══════════════════════════════════════════════════════════════════
 */

class OTPConfig {
    // ========================================
    // AuthKey.io API Credentials
    // ========================================
    const AUTH_KEY = 'dc0b07c812ca4934';
    const SENDER_ID = '25451';  // SID for your registered template
    const COUNTRY_CODE = '91';
    
    // ========================================
    // API Endpoint
    // ========================================
    const API_BASE_URL = 'https://api.authkey.io/request';
    
    // ========================================
    // OTP Settings
    // ========================================
    const OTP_LENGTH = 6;
    const OTP_EXPIRY_MINUTES = 10;
    const MAX_ATTEMPTS = 5;
    const RESEND_COOLDOWN_SECONDS = 60;
    const MAX_RESEND_COUNT = 3;
    
    // ========================================
    // Environment Mode
    // ========================================
    // 'auto' = development on localhost, production otherwise
    // 'development' = skip SMS (OTP returned in response) + allow TEST_OTP
    // 'production' = send real SMS via AuthKey.io
    const ENV_MODE = 'production'; // 'auto' | 'development' | 'production'
    
    // Test OTP for development mode (only works when ENV_MODE = 'development')
    const TEST_OTP = '111111';
    
    // ========================================
    // Rate Limiting
    // ========================================
    const RATE_LIMIT_PER_IP = 10;      // Max OTP requests per IP per hour
    const RATE_LIMIT_PER_MOBILE = 5;   // Max OTP requests per mobile per hour
    
    /**
     * Generate OTP API URL
     */
    public static function getApiUrl($mobile, $otp) {
        $params = [
            'authkey' => self::AUTH_KEY,
            'mobile' => $mobile,
            'country_code' => self::COUNTRY_CODE,
            'sid' => self::SENDER_ID,
            'otp' => $otp
        ];
        
        return self::API_BASE_URL . '?' . http_build_query($params);
    }
    
    /**
     * Generate Random OTP
     */
    public static function generateOTP() {
        $min = pow(10, self::OTP_LENGTH - 1);
        $max = pow(10, self::OTP_LENGTH) - 1;
        return strval(rand($min, $max));
    }
    
    /**
     * Get OTP Expiry DateTime
     */
    public static function getExpiryTime() {
        return date('Y-m-d H:i:s', time() + (self::OTP_EXPIRY_MINUTES * 60));
    }
    
    /**
     * Resolve Environment Mode
     *
     * Priority:
     * 1) Environment variable OTP_ENV_MODE (development|production|dev|prod)
     * 2) ENV_MODE constant (development|production|auto)
     * 3) Auto-detect localhost => development
     */
    public static function getEnvMode() {
        // Allow overriding via environment variable (Apache SetEnv / system env)
        $env = getenv('OTP_ENV_MODE');
        if ($env !== false && $env !== '') {
            $env = strtolower(trim($env));
            if ($env === 'dev') $env = 'development';
            if ($env === 'prod') $env = 'production';

            if ($env === 'development' || $env === 'production') {
                return $env;
            }
        }

        $mode = strtolower(trim(self::ENV_MODE));
        if ($mode === 'development' || $mode === 'production') {
            return $mode;
        }

        // Auto mode
        return self::isLocalhost() ? 'development' : 'production';
    }

    /**
     * Check if in Development Mode
     */
    public static function isDevelopmentMode() {
        return self::getEnvMode() === 'development';
    }

    /**
     * Detect localhost requests (safe default for dev mode)
     */
    private static function isLocalhost() {
        $host = '';
        if (!empty($_SERVER['HTTP_HOST'])) {
            $host = $_SERVER['HTTP_HOST'];
        } elseif (!empty($_SERVER['SERVER_NAME'])) {
            $host = $_SERVER['SERVER_NAME'];
        }

        $host = strtolower(trim((string)$host));
        if ($host === '') {
            return false;
        }

        // Strip port if present (e.g. localhost:8888)
        // Keep IPv6 hosts intact (e.g. ::1) unless bracketed [::1]:port
        if (strpos($host, '[') === 0) {
            $end = strpos($host, ']');
            if ($end !== false) {
                $host = substr($host, 1, $end - 1);
            }
        } elseif (substr_count($host, ':') <= 1) {
            $host = explode(':', $host)[0];
        }

        return in_array($host, ['localhost', '127.0.0.1', '::1'], true);
    }
    
    /**
     * Validate Mobile Number
     */
    public static function validateMobile($mobile) {
        // Remove any spaces, dashes, or country code prefix
        $mobile = preg_replace('/[\s\-\+]/', '', $mobile);
        
        // Remove country code if present
        if (strpos($mobile, '91') === 0 && strlen($mobile) > 10) {
            $mobile = substr($mobile, 2);
        }
        
        // Validate 10-digit Indian mobile number
        if (preg_match('/^[6-9][0-9]{9}$/', $mobile)) {
            return $mobile;
        }
        
        return false;
    }
}
?>

