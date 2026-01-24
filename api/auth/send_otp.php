<?php
require_once '../config/cors.php';
require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'));

    if (!empty($data->mobile) && preg_match('/^[0-9]{10}$/', $data->mobile)) {
        $mobile = $data->mobile;
        // Default test OTP for development
        $otp = '111111';
        // Fix timezone issue - use current timestamp + 10 minutes
        $expires_at = date('Y-m-d H:i:s', time() + (10 * 60));

        try {
            $delete_query = "DELETE FROM otp_verifications WHERE mobile = ?";
            $delete_stmt = $db->prepare($delete_query);
            $delete_stmt->execute([$mobile]);

            $insert_query = "INSERT INTO otp_verifications (mobile, otp, expires_at) VALUES (?, ?, ?)";
            $insert_stmt = $db->prepare($insert_query);
            $insert_stmt->execute([$mobile, $otp, $expires_at]);

            http_response_code(200);
            echo json_encode([
                'success' => true,
                'message' => 'OTP sent successfully',
                'otp' => $otp,
                'expires_in' => 600
            ]);
        } catch (PDOException $e) {
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Failed to send OTP',
                'error' => $e->getMessage()
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Invalid mobile number. Please provide a 10-digit number'
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
