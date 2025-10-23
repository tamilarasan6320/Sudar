<?php
header('Content-Type: text/html; charset=utf-8');
require_once 'api/config/database.php';

$database = new Database();
$db = $database->getConnection();

$mobile = '9384938434';

echo "<html><head>";
echo "<style>
body { font-family: Arial; margin: 20px; background: #f5f5f5; }
.box { background: white; padding: 20px; margin: 20px 0; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
.otp { font-size: 48px; color: #4CAF50; font-weight: bold; letter-spacing: 5px; }
.error { color: #f44336; }
.success { color: #4CAF50; }
table { width: 100%; border-collapse: collapse; margin: 20px 0; }
th, td { padding: 10px; border: 1px solid #ddd; text-align: left; }
th { background: #667eea; color: white; }
button { background: #4CAF50; color: white; border: none; padding: 15px 30px; font-size: 18px; border-radius: 5px; cursor: pointer; margin: 10px 5px; }
button:hover { background: #45a049; }
</style>";
echo "</head><body>";

echo "<h1>🔍 OTP Diagnostic Tool</h1>";

// Step 1: Check database connection
echo "<div class='box'>";
echo "<h2>1️⃣ Database Connection</h2>";
if ($db) {
    echo "<p class='success'>✅ Database connected successfully!</p>";
} else {
    echo "<p class='error'>❌ Database connection failed!</p>";
    exit;
}
echo "</div>";

// Step 2: Check if table exists
echo "<div class='box'>";
echo "<h2>2️⃣ Check otp_verifications Table</h2>";
try {
    $check_table = "SHOW TABLES LIKE 'otp_verifications'";
    $stmt = $db->query($check_table);
    if ($stmt->rowCount() > 0) {
        echo "<p class='success'>✅ Table 'otp_verifications' exists!</p>";
    } else {
        echo "<p class='error'>❌ Table 'otp_verifications' does NOT exist!</p>";
        echo "<p>Creating table now...</p>";
        
        $create_table = "CREATE TABLE IF NOT EXISTS otp_verifications (
            id INT AUTO_INCREMENT PRIMARY KEY,
            mobile VARCHAR(15) NOT NULL,
            otp VARCHAR(6) NOT NULL,
            is_verified TINYINT(1) DEFAULT 0,
            attempts INT DEFAULT 0,
            expires_at DATETIME NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_mobile (mobile),
            INDEX idx_otp (otp),
            INDEX idx_expires (expires_at)
        )";
        $db->exec($create_table);
        echo "<p class='success'>✅ Table created!</p>";
    }
} catch (Exception $e) {
    echo "<p class='error'>❌ Error: " . $e->getMessage() . "</p>";
}
echo "</div>";

// Step 3: Generate fresh OTP
echo "<div class='box'>";
echo "<h2>3️⃣ Generate Fresh OTP</h2>";
$otp = rand(100000, 999999);
// Fix timezone - use current timestamp + 10 minutes
$expires_at = date('Y-m-d H:i:s', time() + (10 * 60));

try {
    // Delete old OTPs
    $delete = "DELETE FROM otp_verifications WHERE mobile = ?";
    $stmt = $db->prepare($delete);
    $stmt->execute([$mobile]);
    echo "<p>🗑️ Cleared old OTPs</p>";
    
    // Insert new OTP
    $insert = "INSERT INTO otp_verifications (mobile, otp, expires_at) VALUES (?, ?, ?)";
    $stmt = $db->prepare($insert);
    $stmt->execute([$mobile, $otp, $expires_at]);
    
    echo "<p class='success'>✅ New OTP generated and stored!</p>";
    echo "<p><strong>Mobile:</strong> $mobile</p>";
    echo "<p><strong>OTP:</strong> <span class='otp'>$otp</span></p>";
    echo "<p><strong>Expires at:</strong> $expires_at</p>";
    
} catch (Exception $e) {
    echo "<p class='error'>❌ Error: " . $e->getMessage() . "</p>";
}
echo "</div>";

// Step 4: Show what's in database
echo "<div class='box'>";
echo "<h2>4️⃣ Current OTPs in Database</h2>";
try {
    $query = "SELECT * FROM otp_verifications WHERE mobile = ? ORDER BY created_at DESC LIMIT 5";
    $stmt = $db->prepare($query);
    $stmt->execute([$mobile]);
    
    if ($stmt->rowCount() > 0) {
        echo "<table>";
        echo "<tr><th>ID</th><th>Mobile</th><th>OTP</th><th>Verified</th><th>Attempts</th><th>Created</th><th>Expires</th></tr>";
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            echo "<tr>";
            echo "<td>" . $row['id'] . "</td>";
            echo "<td>" . $row['mobile'] . "</td>";
            echo "<td><strong>" . $row['otp'] . "</strong></td>";
            echo "<td>" . ($row['is_verified'] ? '✅' : '❌') . "</td>";
            echo "<td>" . $row['attempts'] . "</td>";
            echo "<td>" . $row['created_at'] . "</td>";
            echo "<td>" . $row['expires_at'] . "</td>";
            echo "</tr>";
        }
        echo "</table>";
    } else {
        echo "<p>No OTPs found in database!</p>";
    }
} catch (Exception $e) {
    echo "<p class='error'>❌ Error: " . $e->getMessage() . "</p>";
}
echo "</div>";

// Step 5: Test verification
echo "<div class='box'>";
echo "<h2>5️⃣ Test OTP Verification</h2>";
try {
    $verify_query = "SELECT * FROM otp_verifications 
                     WHERE mobile = ? AND otp = ? AND expires_at > NOW() AND is_verified = 0 
                     ORDER BY created_at DESC LIMIT 1";
    $stmt = $db->prepare($verify_query);
    $stmt->execute([$mobile, $otp]);
    
    if ($stmt->rowCount() > 0) {
        echo "<p class='success'>✅ OTP VERIFICATION WILL WORK!</p>";
        echo "<p><strong>Mobile:</strong> $mobile</p>";
        echo "<p><strong>Use this OTP:</strong> <span class='otp'>$otp</span></p>";
    } else {
        echo "<p class='error'>❌ OTP verification will FAIL!</p>";
        echo "<p>Possible reasons:</p>";
        echo "<ul>";
        echo "<li>OTP expired</li>";
        echo "<li>OTP already used (is_verified = 1)</li>";
        echo "<li>OTP not in database</li>";
        echo "</ul>";
    }
} catch (Exception $e) {
    echo "<p class='error'>❌ Error: " . $e->getMessage() . "</p>";
}
echo "</div>";

// Step 6: Instructions
echo "<div class='box' style='background: #4CAF50; color: white;'>";
echo "<h2>🎯 USE THIS OTP NOW!</h2>";
echo "<div style='text-align: center;'>";
echo "<div class='otp' style='color: white; font-size: 64px;'>$otp</div>";
echo "<p style='font-size: 20px;'>Mobile: <strong>$mobile</strong></p>";
echo "<p>Valid for 10 minutes</p>";
echo "</div>";
echo "</div>";

echo "<div class='box'>";
echo "<h2>📱 How to Use:</h2>";
echo "<ol style='line-height: 2;'>";
echo "<li>Copy the OTP above: <strong>$otp</strong></li>";
echo "<li>Go to your Flutter app: <a href='http://localhost:8888' target='_blank'>http://localhost:8888</a></li>";
echo "<li>Enter mobile: <strong>$mobile</strong> (without +91)</li>";
echo "<li>Click CONTINUE</li>";
echo "<li>Enter OTP: <strong>$otp</strong></li>";
echo "<li>Click SUBMIT</li>";
echo "<li>✅ SUCCESS!</li>";
echo "</ol>";
echo "</div>";

echo "<div class='box'>";
echo "<button onclick='location.reload()'>🔄 Generate New OTP</button>";
echo "<button onclick=\"window.open('http://localhost:8888', '_blank')\">🚀 Open Flutter App</button>";
echo "</div>";

echo "</body></html>";
?>

