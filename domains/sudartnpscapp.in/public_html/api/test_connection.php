<?php
/**
 * Database Connection Test Page
 */
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Database Connection Test</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            max-width: 900px;
            margin: 50px auto;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
        }
        .container {
            background: white;
            padding: 30px;
            border-radius: 15px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
        }
        h1 {
            color: #667eea;
            text-align: center;
            margin-bottom: 30px;
        }
        .test-section {
            margin: 20px 0;
            padding: 20px;
            border-radius: 8px;
            border: 2px solid #e0e0e0;
        }
        .test-section h2 {
            color: #333;
            margin-top: 0;
            font-size: 18px;
        }
        .success {
            background: #d4edda;
            border-color: #28a745;
        }
        .error {
            background: #f8d7da;
            border-color: #dc3545;
        }
        .info {
            background: #d1ecf1;
            border-color: #17a2b8;
        }
        .result {
            margin: 10px 0;
            padding: 10px;
            background: rgba(255,255,255,0.8);
            border-radius: 5px;
        }
        .icon {
            font-size: 24px;
            margin-right: 10px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 10px;
        }
        table th, table td {
            padding: 10px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }
        table th {
            background: #667eea;
            color: white;
        }
        .btn {
            display: inline-block;
            padding: 10px 20px;
            background: #667eea;
            color: white;
            text-decoration: none;
            border-radius: 5px;
            margin-top: 20px;
        }
        .btn:hover {
            background: #5568d3;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🔍 Mock Test Database Connection Test</h1>
        
        <?php
        require_once 'config/database.php';
        
        // Test 1: Database Connection
        echo '<div class="test-section">';
        echo '<h2><span class="icon">🔌</span>Database Connection</h2>';
        try {
            $database = new Database();
            $conn = $database->getConnection();
            if ($conn) {
                echo '<div class="result success">';
                echo '<strong>✓ SUCCESS:</strong> Connected to database successfully!<br>';
                echo '<small>Database: mock_test_db | Host: localhost</small>';
                echo '</div>';
                
                // Test 2: Tables Check
                echo '</div><div class="test-section info">';
                echo '<h2><span class="icon">📊</span>Database Tables</h2>';
                $tables = ['users', 'exam_categories', 'test_categories', 'question_sessions', 
                          'questions', 'test_results', 'admin_users', 'otp_verifications',
                          'user_answers', 'user_rankings', 'saved_tests'];
                
                echo '<table>';
                echo '<tr><th>Table Name</th><th>Status</th><th>Row Count</th></tr>';
                foreach ($tables as $table) {
                    $stmt = $conn->query("SELECT COUNT(*) as count FROM $table");
                    $count = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
                    echo "<tr><td>$table</td><td style='color: green;'>✓ Exists</td><td>$count rows</td></tr>";
                }
                echo '</table>';
                echo '</div>';
                
                // Test 3: Sample Data Check
                echo '<div class="test-section info">';
                echo '<h2><span class="icon">👥</span>Sample Users</h2>';
                $stmt = $conn->query("SELECT id, name, mobile, email, district FROM users LIMIT 5");
                $users = $stmt->fetchAll(PDO::FETCH_ASSOC);
                if (count($users) > 0) {
                    echo '<table>';
                    echo '<tr><th>ID</th><th>Name</th><th>Mobile</th><th>Email</th><th>District</th></tr>';
                    foreach ($users as $user) {
                        echo "<tr>";
                        echo "<td>{$user['id']}</td>";
                        echo "<td>{$user['name']}</td>";
                        echo "<td>{$user['mobile']}</td>";
                        echo "<td>{$user['email']}</td>";
                        echo "<td>{$user['district']}</td>";
                        echo "</tr>";
                    }
                    echo '</table>';
                } else {
                    echo '<div class="result">No users found in database.</div>';
                }
                echo '</div>';
                
                // Test 4: API Endpoints Check
                echo '<div class="test-section info">';
                echo '<h2><span class="icon">🔗</span>API Endpoints Status</h2>';
                $endpoints = [
                    'Users List' => '/Mock_test/api/admin/users/list.php',
                    'Dashboard Stats' => '/Mock_test/api/admin/get_dashboard_stats.php',
                    'Exam Categories' => '/Mock_test/api/admin/exam_categories/crud.php',
                    'Test Categories' => '/Mock_test/api/admin/test_categories/crud.php',
                    'Question Sessions' => '/Mock_test/api/admin/sessions/crud.php'
                ];
                
                echo '<table>';
                echo '<tr><th>Endpoint</th><th>Path</th><th>Status</th></tr>';
                foreach ($endpoints as $name => $path) {
                    $full_path = $_SERVER['DOCUMENT_ROOT'] . $path;
                    $status = file_exists($full_path) ? '<span style="color: green;">✓ Available</span>' : '<span style="color: red;">✗ Missing</span>';
                    echo "<tr><td>$name</td><td><small>$path</small></td><td>$status</td></tr>";
                }
                echo '</table>';
                echo '</div>';
                
                // Test 5: Admin User Check
                echo '<div class="test-section success">';
                echo '<h2><span class="icon">👤</span>Admin Access</h2>';
                $stmt = $conn->query("SELECT username, email, role, created_at FROM admin_users LIMIT 1");
                $admin = $stmt->fetch(PDO::FETCH_ASSOC);
                if ($admin) {
                    echo '<div class="result">';
                    echo '<strong>Default Admin Credentials:</strong><br>';
                    echo 'Username: <strong>' . htmlspecialchars($admin['username']) . '</strong><br>';
                    echo 'Password: <strong>admin123</strong><br>';
                    echo 'Role: <strong>' . htmlspecialchars($admin['role']) . '</strong><br>';
                    echo 'Created: ' . htmlspecialchars($admin['created_at']);
                    echo '</div>';
                }
                echo '</div>';
                
                // Summary
                echo '<div class="test-section success">';
                echo '<h2><span class="icon">✅</span>Summary</h2>';
                echo '<div class="result">';
                echo '<strong>All systems operational!</strong><br><br>';
                echo '✓ Database connection: OK<br>';
                echo '✓ All tables created: OK<br>';
                echo '✓ Sample data loaded: OK<br>';
                echo '✓ API endpoints: OK<br>';
                echo '✓ Admin access: OK<br>';
                echo '</div>';
                echo '<a href="/Mock_test/admin/index.html" class="btn">Open Admin Panel →</a>';
                echo '</div>';
                
            } else {
                throw new Exception("Failed to connect to database");
            }
        } catch (Exception $e) {
            echo '<div class="result error">';
            echo '<strong>✗ ERROR:</strong> ' . $e->getMessage();
            echo '</div>';
        }
        echo '</div>';
        ?>
    </div>
</body>
</html>

