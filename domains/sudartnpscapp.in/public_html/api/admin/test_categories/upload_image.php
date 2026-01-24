<?php
/**
 * Image Upload Handler for Test Categories
 * Handles secure image upload, validation, and storage
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php';

header('Content-Type: application/json');

// Configuration
$upload_dir = '../../../uploads/test_categories/';
$allowed_extensions = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'];
$max_file_size = 5 * 1024 * 1024; // 5MB

// Create upload directory if it doesn't exist
if (!file_exists($upload_dir)) {
    mkdir($upload_dir, 0755, true);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        // Check if file was uploaded
        if (!isset($_FILES['image']) || $_FILES['image']['error'] === UPLOAD_ERR_NO_FILE) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'No image file uploaded'
            ]);
            exit;
        }

        $file = $_FILES['image'];

        // Check for upload errors
        if ($file['error'] !== UPLOAD_ERR_OK) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'File upload error: ' . $file['error']
            ]);
            exit;
        }

        // Validate file size
        if ($file['size'] > $max_file_size) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'File size exceeds maximum allowed size (5MB)'
            ]);
            exit;
        }

        // Get file extension
        $file_extension = strtolower(pathinfo($file['name'], PATHINFO_EXTENSION));

        // Validate file extension
        if (!in_array($file_extension, $allowed_extensions)) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Invalid file type. Allowed types: ' . implode(', ', $allowed_extensions)
            ]);
            exit;
        }

        // Validate file is actually an image
        if ($file_extension !== 'svg') {
            $image_info = getimagesize($file['tmp_name']);
            if ($image_info === false) {
                http_response_code(400);
                echo json_encode([
                    'success' => false,
                    'message' => 'File is not a valid image'
                ]);
                exit;
            }
        }

        // Generate unique filename
        $timestamp = time();
        $random_string = bin2hex(random_bytes(8));
        $new_filename = "test_category_{$timestamp}_{$random_string}.{$file_extension}";
        $destination = $upload_dir . $new_filename;

        // Move uploaded file
        if (move_uploaded_file($file['tmp_name'], $destination)) {
            // Get image dimensions for additional info
            $image_width = 0;
            $image_height = 0;
            if ($file_extension !== 'svg') {
                list($image_width, $image_height) = getimagesize($destination);
            }

            // Return relative path for database storage
            $relative_path = 'uploads/test_categories/' . $new_filename;

            http_response_code(200);
            echo json_encode([
                'success' => true,
                'message' => 'Image uploaded successfully',
                'data' => [
                    'filename' => $new_filename,
                    'path' => $relative_path,
                    'url' => '../' . $relative_path,
                    'size' => $file['size'],
                    'width' => $image_width,
                    'height' => $image_height,
                    'extension' => $file_extension
                ]
            ]);
        } else {
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Failed to move uploaded file'
            ]);
        }
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Server error: ' . $e->getMessage()
        ]);
    }
} elseif ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
    // Handle image deletion
    try {
        $data = json_decode(file_get_contents('php://input'), true);
        
        if (empty($data['filename'])) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Filename is required'
            ]);
            exit;
        }

        $filename = basename($data['filename']); // Security: prevent directory traversal
        $file_path = $upload_dir . $filename;

        if (file_exists($file_path)) {
            if (unlink($file_path)) {
                http_response_code(200);
                echo json_encode([
                    'success' => true,
                    'message' => 'Image deleted successfully'
                ]);
            } else {
                http_response_code(500);
                echo json_encode([
                    'success' => false,
                    'message' => 'Failed to delete image file'
                ]);
            }
        } else {
            http_response_code(404);
            echo json_encode([
                'success' => false,
                'message' => 'Image file not found'
            ]);
        }
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Server error: ' . $e->getMessage()
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

