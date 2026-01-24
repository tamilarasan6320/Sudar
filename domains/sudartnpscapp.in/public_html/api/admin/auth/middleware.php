<?php
/**
 * Admin Authentication Middleware
 * Include this file at the top of admin API endpoints to protect them
 */

session_start();

if (!isset($_SESSION['admin_id']) || !isset($_SESSION['admin_username'])) {
    http_response_code(401);
    echo json_encode([
        'success' => false,
        'message' => 'Unauthorized. Please login first.',
        'authenticated' => false
    ]);
    exit;
}

// Admin is authenticated, continue with the request
?>

