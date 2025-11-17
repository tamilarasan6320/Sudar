<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $type = isset($_GET['type']) ? $_GET['type'] : 'test_results'; // test_results, users, analytics
    $format = isset($_GET['format']) ? $_GET['format'] : 'csv'; // csv, json, excel
    $start_date = isset($_GET['start_date']) ? $_GET['start_date'] : null;
    $end_date = isset($_GET['end_date']) ? $_GET['end_date'] : null;
    $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : null;
    $category_id = isset($_GET['category_id']) ? intval($_GET['category_id']) : null;

    try {
        switch ($type) {
            case 'test_results':
                $where_conditions = ["1=1"];
                $params = [];

                if ($start_date) {
                    $where_conditions[] = "DATE(tr.submitted_at) >= ?";
                    $params[] = $start_date;
                }
                if ($end_date) {
                    $where_conditions[] = "DATE(tr.submitted_at) <= ?";
                    $params[] = $end_date;
                }
                if ($user_id) {
                    $where_conditions[] = "tr.user_id = ?";
                    $params[] = $user_id;
                }
                if ($category_id) {
                    $where_conditions[] = "qs.test_category_id = ?";
                    $params[] = $category_id;
                }

                $where_clause = implode(" AND ", $where_conditions);

                $query = "SELECT tr.id,
                                 u.name as user_name,
                                 u.mobile as user_mobile,
                                 ec.name as exam_name,
                                 tc.name as category_name,
                                 qs.name as session_name,
                                 tr.total_questions,
                                 tr.correct_answers,
                                 tr.wrong_answers,
                                 tr.percentage,
                                 tr.time_taken,
                                 tr.submitted_at
                          FROM test_results tr
                          LEFT JOIN users u ON tr.user_id = u.id
                          LEFT JOIN question_sessions qs ON tr.session_id = qs.id
                          LEFT JOIN test_categories tc ON qs.test_category_id = tc.id
                          LEFT JOIN exam_categories ec ON tc.exam_category_id = ec.id
                          WHERE $where_clause
                          ORDER BY tr.submitted_at DESC";
                
                $stmt = $db->prepare($query);
                $stmt->execute($params);
                $data = $stmt->fetchAll(PDO::FETCH_ASSOC);

                if ($format === 'csv') {
                    header('Content-Type: text/csv');
                    header('Content-Disposition: attachment; filename="test_results_' . date('Y-m-d') . '.csv"');
                    
                    $output = fopen('php://output', 'w');
                    fputcsv($output, ['ID', 'User Name', 'Mobile', 'Exam', 'Category', 'Session', 'Total Questions', 'Correct', 'Wrong', 'Percentage', 'Time Taken (sec)', 'Submitted At']);
                    
                    foreach ($data as $row) {
                        fputcsv($output, [
                            $row['id'],
                            $row['user_name'],
                            $row['user_mobile'],
                            $row['exam_name'],
                            $row['category_name'],
                            $row['session_name'],
                            $row['total_questions'],
                            $row['correct_answers'],
                            $row['wrong_answers'],
                            $row['percentage'],
                            $row['time_taken'],
                            $row['submitted_at']
                        ]);
                    }
                    fclose($output);
                    exit;
                } else {
                    http_response_code(200);
                    echo json_encode([
                        'success' => true,
                        'type' => $type,
                        'format' => $format,
                        'count' => count($data),
                        'data' => $data
                    ]);
                }
                break;

            case 'users':
                $query = "SELECT u.id,
                                 u.name,
                                 u.mobile,
                                 u.email,
                                 u.created_at,
                                 u.last_login,
                                 COUNT(tr.id) as total_tests,
                                 AVG(tr.percentage) as avg_score
                          FROM users u
                          LEFT JOIN test_results tr ON u.id = tr.user_id
                          GROUP BY u.id, u.name, u.mobile, u.email, u.created_at, u.last_login
                          ORDER BY u.created_at DESC";
                
                $stmt = $db->query($query);
                $data = $stmt->fetchAll(PDO::FETCH_ASSOC);

                if ($format === 'csv') {
                    header('Content-Type: text/csv');
                    header('Content-Disposition: attachment; filename="users_' . date('Y-m-d') . '.csv"');
                    
                    $output = fopen('php://output', 'w');
                    fputcsv($output, ['ID', 'Name', 'Mobile', 'Email', 'Total Tests', 'Avg Score', 'Created At', 'Last Login']);
                    
                    foreach ($data as $row) {
                        fputcsv($output, [
                            $row['id'],
                            $row['name'],
                            $row['mobile'],
                            $row['email'] ?? '',
                            $row['total_tests'],
                            round($row['avg_score'] ?? 0, 1),
                            $row['created_at'],
                            $row['last_login'] ?? ''
                        ]);
                    }
                    fclose($output);
                    exit;
                } else {
                    http_response_code(200);
                    echo json_encode([
                        'success' => true,
                        'type' => $type,
                        'format' => $format,
                        'count' => count($data),
                        'data' => $data
                    ]);
                }
                break;

            case 'analytics':
                // Export analytics summary
                $analytics = [
                    'total_users' => $db->query("SELECT COUNT(*) FROM users")->fetchColumn(),
                    'total_tests' => $db->query("SELECT COUNT(*) FROM test_results")->fetchColumn(),
                    'avg_score' => round($db->query("SELECT AVG(percentage) FROM test_results")->fetchColumn(), 1),
                    'total_questions' => $db->query("SELECT COUNT(*) FROM questions")->fetchColumn(),
                    'export_date' => date('Y-m-d H:i:s')
                ];

                if ($format === 'json') {
                    http_response_code(200);
                    echo json_encode([
                        'success' => true,
                        'type' => $type,
                        'format' => $format,
                        'data' => $analytics
                    ]);
                } else {
                    http_response_code(400);
                    echo json_encode(['success' => false, 'message' => 'Analytics export only supports JSON format']);
                }
                break;

            default:
                throw new Exception('Invalid export type');
        }

    } catch (Exception $e) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => $e->getMessage()
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed.']);
}
?>

