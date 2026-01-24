<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

$uploadDir = '../../../uploads/questions/images/';
$baseUrl = 'https://sudartnpscapp.in/uploads/questions/images/';
$maxFileSize = 5 * 1024 * 1024;
$allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

if (!file_exists($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

$response = ['success' => false];

try {
    if (isset($_POST['imageData']) && isset($_POST['imageName'])) {
        $imageData = $_POST['imageData'];
        $imageName = $_POST['imageName'];
        
        if (strpos($imageData, 'base64,') !== false) {
            $imageData = explode('base64,', $imageData)[1];
        }
        
        $imageData = base64_decode($imageData);
        
        if ($imageData === false) {
            throw new Exception('Invalid image data');
        }
        
        $extension = pathinfo($imageName, PATHINFO_EXTENSION);
        if (empty($extension)) {
            $extension = 'jpg';
        }
        $filename = uniqid() . '_' . time() . '.' . $extension;
        $filepath = $uploadDir . $filename;
        
        if (file_put_contents($filepath, $imageData)) {
            $response['success'] = true;
            $response['filename'] = $filename;
            $response['url'] = $baseUrl . $filename;
            $response['message'] = 'Image uploaded successfully';
        } else {
            throw new Exception('Failed to save image');
        }
        
    } elseif (isset($_FILES['image'])) {
        $file = $_FILES['image'];
        
        if ($file['error'] !== UPLOAD_ERR_OK) {
            throw new Exception('Upload error: ' . $file['error']);
        }
        
        if ($file['size'] > $maxFileSize) {
            throw new Exception('File too large. Max size: 5MB');
        }
        
        $finfo = finfo_open(FILEINFO_MIME_TYPE);
        $mimeType = finfo_file($finfo, $file['tmp_name']);
        finfo_close($finfo);
        
        if (!in_array($mimeType, $allowedTypes)) {
            throw new Exception('Invalid file type');
        }
        
        $extension = pathinfo($file['name'], PATHINFO_EXTENSION);
        $filename = uniqid() . '_' . time() . '.' . $extension;
        $filepath = $uploadDir . $filename;
        
        if (move_uploaded_file($file['tmp_name'], $filepath)) {
            $response['success'] = true;
            $response['filename'] = $filename;
            $response['url'] = $baseUrl . $filename;
            $response['message'] = 'Image uploaded successfully';
        } else {
            throw new Exception('Failed to move uploaded file');
        }
        
    } else {
        throw new Exception('No image data provided');
    }
    
} catch (Exception $e) {
    $response['success'] = false;
    $response['message'] = $e->getMessage();
    error_log('Image upload error: ' . $e->getMessage());
}

echo json_encode($response);
?>

