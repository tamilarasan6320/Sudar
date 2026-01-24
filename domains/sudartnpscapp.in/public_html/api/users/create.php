<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/User.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'));

    // Get device_id from request body or header
    $deviceId = null;
    if (!empty($data->device_id)) {
        $deviceId = trim($data->device_id);
    }
    if (!$deviceId) {
        $headers = function_exists('getallheaders') ? getallheaders() : [];
        if (isset($headers['X-Device-Id'])) $deviceId = trim($headers['X-Device-Id']);
        if (isset($headers['x-device-id'])) $deviceId = trim($headers['x-device-id']);
        if (isset($_SERVER['HTTP_X_DEVICE_ID'])) $deviceId = trim($_SERVER['HTTP_X_DEVICE_ID']);
    }

    if (!empty($data->mobile) && !empty($data->name)) {
        $user = new User($db);

        $user->mobile = $data->mobile;
        $existing = $user->getUserByMobile();

        if ($existing->rowCount() > 0) {
            http_response_code(409);
            echo json_encode([
                'success' => false,
                'message' => 'User already exists'
            ]);
            exit;
        }

        $user->name = $data->name;
        $user->email = isset($data->email) ? $data->email : null;
        $user->age = isset($data->age) ? $data->age : null;
        $user->district = isset($data->district) ? $data->district : null;
        $user->education = isset($data->education) ? $data->education : null;
        $user->language = isset($data->language) ? $data->language : 'en';

        if ($user->create()) {
            // Generate secure session token
            $sessionToken = bin2hex(random_bytes(32));
            
            // Save device session for single-device auth
            if ($deviceId) {
                $user->updateDeviceSession($deviceId, $sessionToken);
            }

            http_response_code(201);
            echo json_encode([
                'success' => true,
                'message' => 'User profile created successfully',
                'user' => [
                    'id' => $user->id,
                    'name' => $user->name,
                    'mobile' => $user->mobile,
                    'email' => $user->email,
                    'age' => $user->age,
                    'district' => $user->district,
                    'education' => $user->education,
                    'language' => $user->language
                ],
                'token' => $sessionToken
            ]);
        } else {
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Failed to create user profile'
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Mobile and name are required'
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
