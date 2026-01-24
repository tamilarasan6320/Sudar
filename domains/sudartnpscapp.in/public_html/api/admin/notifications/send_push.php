<?php
/**
 * Admin Panel → OneSignal Push Notification API
 * 
 * Sends push notifications to app users via OneSignal REST API
 * 
 * POST /api/admin/notifications/send_push.php
 * 
 * Body:
 * {
 *   "title": "Notification Title",
 *   "message": "Notification Message",
 *   "target_type": "all" | "specific_user" | "user_segment",
 *   "target_user_id": 123,  // Required if target_type = "specific_user"
 *   "additional_data": {}   // Optional custom data
 * }
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require admin authentication

// OneSignal Configuration
define('ONESIGNAL_APP_ID', '4ead7a7e-e376-41bf-8219-dd1a04e36da4');
define('ONESIGNAL_REST_API_KEY', 'os_v2_app_j2wxu7xdoza37aqz3unajy3nurwzo5bcvagu6557lb2fgs4cm6wy4bi4lcolunusica4pyca4dtq63xintb3kw6ykuviqmbvpm2fgly');

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        $data = json_decode(file_get_contents('php://input'), true);
        
        // Validate input
        $title = trim($data['title'] ?? '');
        $message = trim($data['message'] ?? '');
        $targetType = $data['target_type'] ?? 'all';
        $targetUserId = isset($data['target_user_id']) ? intval($data['target_user_id']) : null;
        $additionalData = $data['additional_data'] ?? [];
        
        if (empty($title) || empty($message)) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Title and message are required'
            ]);
            exit;
        }
        
        $database = new Database();
        $db = $database->getConnection();
        
        // 1. Store notification in database
        $stmt = $db->prepare("
            INSERT INTO notifications 
            (title, message, target_audience, target_user_id, created_by, created_at) 
            VALUES (?, ?, ?, ?, ?, NOW())
        ");
        $adminId = $_SESSION['admin_id'] ?? null;
        $stmt->execute([
            $title, 
            $message, 
            $targetType, 
            $targetUserId, 
            $adminId
        ]);
        $notificationId = $db->lastInsertId();
        
        // 2. Prepare OneSignal payload
        $onesignalData = [
            'app_id' => ONESIGNAL_APP_ID,
            'contents' => ['en' => $message],
            'headings' => ['en' => $title],
            'data' => array_merge($additionalData, [
                'notification_id' => $notificationId,
                'type' => 'admin_notification',
                'target_type' => $targetType
            ])
        ];
        
        // 3. Set target audience based on target_type
        if ($targetType === 'specific_user' && $targetUserId) {
            // Get user's OneSignal player ID from database
            $userStmt = $db->prepare("SELECT onesignal_player_id FROM users WHERE id = ?");
            $userStmt->execute([$targetUserId]);
            $user = $userStmt->fetch(PDO::FETCH_ASSOC);
            
            if ($user && !empty($user['onesignal_player_id'])) {
                // Use player ID if available
                $onesignalData['include_player_ids'] = [$user['onesignal_player_id']];
            } else {
                // Fallback: Use external user ID
                $onesignalData['include_external_user_ids'] = [(string)$targetUserId];
            }
        } else {
            // Send to all users
            $onesignalData['included_segments'] = ['All'];
        }
        
        // 4. Send to OneSignal REST API
        $ch = curl_init('https://onesignal.com/api/v1/notifications');
        curl_setopt_array($ch, [
            CURLOPT_HTTPHEADER => [
                'Content-Type: application/json; charset=utf-8',
                'Authorization: Bearer ' . ONESIGNAL_REST_API_KEY
            ],
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => json_encode($onesignalData),
            CURLOPT_SSL_VERIFYPEER => true
        ]);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlError = curl_error($ch);
        curl_close($ch);
        
        if ($curlError) {
            throw new Exception('CURL Error: ' . $curlError);
        }
        
        $onesignalResult = json_decode($response, true);
        
        // 5. Update notification record with OneSignal response
        if ($httpCode === 200 && isset($onesignalResult['id'])) {
            $updateStmt = $db->prepare("
                UPDATE notifications 
                SET onesignal_notification_id = ?, 
                    sent_at = NOW() 
                WHERE id = ?
            ");
            $updateStmt->execute([$onesignalResult['id'], $notificationId]);
        }
        
        // 6. Return response
        if ($httpCode === 200) {
            http_response_code(200);
            echo json_encode([
                'success' => true,
                'message' => 'Notification sent successfully',
                'notification_id' => $notificationId,
                'onesignal_notification_id' => $onesignalResult['id'] ?? null,
                'recipients' => $onesignalResult['recipients'] ?? 0
            ]);
        } else {
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'Failed to send notification',
                'onesignal_error' => $onesignalResult['errors'] ?? $response
            ]);
        }
        
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Database error: ' . $e->getMessage()
        ]);
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Error: ' . $e->getMessage()
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

