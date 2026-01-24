<?php
/**
 * Privacy Policy API Endpoint
 * Returns privacy policy content for app store listings
 * Accessible at: https://sudartnpscapp.in/api/privacy-policy.php
 */

require_once __DIR__ . '/config/cors.php';
require_once __DIR__ . '/config/database.php';

header('Content-Type: application/json; charset=utf-8');

try {
    $database = new Database();
    $db = $database->getConnection();
    
    // Get privacy policy from settings
    $stmt = $db->prepare("SELECT setting_value, setting_type FROM app_settings WHERE setting_key = 'privacy_policy' LIMIT 1");
    $stmt->execute();
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    $privacyPolicy = '';
    if ($result && !empty($result['setting_value'])) {
        $value = $result['setting_value'];
        
        // If it's JSON type, decode it
        if ($result['setting_type'] === 'json') {
            $decoded = json_decode($value, true);
            $privacyPolicy = is_array($decoded) ? ($decoded['text'] ?? $value) : $value;
        } else {
            $privacyPolicy = $value;
        }
    }
    
    // If no privacy policy in database, return default
    if (empty($privacyPolicy)) {
        $privacyPolicy = getDefaultPrivacyPolicy();
    }
    
    // Return JSON response
    echo json_encode([
        'success' => true,
        'privacy_policy' => $privacyPolicy,
        'last_updated' => date('Y-m-d'),
        'app_name' => 'SUDAR - TNPSC Mock Test',
        'contact_email' => 'support@sudartnpscapp.in',
        'website' => 'https://sudartnpscapp.in'
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error retrieving privacy policy',
        'privacy_policy' => getDefaultPrivacyPolicy(),
        'last_updated' => date('Y-m-d')
    ], JSON_PRETTY_PRINT);
}

function getDefaultPrivacyPolicy() {
    return <<<POLICY
PRIVACY POLICY - SUDAR TNPSC MOCK TEST

Last Updated: January 2025

1. INFORMATION WE COLLECT
We collect mobile numbers for account creation and OTP verification. We also collect test results, performance data, and usage patterns to improve our services.

2. HOW WE USE YOUR INFORMATION
We use your information to provide, maintain, and improve our services, authenticate your identity, track test performance, generate rankings, and provide customer support.

3. DATA STORAGE AND SECURITY
Your data is stored on secure servers with appropriate security measures to protect against unauthorized access.

4. INFORMATION SHARING
We do not sell, trade, or rent your personal information. We may share information only with trusted service providers or when required by law.

5. USER RIGHTS
You have the right to access, correct, delete, or request a copy of your personal data. Contact us to exercise these rights.

6. THIRD-PARTY SERVICES
Our App may contain links to third-party services. We are not responsible for their privacy practices.

7. CHILDREN'S PRIVACY
Our App is not intended for children under 13. We do not knowingly collect information from children under 13.

8. CHANGES TO PRIVACY POLICY
We may update this Privacy Policy from time to time. Changes will be posted on this page with an updated date.

9. CONTACT US
For questions about this Privacy Policy, contact us at:
Email: support@sudartnpscapp.in
Website: https://sudartnpscapp.in

By using our App, you consent to this Privacy Policy.
POLICY;
}
?>

