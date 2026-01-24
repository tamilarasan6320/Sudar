<?php
require_once '../config/cors.php';
session_start();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Destroy session
    session_unset();
    session_destroy();
    
    http_response_code(200);
    echo json_encode([
        'success' => true,
        'message' => 'Logged out successfully'
    ]);
} else {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed'
    ]);
}
?>

