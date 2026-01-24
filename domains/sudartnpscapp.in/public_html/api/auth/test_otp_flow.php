<?php
require_once '../config/cors.php';
require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

// Test mobile number
$mobile = '9384938434';

echo "<h2>OTP Flow Test for Mobile: $mobile</h2>";
echo "<hr>";

// Step 1: Generate OTP
echo "<h3>Step 1: Generate OTP</h3>";
$otp = rand(100000, 999999);
$expires_at = date('Y-m-d H:i:s', strtotime('+10 minutes'));

try {
    // Clear old OTPs
    $delete_query = "DELETE FROM otp_verifications WHERE mobile = ?";
    $delete_stmt = $db->prepare($delete_query);
    $delete_stmt->execute([$mobile]);
    echo "✅ Cleared old OTPs<br>";
    
    // Insert new OTP
    $insert_query = "INSERT INTO otp_verifications (mobile, otp, expires_at) VALUES (?, ?, ?)";
    $insert_stmt = $db->prepare($insert_query);
    $insert_stmt->execute([$mobile, $otp, $expires_at]);
    echo "✅ Generated new OTP: <strong style='color:green; font-size:24px'>$otp</strong><br>";
    echo "⏰ Expires at: $expires_at<br>";
    
    // Step 2: Verify what's in database
    echo "<br><h3>Step 2: Check Database</h3>";
    $check_query = "SELECT * FROM otp_verifications WHERE mobile = ? ORDER BY created_at DESC";
    $check_stmt = $db->prepare($check_query);
    $check_stmt->execute([$mobile]);
    
    if ($check_stmt->rowCount() > 0) {
        echo "<table border='1' cellpadding='5'>";
        echo "<tr><th>ID</th><th>Mobile</th><th>OTP</th><th>Is Verified</th><th>Attempts</th><th>Expires At</th></tr>";
        while ($row = $check_stmt->fetch(PDO::FETCH_ASSOC)) {
            echo "<tr>";
            echo "<td>" . $row['id'] . "</td>";
            echo "<td>" . $row['mobile'] . "</td>";
            echo "<td><strong>" . $row['otp'] . "</strong></td>";
            echo "<td>" . $row['is_verified'] . "</td>";
            echo "<td>" . $row['attempts'] . "</td>";
            echo "<td>" . $row['expires_at'] . "</td>";
            echo "</tr>";
        }
        echo "</table>";
    }
    
    // Step 3: Test verification
    echo "<br><h3>Step 3: Test Verification</h3>";
    $verify_query = "SELECT * FROM otp_verifications
                     WHERE mobile = ? AND otp = ? AND expires_at > NOW() AND is_verified = 0
                     ORDER BY created_at DESC LIMIT 1";
    $verify_stmt = $db->prepare($verify_query);
    $verify_stmt->execute([$mobile, $otp]);
    
    if ($verify_stmt->rowCount() > 0) {
        echo "✅ <strong style='color:green'>OTP verification would SUCCEED!</strong><br>";
        echo "✅ Mobile: $mobile<br>";
        echo "✅ OTP: <strong style='font-size:24px; color:green'>$otp</strong><br>";
    } else {
        echo "❌ OTP verification would FAIL<br>";
    }
    
    echo "<br><hr>";
    echo "<h3>🎯 Use This OTP in Your App:</h3>";
    echo "<div style='background:#4CAF50; color:white; padding:20px; font-size:32px; text-align:center; border-radius:10px;'>";
    echo "$otp";
    echo "</div>";
    echo "<p>Mobile: $mobile</p>";
    
} catch (PDOException $e) {
    echo "❌ Error: " . $e->getMessage();
}
?>

