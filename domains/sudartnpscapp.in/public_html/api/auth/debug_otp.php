<?php
/**
 * Debug OTP - Find the actual error
 */
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

error_reporting(E_ALL);
ini_set('display_errors', 0);

$errors = [];
$checks = [];

// Check 1: Config files exist
$configFiles = [
    '../config/cors.php' => 'CORS Config',
    '../config/database.php' => 'Database Config', 
    '../config/otp.php' => 'OTP Config',
    '../services/OtpService.php' => 'OTP Service'
];

foreach ($configFiles as $file => $name) {
    $fullPath = __DIR__ . '/' . $file;
    if (file_exists($fullPath)) {
        $checks[$name] = '✅ EXISTS';
    } else {
        $checks[$name] = '❌ MISSING: ' . $fullPath;
        $errors[] = "$name is missing";
    }
}

// Check 2: Try to include files
try {
    if (file_exists(__DIR__ . '/../config/cors.php')) {
        require_once '../config/cors.php';
        $checks['CORS Include'] = '✅ OK';
    }
} catch (Exception $e) {
    $checks['CORS Include'] = '❌ ERROR: ' . $e->getMessage();
    $errors[] = 'CORS: ' . $e->getMessage();
}

try {
    if (file_exists(__DIR__ . '/../config/database.php')) {
        require_once '../config/database.php';
        $checks['Database Include'] = '✅ OK';
    }
} catch (Exception $e) {
    $checks['Database Include'] = '❌ ERROR: ' . $e->getMessage();
    $errors[] = 'Database: ' . $e->getMessage();
}

try {
    if (file_exists(__DIR__ . '/../config/otp.php')) {
        require_once '../config/otp.php';
        $checks['OTP Config Include'] = '✅ OK';
        
        // Check OTPConfig class
        if (class_exists('OTPConfig')) {
            $checks['OTPConfig Class'] = '✅ EXISTS';
            $checks['AUTH_KEY'] = OTPConfig::AUTH_KEY ? '✅ SET' : '❌ EMPTY';
            $checks['ENV_MODE'] = OTPConfig::ENV_MODE;
        } else {
            $checks['OTPConfig Class'] = '❌ NOT FOUND';
            $errors[] = 'OTPConfig class not found';
        }
    }
} catch (Exception $e) {
    $checks['OTP Config Include'] = '❌ ERROR: ' . $e->getMessage();
    $errors[] = 'OTP Config: ' . $e->getMessage();
}

try {
    if (file_exists(__DIR__ . '/../services/OtpService.php')) {
        require_once '../services/OtpService.php';
        $checks['OTP Service Include'] = '✅ OK';
        
        if (class_exists('OtpService')) {
            $checks['OtpService Class'] = '✅ EXISTS';
        } else {
            $checks['OtpService Class'] = '❌ NOT FOUND';
            $errors[] = 'OtpService class not found';
        }
    }
} catch (Exception $e) {
    $checks['OTP Service Include'] = '❌ ERROR: ' . $e->getMessage();
    $errors[] = 'OTP Service: ' . $e->getMessage();
}

// Check 3: Database connection
try {
    if (class_exists('Database')) {
        $database = new Database();
        $db = $database->getConnection();
        if ($db) {
            $checks['Database Connection'] = '✅ CONNECTED';
            
            // Check otp_verifications table
            try {
                $stmt = $db->query("SHOW TABLES LIKE 'otp_verifications'");
                if ($stmt->rowCount() > 0) {
                    $checks['OTP Table'] = '✅ EXISTS';
                } else {
                    $checks['OTP Table'] = '⚠️ Will be created on first use';
                }
            } catch (Exception $e) {
                $checks['OTP Table Check'] = '❌ ERROR: ' . $e->getMessage();
            }
        } else {
            $checks['Database Connection'] = '❌ FAILED';
            $errors[] = 'Database connection failed';
        }
    }
} catch (Exception $e) {
    $checks['Database Connection'] = '❌ ERROR: ' . $e->getMessage();
    $errors[] = 'DB Connection: ' . $e->getMessage();
}

// Check 4: cURL available
$checks['cURL'] = function_exists('curl_init') ? '✅ AVAILABLE' : '❌ NOT AVAILABLE';
if (!function_exists('curl_init')) {
    $errors[] = 'cURL is not available';
}

// Check 5: Try OTP Service
if (empty($errors) && class_exists('OtpService') && isset($db) && $db) {
    try {
        $otpService = new OtpService($db);
        $checks['OTP Service Init'] = '✅ OK';
        
        // Try a test (dry run)
        $checks['Ready to Send OTP'] = '✅ YES';
    } catch (Exception $e) {
        $checks['OTP Service Init'] = '❌ ERROR: ' . $e->getMessage();
        $errors[] = 'OTP Service Init: ' . $e->getMessage();
    }
}

// Output result
echo json_encode([
    'success' => empty($errors),
    'message' => empty($errors) ? 'All checks passed! OTP system is ready.' : 'Found ' . count($errors) . ' error(s)',
    'checks' => $checks,
    'errors' => $errors,
    'php_version' => PHP_VERSION,
    'server' => $_SERVER['SERVER_SOFTWARE'] ?? 'Unknown'
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
?>

