<?php
/**
 * Debug endpoint to check OTP environment mode
 */
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

require_once __DIR__ . '/../config/otp.php';

$response = [
    'env_mode' => OTPConfig::getEnvMode(),
    'is_development' => OTPConfig::isDevelopmentMode(),
    'test_otp' => OTPConfig::isDevelopmentMode() ? OTPConfig::TEST_OTP : '(hidden in production)',
    'http_host' => $_SERVER['HTTP_HOST'] ?? 'not set',
    'server_name' => $_SERVER['SERVER_NAME'] ?? 'not set',
    'remote_addr' => $_SERVER['REMOTE_ADDR'] ?? 'not set',
    'config_env_mode' => OTPConfig::ENV_MODE,
    'message' => OTPConfig::isDevelopmentMode() 
        ? 'Development mode active! Use OTP: ' . OTPConfig::TEST_OTP 
        : 'Production mode - real SMS OTP required'
];

echo json_encode($response, JSON_PRETTY_PRINT);
?>

