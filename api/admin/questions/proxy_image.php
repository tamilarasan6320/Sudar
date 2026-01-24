<?php
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

header('Content-Type: application/json');

$response = ['success' => false];

try {
    // Get image URL from POST request
    $imageUrl = $_POST['imageUrl'] ?? '';
    
    if (empty($imageUrl)) {
        throw new Exception('No image URL provided');
    }
    
    // Validate URL
    if (!filter_var($imageUrl, FILTER_VALIDATE_URL)) {
        throw new Exception('Invalid image URL');
    }
    
    // Initialize cURL to download the image
    $ch = curl_init($imageUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    curl_setopt($ch, CURLOPT_USERAGENT, 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36');
    
    $imageData = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);
    
    if ($httpCode !== 200 || $imageData === false) {
        throw new Exception("Failed to download image: HTTP $httpCode - $error");
    }
    
    // Get file extension from URL
    $urlParts = parse_url($imageUrl);
    $pathParts = pathinfo($urlParts['path']);
    $extension = $pathParts['extension'] ?? 'jpg';
    
    // Validate it's an image
    $finfo = new finfo(FILEINFO_MIME_TYPE);
    $mimeType = $finfo->buffer($imageData);
    
    $allowedMimes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
    if (!in_array($mimeType, $allowedMimes)) {
        throw new Exception('Downloaded file is not a valid image');
    }
    
    // Return base64 encoded image
    $base64Data = base64_encode($imageData);
    
    $response['success'] = true;
    $response['imageData'] = 'data:' . $mimeType . ';base64,' . $base64Data;
    $response['mimeType'] = $mimeType;
    $response['size'] = strlen($imageData);
    $response['extension'] = $extension;
    $response['message'] = 'Image downloaded successfully';
    
} catch (Exception $e) {
    $response['success'] = false;
    $response['message'] = $e->getMessage();
    error_log('Proxy image error: ' . $e->getMessage());
}

echo json_encode($response);
?>

