<?php
require_once '../config/cors.php';
require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $session_id = isset($_GET['session_id']) ? $_GET['session_id'] : null;
    $limit = isset($_GET['limit']) ? intval($_GET['limit']) : 100;

    if (!empty($session_id)) {
        $query = "SELECT tr.rank, tr.score, tr.percentage, tr.time_taken, tr.submitted_at,
                         u.id AS user_id, u.name AS user_name
                  FROM test_results tr
                  LEFT JOIN users u ON tr.user_id = u.id
                  WHERE tr.session_id = ?
                  ORDER BY tr.rank ASC
                  LIMIT ?";

        $stmt = $db->prepare($query);
        $stmt->bindParam(1, $session_id, PDO::PARAM_INT);
        $stmt->bindParam(2, $limit, PDO::PARAM_INT);
        $stmt->execute();

        $rankings = [];

        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $rankings[] = [
                'rank' => $row['rank'],
                'user_id' => $row['user_id'],
                'user_name' => $row['user_name'],
                'score' => $row['score'],
                'percentage' => round($row['percentage'], 2),
                'time_taken' => $row['time_taken'],
                'submitted_at' => $row['submitted_at']
            ];
        }

        http_response_code(200);
        echo json_encode([
            'success' => true,
            'count' => count($rankings),
            'rankings' => $rankings
        ]);
    } else {
        $query = "SELECT
                      u.id, u.name, u.district,
                      COUNT(tr.id) AS total_tests,
                      AVG(tr.percentage) AS avg_score,
                      MAX(tr.score) AS best_score,
                      SUM(tr.score) AS total_score
                  FROM users u
                  LEFT JOIN test_results tr ON u.id = tr.user_id
                  GROUP BY u.id
                  HAVING total_tests > 0
                  ORDER BY avg_score DESC, best_score DESC
                  LIMIT ?";

        $stmt = $db->prepare($query);
        $stmt->bindParam(1, $limit, PDO::PARAM_INT);
        $stmt->execute();

        $rankings = [];
        $rank = 1;

        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $rankings[] = [
                'rank' => $rank++,
                'user_id' => $row['id'],
                'user_name' => $row['name'],
                'district' => $row['district'],
                'total_tests' => $row['total_tests'],
                'avg_score' => round($row['avg_score'], 2),
                'best_score' => $row['best_score'],
                'total_score' => $row['total_score']
            ];
        }

        http_response_code(200);
        echo json_encode([
            'success' => true,
            'count' => count($rankings),
            'rankings' => $rankings
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
