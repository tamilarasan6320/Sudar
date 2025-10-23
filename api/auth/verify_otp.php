<?php
require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../models/User.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'));

    if (!empty($data->mobile) && !empty($data->otp)) {
        $mobile = $data->mobile;
        $otp = $data->otp;

        try {
            // Development bypass - accept 123456 as valid test OTP
            if ($otp == '123456') {
                $user = new User($db);
                $user->mobile = $mobile;
                $user_stmt = $user->getUserByMobile();

                if ($user_stmt->rowCount() > 0) {
                    $user_data = $user_stmt->fetch(PDO::FETCH_ASSOC);
                    $user->id = $user_data['id'];
                    $user->updateLastLogin();

                    http_response_code(200);
                    echo json_encode([
                        'success' => true,
                        'message' => 'OTP verified successfully (Test Mode)',
                        'is_new_user' => false,
                        'user' => [
                            'id' => $user_data['id'],
                            'name' => $user_data['name'],
                            'mobile' => $user_data['mobile'],
                            'email' => $user_data['email'],
                            'age' => $user_data['age'],
                            'district' => $user_data['district'],
                            'education' => $user_data['education'],
                            'language' => $user_data['language'],
                            'profile_pic' => $user_data['profile_pic']
                        ],
                        'token' => base64_encode($user_data['id'] . ':' . time())
                    ]);
                    exit;
                } else {
                    http_response_code(200);
                    echo json_encode([
                        'success' => true,
                        'message' => 'OTP verified successfully (Test Mode)',
                        'is_new_user' => true,
                        'mobile' => $mobile,
                        'token' => base64_encode($mobile . ':' . time())
                    ]);
                    exit;
                }
            }
            
            // Normal OTP verification
            $query = "SELECT * FROM otp_verifications
                      WHERE mobile = ? AND otp = ? AND expires_at > NOW() AND is_verified = 0
                      ORDER BY created_at DESC LIMIT 1";
            $stmt = $db->prepare($query);
            $stmt->execute([$mobile, $otp]);

            if ($stmt->rowCount() > 0) {
                $otp_row = $stmt->fetch(PDO::FETCH_ASSOC);

                if ($otp_row['attempts'] >= 5) {
                    http_response_code(429);
                    echo json_encode([
                        'success' => false,
                        'message' => 'Too many attempts. Please request a new OTP'
                    ]);
                    exit;
                }

                $update_query = "UPDATE otp_verifications SET is_verified = 1 WHERE id = ?";
                $update_stmt = $db->prepare($update_query);
                $update_stmt->execute([$otp_row['id']]);

                $user = new User($db);
                $user->mobile = $mobile;
                $user_stmt = $user->getUserByMobile();

                if ($user_stmt->rowCount() > 0) {
                    $user_data = $user_stmt->fetch(PDO::FETCH_ASSOC);
                    $user->id = $user_data['id'];
                    $user->updateLastLogin();

                    http_response_code(200);
                    echo json_encode([
                        'success' => true,
                        'message' => 'OTP verified successfully',
                        'is_new_user' => false,
                        'user' => [
                            'id' => $user_data['id'],
                            'name' => $user_data['name'],
                            'mobile' => $user_data['mobile'],
                            'email' => $user_data['email'],
                            'age' => $user_data['age'],
                            'district' => $user_data['district'],
                            'education' => $user_data['education'],
                            'language' => $user_data['language'],
                            'profile_pic' => $user_data['profile_pic']
                        ],
                        'token' => base64_encode($user_data['id'] . ':' . time())
                    ]);
                } else {
                    http_response_code(200);
                    echo json_encode([
                        'success' => true,
                        'message' => 'OTP verified successfully',
                        'is_new_user' => true,
                        'mobile' => $mobile,
                        'token' => base64_encode($mobile . ':' . time())
                    ]);
                }
            } else {
                $update_attempts = "UPDATE otp_verifications SET attempts = attempts + 1
                                   WHERE mobile = ? AND otp = ? AND is_verified = 0";
                $update_stmt = $db->prepare($update_attempts);
                $update_stmt->execute([$mobile, $otp]);

                http_response_code(400);
                echo json_encode([
                    'success' => false,
                    'message' => 'Invalid or expired OTP'
                ]);
            }
        } catch (PDOException $e) {
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Verification failed',
                'error' => $e->getMessage()
            ]);
        }
    } else {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Mobile number and OTP are required'
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
