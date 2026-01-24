<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    try {
        // Get filters
        $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : null;
        $session_id = isset($_GET['session_id']) ? intval($_GET['session_id']) : null;
        $date_from = isset($_GET['date_from']) ? $_GET['date_from'] : null;
        $date_to = isset($_GET['date_to']) ? $_GET['date_to'] : null;
        $min_score = isset($_GET['min_score']) ? floatval($_GET['min_score']) : null;
        $max_score = isset($_GET['max_score']) ? floatval($_GET['max_score']) : null;
        $limit = isset($_GET['limit']) ? intval($_GET['limit']) : 100;
        $offset = isset($_GET['offset']) ? intval($_GET['offset']) : 0;
        
        // Build query
        $query = "SELECT 
                    tr.*,
                    u.name as user_name,
                    u.mobile as user_mobile,
                    qs.name as test_name,
                    qs.total_questions as session_total_questions,
                    tc.name as category_name,
                    ec.name as exam_name
                  FROM test_results tr
                  INNER JOIN users u ON tr.user_id = u.id
                  INNER JOIN question_sessions qs ON tr.session_id = qs.id
                  LEFT JOIN test_categories tc ON qs.test_category_id = tc.id
                  LEFT JOIN exam_categories ec ON tc.exam_category_id = ec.id
                  WHERE 1=1";
        
        $params = [];
        
        if ($user_id) {
            $query .= " AND tr.user_id = :user_id";
            $params[':user_id'] = $user_id;
        }
        
        if ($session_id) {
            $query .= " AND tr.session_id = :session_id";
            $params[':session_id'] = $session_id;
        }
        
        if ($date_from) {
            $query .= " AND DATE(tr.submitted_at) >= :date_from";
            $params[':date_from'] = $date_from;
        }
        
        if ($date_to) {
            $query .= " AND DATE(tr.submitted_at) <= :date_to";
            $params[':date_to'] = $date_to;
        }
        
        if ($min_score !== null) {
            $query .= " AND tr.percentage >= :min_score";
            $params[':min_score'] = $min_score;
        }
        
        if ($max_score !== null) {
            $query .= " AND tr.percentage <= :max_score";
            $params[':max_score'] = $max_score;
        }
        
        $query .= " ORDER BY tr.submitted_at DESC LIMIT :limit OFFSET :offset";
        
        $stmt = $db->prepare($query);
        
        foreach ($params as $key => $value) {
            $stmt->bindValue($key, $value);
        }
        
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
        
        $stmt->execute();
        
        $results = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $results[] = [
                'id' => intval($row['id']),
                'user_id' => intval($row['user_id']),
                'user_name' => $row['user_name'],
                'user_mobile' => $row['user_mobile'],
                'session_id' => intval($row['session_id']),
                'test_name' => $row['test_name'],
                'category_name' => $row['category_name'],
                'exam_name' => $row['exam_name'],
                'total_questions' => intval($row['total_questions']),
                'attempted_questions' => intval($row['attempted_questions']),
                'correct_answers' => intval($row['correct_answers']),
                'wrong_answers' => intval($row['wrong_answers']),
                'unanswered' => intval($row['unanswered']),
                'score' => floatval($row['score']),
                'percentage' => round(floatval($row['percentage']), 2),
                'time_taken' => intval($row['time_taken']),
                'rank' => $row['rank'] ? intval($row['rank']) : null,
                'started_at' => $row['started_at'],
                'submitted_at' => $row['submitted_at'],
                'status' => floatval($row['percentage']) >= 50 ? 'passed' : 'failed'
            ];
        }
        
        // Get total count for pagination
        $countQuery = "SELECT COUNT(*) as total FROM test_results tr WHERE 1=1";
        $countParams = [];
        
        if ($user_id) {
            $countQuery .= " AND tr.user_id = :user_id";
            $countParams[':user_id'] = $user_id;
        }
        if ($session_id) {
            $countQuery .= " AND tr.session_id = :session_id";
            $countParams[':session_id'] = $session_id;
        }
        if ($date_from) {
            $countQuery .= " AND DATE(tr.submitted_at) >= :date_from";
            $countParams[':date_from'] = $date_from;
        }
        if ($date_to) {
            $countQuery .= " AND DATE(tr.submitted_at) <= :date_to";
            $countParams[':date_to'] = $date_to;
        }
        if ($min_score !== null) {
            $countQuery .= " AND tr.percentage >= :min_score";
            $countParams[':min_score'] = $min_score;
        }
        if ($max_score !== null) {
            $countQuery .= " AND tr.percentage <= :max_score";
            $countParams[':max_score'] = $max_score;
        }
        
        $countStmt = $db->prepare($countQuery);
        foreach ($countParams as $key => $value) {
            $countStmt->bindValue($key, $value);
        }
        $countStmt->execute();
        $totalCount = $countStmt->fetch(PDO::FETCH_ASSOC)['total'];
        
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'count' => count($results),
            'total' => intval($totalCount),
            'limit' => $limit,
            'offset' => $offset,
            'results' => $results
        ]);
        
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Database error: ' . $e->getMessage()
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

