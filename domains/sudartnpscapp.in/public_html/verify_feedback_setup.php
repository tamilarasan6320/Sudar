<?php
/**
 * Feedback System Verification Script
 * Run this to verify all files are in place and system is ready
 */

echo "<!DOCTYPE html>
<html>
<head>
    <title>Feedback System Verification</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 800px; margin: 50px auto; padding: 20px; }
        .success { color: #4CAF50; }
        .error { color: #f44336; }
        .warning { color: #FF9800; }
        .info { color: #2196F3; }
        h1 { color: #333; }
        .check-item { margin: 10px 0; padding: 10px; background: #f5f5f5; border-radius: 5px; }
        .summary { margin-top: 30px; padding: 20px; background: #e3f2fd; border-radius: 5px; }
    </style>
</head>
<body>
    <h1>🔍 Feedback System Verification</h1>";

$checks = [];
$all_passed = true;

// Check 1: API Files
echo "<h2>1. Checking API Files</h2>";
$api_files = [
    'api/admin/feedback/crud.php' => 'Admin Feedback API',
    'api/feedback/submit.php' => 'Public Feedback API',
    'api/install/create_feedback_table.php' => 'Table Creation Script',
    'api/install/setup_feedback.php' => 'Setup Script'
];

foreach ($api_files as $file => $description) {
    if (file_exists($file)) {
        echo "<div class='check-item'><span class='success'>✓</span> {$description} - <strong>Found</strong></div>";
        $checks[] = ['type' => 'file', 'name' => $file, 'status' => 'ok'];
    } else {
        echo "<div class='check-item'><span class='error'>✗</span> {$description} - <strong>NOT FOUND</strong> ({$file})</div>";
        $checks[] = ['type' => 'file', 'name' => $file, 'status' => 'missing'];
        $all_passed = false;
    }
}

// Check 2: Admin Panel Files
echo "<h2>2. Checking Admin Panel Files</h2>";
$admin_files = [
    'admin/index.html' => 'Admin Panel HTML',
    'admin/js/script.js' => 'Admin JavaScript'
];

foreach ($admin_files as $file => $description) {
    if (file_exists($file)) {
        // Check if feedback code exists
        $content = file_get_contents($file);
        if (strpos($content, 'feedback') !== false || strpos($content, 'Feedback') !== false) {
            echo "<div class='check-item'><span class='success'>✓</span> {$description} - <strong>Found & Updated</strong></div>";
            $checks[] = ['type' => 'file', 'name' => $file, 'status' => 'ok'];
        } else {
            echo "<div class='check-item'><span class='warning'>⚠</span> {$description} - <strong>Found but may not be updated</strong></div>";
            $checks[] = ['type' => 'file', 'name' => $file, 'status' => 'warning'];
        }
    } else {
        echo "<div class='check-item'><span class='error'>✗</span> {$description} - <strong>NOT FOUND</strong></div>";
        $checks[] = ['type' => 'file', 'name' => $file, 'status' => 'missing'];
        $all_passed = false;
    }
}

// Check 3: Database Connection
echo "<h2>3. Checking Database Connection</h2>";
try {
    require_once 'api/config/database.php';
    $database = new Database();
    $conn = $database->getConnection();
    
    if ($conn) {
        echo "<div class='check-item'><span class='success'>✓</span> Database Connection - <strong>Success</strong></div>";
        $checks[] = ['type' => 'database', 'name' => 'connection', 'status' => 'ok'];
        
        // Check if table exists
        $stmt = $conn->query("SHOW TABLES LIKE 'feedback'");
        if ($stmt->rowCount() > 0) {
            echo "<div class='check-item'><span class='success'>✓</span> Feedback Table - <strong>Exists</strong></div>";
            $checks[] = ['type' => 'database', 'name' => 'table', 'status' => 'ok'];
            
            // Count feedback
            $stmt = $conn->query("SELECT COUNT(*) as count FROM feedback");
            $count = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
            echo "<div class='check-item'><span class='info'>ℹ</span> Total Feedback Records - <strong>{$count}</strong></div>";
        } else {
            echo "<div class='check-item'><span class='warning'>⚠</span> Feedback Table - <strong>Not Created Yet</strong></div>";
            echo "<div class='check-item'><span class='info'>💡</span> <strong>Solution:</strong> Visit <a href='api/install/setup_feedback.php'>api/install/setup_feedback.php</a> to create the table</div>";
            $checks[] = ['type' => 'database', 'name' => 'table', 'status' => 'missing'];
        }
    } else {
        echo "<div class='check-item'><span class='error'>✗</span> Database Connection - <strong>Failed</strong></div>";
        $checks[] = ['type' => 'database', 'name' => 'connection', 'status' => 'error'];
        $all_passed = false;
    }
} catch (Exception $e) {
    echo "<div class='check-item'><span class='error'>✗</span> Database Check - <strong>Error: " . htmlspecialchars($e->getMessage()) . "</strong></div>";
    $checks[] = ['type' => 'database', 'name' => 'check', 'status' => 'error'];
    $all_passed = false;
}

// Summary
echo "<div class='summary'>";
echo "<h2>📊 Summary</h2>";

$ok_count = count(array_filter($checks, function($c) { return $c['status'] === 'ok'; }));
$total_count = count($checks);

if ($all_passed && $ok_count == $total_count) {
    echo "<p class='success'><strong>✅ All checks passed! Feedback system is ready to use.</strong></p>";
} else {
    echo "<p class='warning'><strong>⚠ Some checks failed. Please review the issues above.</strong></p>";
}

echo "<p>Passed: {$ok_count} / {$total_count}</p>";
echo "</div>";

// Next Steps
echo "<h2>🚀 Next Steps</h2>";
echo "<ol>";
echo "<li>If table is missing, visit: <a href='api/install/setup_feedback.php'>api/install/setup_feedback.php</a></li>";
echo "<li>Access admin panel and click 'Feedback' in sidebar</li>";
echo "<li>Test by submitting feedback via API or admin panel</li>";
echo "</ol>";

echo "</body></html>";
?>

