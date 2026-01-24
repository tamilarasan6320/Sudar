<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json; charset=UTF-8");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../../config/database.php';
require_once '../../admin/auth/middleware.php'; // Require authentication

$database = new Database();
$conn = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];

try {
    switch ($method) {
        case 'GET':
            // Get all languages or filter by exam_category_id
            $exam_category_id = isset($_GET['exam_category_id']) ? intval($_GET['exam_category_id']) : null;
            
            if ($exam_category_id) {
                $query = "SELECT l.*, ec.name as exam_name 
                         FROM languages l 
                         LEFT JOIN exam_categories ec ON l.exam_category_id = ec.id
                         WHERE l.exam_category_id = ?
                         ORDER BY l.display_order ASC, l.name ASC";
                $stmt = $conn->prepare($query);
                $stmt->execute([$exam_category_id]);
            } else {
                $query = "SELECT l.*, ec.name as exam_name 
                         FROM languages l 
                         LEFT JOIN exam_categories ec ON l.exam_category_id = ec.id
                         ORDER BY l.exam_category_id, l.display_order ASC, l.name ASC";
                $stmt = $conn->prepare($query);
                $stmt->execute();
            }
            
            $languages = $stmt->fetchAll(PDO::FETCH_ASSOC);
            
            echo json_encode([
                'success' => true,
                'data' => $languages,
                'count' => count($languages)
            ]);
            break;
            
        case 'POST':
            // Create new language
            $data = json_decode(file_get_contents("php://input"), true);
            
            if (!isset($data['exam_category_id']) || !isset($data['name']) || !isset($data['code'])) {
                throw new Exception("Missing required fields: exam_category_id, name, code");
            }
            
            $query = "INSERT INTO languages (exam_category_id, name, code, icon, is_active, display_order) 
                     VALUES (?, ?, ?, ?, ?, ?)";
            
            $stmt = $conn->prepare($query);
            $stmt->execute([
                $data['exam_category_id'],
                $data['name'],
                $data['code'],
                isset($data['icon']) ? $data['icon'] : '🌐',
                isset($data['is_active']) ? $data['is_active'] : 1,
                isset($data['display_order']) ? $data['display_order'] : 0
            ]);
            
            $id = $conn->lastInsertId();
            
            echo json_encode([
                'success' => true,
                'message' => 'Language created successfully',
                'id' => $id
            ]);
            break;
            
        case 'PUT':
            // Update language
            $data = json_decode(file_get_contents("php://input"), true);
            
            if (!isset($data['id'])) {
                throw new Exception("Language ID is required");
            }
            
            $updates = [];
            $params = [];
            
            if (isset($data['exam_category_id'])) {
                $updates[] = "exam_category_id = ?";
                $params[] = $data['exam_category_id'];
            }
            if (isset($data['name'])) {
                $updates[] = "name = ?";
                $params[] = $data['name'];
            }
            if (isset($data['code'])) {
                $updates[] = "code = ?";
                $params[] = $data['code'];
            }
            if (isset($data['icon'])) {
                $updates[] = "icon = ?";
                $params[] = $data['icon'];
            }
            if (isset($data['is_active'])) {
                $updates[] = "is_active = ?";
                $params[] = $data['is_active'];
            }
            if (isset($data['display_order'])) {
                $updates[] = "display_order = ?";
                $params[] = $data['display_order'];
            }
            
            if (empty($updates)) {
                throw new Exception("No fields to update");
            }
            
            $params[] = $data['id'];
            $query = "UPDATE languages SET " . implode(", ", $updates) . " WHERE id = ?";
            
            $stmt = $conn->prepare($query);
            $stmt->execute($params);
            
            echo json_encode([
                'success' => true,
                'message' => 'Language updated successfully'
            ]);
            break;
            
        case 'DELETE':
            // Delete language
            $data = json_decode(file_get_contents("php://input"), true);
            
            if (!isset($data['id'])) {
                throw new Exception("Language ID is required");
            }
            
            $query = "DELETE FROM languages WHERE id = ?";
            $stmt = $conn->prepare($query);
            $stmt->execute([$data['id']]);
            
            echo json_encode([
                'success' => true,
                'message' => 'Language deleted successfully'
            ]);
            break;
            
        default:
            throw new Exception("Method not supported");
    }
    
} catch (Exception $e) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
}
?>


