<?php
/**
 * One-time script to update WhatsApp support number in About settings
 * Run this once, then delete the file.
 * 
 * Access via: http://localhost/MockTest/update_whatsapp_number.php
 */

require_once 'api/config/database.php';

$database = new Database();
$db = $database->getConnection();

// The new WhatsApp number to set
$newPhoneNumber = '+91 83003 21814';

try {
    // First, check if about setting exists
    $query = "SELECT setting_value FROM app_settings WHERE setting_key = 'about'";
    $stmt = $db->prepare($query);
    $stmt->execute();
    $existing = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($existing) {
        // Parse existing JSON and update contact_phone
        $aboutData = json_decode($existing['setting_value'], true);
        if (!is_array($aboutData)) {
            $aboutData = [];
        }
        $oldPhone = $aboutData['contact_phone'] ?? '(not set)';
        $aboutData['contact_phone'] = $newPhoneNumber;
        
        // Update the setting
        $updateQuery = "UPDATE app_settings SET setting_value = ?, setting_type = 'json' WHERE setting_key = 'about'";
        $updateStmt = $db->prepare($updateQuery);
        $updateStmt->execute([json_encode($aboutData)]);
        
        echo "<h2>✅ WhatsApp Number Updated Successfully!</h2>";
        echo "<p><strong>Old number:</strong> " . htmlspecialchars($oldPhone) . "</p>";
        echo "<p><strong>New number:</strong> " . htmlspecialchars($newPhoneNumber) . "</p>";
        echo "<p><strong>Full about data:</strong></p>";
        echo "<pre>" . htmlspecialchars(json_encode($aboutData, JSON_PRETTY_PRINT)) . "</pre>";
    } else {
        // Create new about setting
        $aboutData = [
            'app_name' => 'TNPSC Mock Test',
            'app_version' => '1.0.0',
            'description' => 'Prepare for TNPSC exams with comprehensive mock tests and detailed analytics.',
            'contact_email' => '',
            'contact_phone' => $newPhoneNumber
        ];
        
        $insertQuery = "INSERT INTO app_settings (setting_key, setting_value, setting_type, description) VALUES ('about', ?, 'json', 'About information for the mobile app')";
        $insertStmt = $db->prepare($insertQuery);
        $insertStmt->execute([json_encode($aboutData)]);
        
        echo "<h2>✅ About Setting Created with WhatsApp Number!</h2>";
        echo "<p><strong>Phone number:</strong> " . htmlspecialchars($newPhoneNumber) . "</p>";
        echo "<p><strong>Full about data:</strong></p>";
        echo "<pre>" . htmlspecialchars(json_encode($aboutData, JSON_PRETTY_PRINT)) . "</pre>";
    }
    
    echo "<hr>";
    echo "<p style='color: green;'><strong>Done!</strong> You can now delete this file (update_whatsapp_number.php).</p>";
    echo "<p>Verify by visiting: <a href='api/settings/get_public.php?key=about' target='_blank'>api/settings/get_public.php?key=about</a></p>";
    
} catch (PDOException $e) {
    echo "<h2>❌ Error</h2>";
    echo "<p style='color: red;'>" . htmlspecialchars($e->getMessage()) . "</p>";
}
?>
