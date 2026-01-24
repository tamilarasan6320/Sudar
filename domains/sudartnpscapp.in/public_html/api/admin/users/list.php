<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication
require_once '../../models/User.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $limit = isset($_GET['limit']) ? intval($_GET['limit']) : 100;
    $offset = isset($_GET['offset']) ? intval($_GET['offset']) : 0;
    $search = isset($_GET['search']) ? $_GET['search'] : '';
    
    try {
        $user = new User($db);
        
        if (!empty($search)) {
            $stmt = $user->searchUsers($search);
        } else {
            $stmt = $user->getAllUsers($limit, $offset);
        }
        
        $users = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            // Get verification_method directly from database
            $verifyQuery = "SELECT verification_method FROM users WHERE id = :user_id";
            $verifyStmt = $db->prepare($verifyQuery);
            $verifyStmt->bindParam(':user_id', $row['id'], PDO::PARAM_INT);
            $verifyStmt->execute();
            $verifyData = $verifyStmt->fetch(PDO::FETCH_ASSOC);
            $row['verification_method'] = $verifyData['verification_method'] ?? 'otp';
            
            // Check premium status for each user
            $row['is_premium'] = false;
            $row['premium_expires_at'] = null;
            $row['subscription_status'] = 'free';
            
            // Check manual premium in users table
            $premiumQuery = "SELECT is_premium, premium_expires_at FROM users WHERE id = :user_id";
            $premiumStmt = $db->prepare($premiumQuery);
            $premiumStmt->bindParam(':user_id', $row['id'], PDO::PARAM_INT);
            $premiumStmt->execute();
            $premiumData = $premiumStmt->fetch(PDO::FETCH_ASSOC);
            
            if ($premiumData && $premiumData['is_premium']) {
                if (!$premiumData['premium_expires_at'] || strtotime($premiumData['premium_expires_at']) > time()) {
                    $row['is_premium'] = true;
                    $row['premium_expires_at'] = $premiumData['premium_expires_at'];
                    $row['subscription_status'] = 'premium';
                }
            }
            
            // Check subscription table
            $subQuery = "SELECT status, current_period_end, trial_end, is_trial 
                         FROM subscriptions 
                         WHERE user_id = :user_id AND status IN ('active', 'authenticated') 
                         ORDER BY created_at DESC LIMIT 1";
            $subStmt = $db->prepare($subQuery);
            $subStmt->bindParam(':user_id', $row['id'], PDO::PARAM_INT);
            $subStmt->execute();
            $subData = $subStmt->fetch(PDO::FETCH_ASSOC);
            
            if ($subData) {
                $row['is_premium'] = true;
                $row['subscription_status'] = $subData['is_trial'] ? 'trial' : $subData['status'];
                $row['premium_expires_at'] = $subData['current_period_end'] ?: $subData['trial_end'];
            }
            
            $users[] = $row;
        }
        
        // Get total count
        $count_query = "SELECT COUNT(*) as total FROM users WHERE is_active = 1";
        $count_stmt = $db->query($count_query);
        $total = $count_stmt->fetch(PDO::FETCH_ASSOC)['total'];
        
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'users' => $users,
            'total' => $total,
            'count' => count($users)
        ]);
        
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Error fetching users',
            'error' => $e->getMessage()
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

