<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/User.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $user_id = isset($_GET['user_id']) ? $_GET['user_id'] : null;

    if (!empty($user_id)) {
        $user = new User($db);
        $user->id = $user_id;

        $stmt = $user->getUserStats();

        if ($stmt->rowCount() > 0) {
            $row = $stmt->fetch(PDO::FETCH_ASSOC);

            http_response_code(200);
            echo json_encode([
                'success' => true,
                'user' => $row
            ]);
        } else {
            http_response_code(404);
            echo json_encode([
                'success' => false,
                'message' => 'User not found'
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'User ID is required'
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
