<?php
/**
 * Users CRUD Operations
 * Handles Create, Update, and Delete operations for users
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication
require_once '../../models/User.php';

$database = new Database();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

// CREATE - Add new user
if ($method === 'POST') {
    $data = json_decode(file_get_contents("php://input"));
    
    if (!empty($data->name) && !empty($data->mobile)) {
        try {
            $user = new User($db);
            
            $user->name = $data->name;
            $user->mobile = $data->mobile;
            $user->email = $data->email ?? null;
            $user->age = $data->age ?? null;
            $user->district = $data->district ?? null;
            $user->education = $data->education ?? null;
            $user->language = $data->language ?? 'en';
            
            if ($user->create()) {
                http_response_code(201);
                echo json_encode([
                    'success' => true,
                    'message' => 'User created successfully',
                    'id' => $db->lastInsertId()
                ]);
            } else {
                http_response_code(500);
                echo json_encode([
                    'success' => false,
                    'message' => 'Unable to create user'
                ]);
            }
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Error creating user',
                'error' => $e->getMessage()
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Incomplete data. Name and mobile are required.'
        ]);
    }
}

// UPDATE - Edit existing user
else if ($method === 'PUT') {
    $data = json_decode(file_get_contents("php://input"));
    
    if (!empty($data->id)) {
        try {
            $user = new User($db);
            
            $user->id = $data->id;
            $user->name = $data->name;
            $user->mobile = $data->mobile;
            $user->email = $data->email ?? null;
            $user->age = $data->age ?? null;
            $user->district = $data->district ?? null;
            $user->education = $data->education ?? null;
            $user->language = $data->language ?? 'en';
            
            if ($user->update()) {
                http_response_code(200);
                echo json_encode([
                    'success' => true,
                    'message' => 'User updated successfully'
                ]);
            } else {
                http_response_code(500);
                echo json_encode([
                    'success' => false,
                    'message' => 'Unable to update user'
                ]);
            }
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Error updating user',
                'error' => $e->getMessage()
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'User ID is required'
        ]);
    }
}

// DELETE - Remove user
else if ($method === 'DELETE') {
    $data = json_decode(file_get_contents("php://input"));
    
    if (!empty($data->id)) {
        try {
            $user = new User($db);
            $user->id = $data->id;
            
            if ($user->delete()) {
                http_response_code(200);
                echo json_encode([
                    'success' => true,
                    'message' => 'User deleted successfully'
                ]);
            } else {
                http_response_code(500);
                echo json_encode([
                    'success' => false,
                    'message' => 'Unable to delete user'
                ]);
            }
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Error deleting user',
                'error' => $e->getMessage()
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'User ID is required'
        ]);
    }
}

else {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed'
    ]);
}
?>

