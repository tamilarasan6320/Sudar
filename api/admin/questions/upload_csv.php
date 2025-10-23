<?php
require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../../models/Question.php';
require_once '../../models/QuestionSession.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $session_id = isset($_POST['session_id']) ? intval($_POST['session_id']) : 0;
    
    if ($session_id <= 0) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Session ID is required']);
        exit;
    }
    
    if (!isset($_FILES['csv_file']) || $_FILES['csv_file']['error'] !== UPLOAD_ERR_OK) {
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'CSV file is required']);
        exit;
    }
    
    $file = $_FILES['csv_file']['tmp_name'];
    
    try {
        $db->beginTransaction();
        
        $handle = fopen($file, 'r');
        if ($handle === false) {
            throw new Exception('Unable to read CSV file');
        }
        
        // Skip header row
        $header = fgetcsv($handle);
        
        $added = 0;
        $skipped = 0;
        $errors = [];
        $row_num = 1;
        
        while (($data = fgetcsv($handle)) !== false) {
            $row_num++;
            
            // Skip empty rows
            if (empty(array_filter($data))) {
                $skipped++;
                continue;
            }
            
            // Validate minimum required fields
            if (count($data) < 13) {
                $errors[] = "Row $row_num: Insufficient columns";
                $skipped++;
                continue;
            }
            
            $question = new Question($db);
            $question->session_id = $session_id;
            $question->question_en = !empty($data[0]) ? trim($data[0]) : null;
            $question->option_a_en = !empty($data[1]) ? trim($data[1]) : null;
            $question->option_b_en = !empty($data[2]) ? trim($data[2]) : null;
            $question->option_c_en = !empty($data[3]) ? trim($data[3]) : null;
            $question->option_d_en = !empty($data[4]) ? trim($data[4]) : null;
            $question->correct_answer = !empty($data[5]) ? strtoupper(trim($data[5])) : 'A';
            $question->explanation_en = !empty($data[6]) ? trim($data[6]) : null;
            $question->question_ta = !empty($data[7]) ? trim($data[7]) : null;
            $question->option_a_ta = !empty($data[8]) ? trim($data[8]) : null;
            $question->option_b_ta = !empty($data[9]) ? trim($data[9]) : null;
            $question->option_c_ta = !empty($data[10]) ? trim($data[10]) : null;
            $question->option_d_ta = !empty($data[11]) ? trim($data[11]) : null;
            $question->explanation_ta = !empty($data[12]) ? trim($data[12]) : null;
            $question->difficulty = 'medium';
            $question->marks = 1;
            $question->negative_marks = 0.25;
            $question->display_order = $added;
            
            // Check if at least one language version exists
            if (empty($question->question_en) && empty($question->question_ta)) {
                $errors[] = "Row $row_num: Question text required in at least one language";
                $skipped++;
                continue;
            }
            
            // Validate correct answer
            if (!in_array($question->correct_answer, ['A', 'B', 'C', 'D'])) {
                $errors[] = "Row $row_num: Correct answer must be A, B, C, or D";
                $skipped++;
                continue;
            }
            
            // Check for duplicates
            if ($question->checkDuplicate()) {
                $errors[] = "Row $row_num: Duplicate question found";
                $skipped++;
                continue;
            }
            
            if ($question->create()) {
                $added++;
            } else {
                $errors[] = "Row $row_num: Failed to insert question";
                $skipped++;
            }
        }
        
        fclose($handle);
        
        // Update session question count
        $session = new QuestionSession($db);
        $session->id = $session_id;
        $session->updateQuestionCount();
        
        $db->commit();
        
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => "Successfully uploaded $added questions",
            'added' => $added,
            'skipped' => $skipped,
            'errors' => $errors
        ]);
        
    } catch (Exception $e) {
        $db->rollBack();
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Error processing CSV file',
            'error' => $e->getMessage()
        ]);
    }
    
} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
}
?>

