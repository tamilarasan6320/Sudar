<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/User.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'PUT' || $_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'));

    if (!empty($data->user_id)) {
        $user = new User($db);
        $user->id = $data->user_id;

        $user->name = isset($data->name) ? $data->name : '';
        $user->email = isset($data->email) ? $data->email : null;
        $user->age = isset($data->age) ? $data->age : null;
        $user->district = isset($data->district) ? $data->district : null;
        $user->education = isset($data->education) ? $data->education : null;
        $user->language = isset($data->language) ? $data->language : 'en';
        $user->profile_pic = isset($data->profile_pic) ? $data->profile_pic : null;

        if ($user->update()) {
            http_response_code(200);
            echo json_encode([
                'success' => true,
                'message' => 'Profile updated successfully',
                'user' => [
                    'id' => $user->id,
                    'name' => $user->name,
                    'email' => $user->email,
                    'age' => $user->age,
                    'district' => $user->district,
                    'education' => $user->education,
                    'language' => $user->language,
                    'profile_pic' => $user->profile_pic
                ]
            ]);
        } else {
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Failed to update profile'
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
