<?php
/**
 * Premium Video Upload Handler
 * Handles secure video upload, validation, and storage
 * Updates app_settings with video metadata
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php';

header('Content-Type: application/json');

// Configuration
$upload_dir = '../../../uploads/premium/';
$allowed_extensions = ['mp4', 'webm', 'mov'];
$allowed_mimes = ['video/mp4', 'video/webm', 'video/quicktime'];
$max_file_size = 100 * 1024 * 1024; // 100MB

// Create upload directory if it doesn't exist
if (!file_exists($upload_dir)) {
    mkdir($upload_dir, 0755, true);
}

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        // Check if file was uploaded
        if (!isset($_FILES['video']) || $_FILES['video']['error'] === UPLOAD_ERR_NO_FILE) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'No video file uploaded'
            ]);
            exit;
        }

        $file = $_FILES['video'];

        // Check for upload errors
        if ($file['error'] !== UPLOAD_ERR_OK) {
            $error_messages = [
                UPLOAD_ERR_INI_SIZE => 'File exceeds upload_max_filesize directive in php.ini',
                UPLOAD_ERR_FORM_SIZE => 'File exceeds MAX_FILE_SIZE directive in the HTML form',
                UPLOAD_ERR_PARTIAL => 'File was only partially uploaded',
                UPLOAD_ERR_NO_TMP_DIR => 'Missing a temporary folder',
                UPLOAD_ERR_CANT_WRITE => 'Failed to write file to disk',
                UPLOAD_ERR_EXTENSION => 'A PHP extension stopped the file upload'
            ];
            $error_msg = isset($error_messages[$file['error']]) 
                ? $error_messages[$file['error']] 
                : 'Unknown upload error';
            
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'File upload error: ' . $error_msg
            ]);
            exit;
        }

        // Validate file size
        if ($file['size'] > $max_file_size) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'File size exceeds maximum allowed size (100MB)'
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

        // Validate MIME type
        $finfo = finfo_open(FILEINFO_MIME_TYPE);
        $detected_mime = finfo_file($finfo, $file['tmp_name']);
        finfo_close($finfo);

        if (!in_array($detected_mime, $allowed_mimes)) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Invalid MIME type. Detected: ' . $detected_mime
            ]);
            exit;
        }

        // Generate unique filename with timestamp as version
        $timestamp = time();
        $random_string = bin2hex(random_bytes(8));
        $new_filename = "premium_{$timestamp}_{$random_string}.{$file_extension}";
        $destination = $upload_dir . $new_filename;

        // Delete old premium videos (keep only latest)
        $old_files = glob($upload_dir . 'premium_*');
        foreach ($old_files as $old_file) {
            if (is_file($old_file)) {
                unlink($old_file);
            }
        }

        // Move uploaded file
        if (move_uploaded_file($file['tmp_name'], $destination)) {
            // Relative path for database storage
            $relative_path = 'uploads/premium/' . $new_filename;

            // Prepare JSON metadata
            $video_metadata = json_encode([
                'path' => $relative_path,
                'version' => (string) $timestamp,
                'mime' => $detected_mime,
                'size' => $file['size'],
                'filename' => $new_filename,
                'uploaded_at' => date('Y-m-d H:i:s')
            ]);

            // Update or insert app_settings
            $check_query = "SELECT id FROM app_settings WHERE setting_key = 'premium_video'";
            $check_stmt = $db->prepare($check_query);
            $check_stmt->execute();

            if ($check_stmt->rowCount() > 0) {
                // Update existing
                $update_query = "UPDATE app_settings 
                                SET setting_value = ?, setting_type = 'json'
                                WHERE setting_key = 'premium_video'";
                $update_stmt = $db->prepare($update_query);
                $update_stmt->execute([$video_metadata]);
            } else {
                // Create new
                $insert_query = "INSERT INTO app_settings (setting_key, setting_value, setting_type, description)
                                VALUES ('premium_video', ?, 'json', 'Premium subscription video for Android app')";
                $insert_stmt = $db->prepare($insert_query);
                $insert_stmt->execute([$video_metadata]);
            }

            http_response_code(200);
            echo json_encode([
                'success' => true,
                'message' => 'Premium video uploaded successfully',
                'data' => [
                    'filename' => $new_filename,
                    'path' => $relative_path,
                    'version' => (string) $timestamp,
                    'size' => $file['size'],
                    'mime' => $detected_mime
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
} elseif ($_SERVER['REQUEST_METHOD'] === 'GET') {
    // Get current premium video info
    try {
        $query = "SELECT setting_value FROM app_settings WHERE setting_key = 'premium_video'";
        $stmt = $db->prepare($query);
        $stmt->execute();
        $result = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($result) {
            $video_data = json_decode($result['setting_value'], true);
            http_response_code(200);
            echo json_encode([
                'success' => true,
                'data' => $video_data
            ]);
        } else {
            http_response_code(404);
            echo json_encode([
                'success' => false,
                'message' => 'No premium video configured'
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
    // Delete premium video
    try {
        // Get current video info
        $query = "SELECT setting_value FROM app_settings WHERE setting_key = 'premium_video'";
        $stmt = $db->prepare($query);
        $stmt->execute();
        $result = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($result) {
            $video_data = json_decode($result['setting_value'], true);
            
            // Delete file
            $file_path = '../../../' . $video_data['path'];
            if (file_exists($file_path)) {
                unlink($file_path);
            }

            // Delete setting
            $delete_query = "DELETE FROM app_settings WHERE setting_key = 'premium_video'";
            $delete_stmt = $db->prepare($delete_query);
            $delete_stmt->execute();

            http_response_code(200);
            echo json_encode([
                'success' => true,
                'message' => 'Premium video deleted successfully'
            ]);
        } else {
            http_response_code(404);
            echo json_encode([
                'success' => false,
                'message' => 'No premium video to delete'
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
