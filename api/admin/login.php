<?php
require_once '../config/cors.php';
require_once '../config/database.php';
session_start();

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'));

    if (!empty($data->username) && !empty($data->password)) {
        $query = "SELECT * FROM admin_users WHERE username = ? AND is_active = 1 LIMIT 1";
        $stmt = $db->prepare($query);
        $stmt->execute([$data->username]);

        if ($stmt->rowCount() > 0) {
            $admin = $stmt->fetch(PDO::FETCH_ASSOC);

            if (password_verify($data->password, $admin['password'])) {
                $update_query = "UPDATE admin_users SET last_login = NOW() WHERE id = ?";
                $update_stmt = $db->prepare($update_query);
                $update_stmt->execute([$admin['id']]);

                $_SESSION['admin_id'] = $admin['id'];
                $_SESSION['admin_username'] = $admin['username'];
                $_SESSION['admin_role'] = $admin['role'];

                http_response_code(200);
                echo json_encode([
                    'success' => true,
                    'message' => 'Login successful',
                    'admin' => [
                        'id' => $admin['id'],
                        'username' => $admin['username'],
                        'email' => $admin['email'],
                        'role' => $admin['role']
                    ],
                    'session_token' => session_id()
                ]);
            } else {
                http_response_code(401);
                echo json_encode([
                    'success' => false,
                    'message' => 'Invalid password'
                ]);
            }
        } else {
            http_response_code(401);
            echo json_encode([
                'success' => false,
                'message' => 'Invalid username'
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Username and password are required'
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed'
    ]);
}
?>
