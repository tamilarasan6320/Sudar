<?php
/**
 * Get Trial Test API
 * 
 * Returns 10 random questions from different categories for trial/demo purposes.
 * Questions are automatically generated from the existing question database.
 */

require_once '../config/cors.php';
require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $language = isset($_GET['language']) ? $_GET['language'] : 'en';
    $exam_id = isset($_GET['exam_id']) ? intval($_GET['exam_id']) : null;
    $question_count = isset($_GET['count']) ? min(intval($_GET['count']), 20) : 10;

    try {
        // First, get all active test categories (optionally filtered by exam)
        $categoryQuery = "SELECT tc.id, tc.name 
                          FROM test_categories tc 
                          WHERE tc.is_active = 1";
        
        if ($exam_id) {
            $categoryQuery .= " AND tc.exam_category_id = :exam_id";
        }
        
        $categoryQuery .= " ORDER BY RAND() LIMIT 10";
        
        $categoryStmt = $db->prepare($categoryQuery);
        if ($exam_id) {
            $categoryStmt->bindParam(':exam_id', $exam_id, PDO::PARAM_INT);
        }
        $categoryStmt->execute();
        $categories = $categoryStmt->fetchAll(PDO::FETCH_ASSOC);
        
        if (empty($categories)) {
            http_response_code(200);
            echo json_encode([
                'success' => false,
                'message' => 'No test categories available'
            ]);
            exit;
        }
        
        $questions = [];
        $categoryIds = array_column($categories, 'id');
        $categoryNames = [];
        foreach ($categories as $cat) {
            $categoryNames[$cat['id']] = $cat['name'];
        }
        
        // Strategy: Get random questions distributed across categories
        // First try to get 1 question from each category, then fill remaining randomly
        $questionsPerCategory = max(1, floor($question_count / count($categoryIds)));
        $remainingCount = $question_count;
        
        // Collect questions from each category
        foreach ($categoryIds as $catId) {
            if ($remainingCount <= 0) break;
            
            $limit = min($questionsPerCategory, $remainingCount);
            
            $query = "SELECT q.*, tc.name as category_name
                      FROM questions q
                      INNER JOIN question_sessions qs ON q.session_id = qs.id
                      INNER JOIN test_categories tc ON qs.test_category_id = tc.id
                      WHERE qs.test_category_id = :cat_id 
                      AND qs.is_active = 1
                      AND (q.question_en IS NOT NULL AND q.question_en != '' 
                           OR q.question_ta IS NOT NULL AND q.question_ta != '')
                      ORDER BY RAND()
                      LIMIT :limit";
            
            $stmt = $db->prepare($query);
            $stmt->bindParam(':cat_id', $catId, PDO::PARAM_INT);
            $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
            $stmt->execute();
            
            while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                $questions[] = $row;
                $remainingCount--;
            }
        }
        
        // If we still need more questions, get random ones from any category
        if ($remainingCount > 0) {
            $existingIds = array_column($questions, 'id');
            $excludeIds = !empty($existingIds) ? implode(',', $existingIds) : '0';
            
            $fillQuery = "SELECT q.*, tc.name as category_name
                          FROM questions q
                          INNER JOIN question_sessions qs ON q.session_id = qs.id
                          INNER JOIN test_categories tc ON qs.test_category_id = tc.id
                          WHERE qs.is_active = 1
                          AND q.id NOT IN ($excludeIds)
                          AND (q.question_en IS NOT NULL AND q.question_en != '' 
                               OR q.question_ta IS NOT NULL AND q.question_ta != '')";
            
            if ($exam_id) {
                $fillQuery .= " AND tc.exam_category_id = :exam_id";
            }
            
            $fillQuery .= " ORDER BY RAND() LIMIT :remaining";
            
            $fillStmt = $db->prepare($fillQuery);
            if ($exam_id) {
                $fillStmt->bindParam(':exam_id', $exam_id, PDO::PARAM_INT);
            }
            $fillStmt->bindParam(':remaining', $remainingCount, PDO::PARAM_INT);
            $fillStmt->execute();
            
            while ($row = $fillStmt->fetch(PDO::FETCH_ASSOC)) {
                $questions[] = $row;
            }
        }
        
        // Shuffle final questions for variety
        shuffle($questions);
        
        // Format questions for response
        $formattedQuestions = [];
        $displayOrder = 1;
        $topicsIncluded = [];
        
        foreach ($questions as $row) {
            $hasEnglish = !empty($row['question_en']) && !empty($row['option_a_en']);
            $hasTamil = !empty($row['question_ta']) && !empty($row['option_a_ta']);
            
            // Track unique topics
            if (!empty($row['category_name']) && !in_array($row['category_name'], $topicsIncluded)) {
                $topicsIncluded[] = $row['category_name'];
            }
            
            $formatted = [
                'id' => $row['id'],
                'session_id' => $row['session_id'],
                'correct_answer' => $row['correct_answer'],
                'difficulty' => $row['difficulty'] ?? 'medium',
                'marks' => $row['marks'] ?? 1,
                'negative_marks' => $row['negative_marks'] ?? 0,
                'display_order' => $displayOrder++,
                'category_name' => $row['category_name'] ?? 'General',
                
                // English version
                'question_en' => $row['question_en'] ?? null,
                'option_a_en' => $row['option_a_en'] ?? null,
                'option_b_en' => $row['option_b_en'] ?? null,
                'option_c_en' => $row['option_c_en'] ?? null,
                'option_d_en' => $row['option_d_en'] ?? null,
                'explanation_en' => $row['explanation_en'] ?? null,
                
                // Tamil version
                'question_ta' => $row['question_ta'] ?? null,
                'option_a_ta' => $row['option_a_ta'] ?? null,
                'option_b_ta' => $row['option_b_ta'] ?? null,
                'option_c_ta' => $row['option_c_ta'] ?? null,
                'option_d_ta' => $row['option_d_ta'] ?? null,
                'explanation_ta' => $row['explanation_ta'] ?? null,
                
                // Language availability flags
                'has_english' => $hasEnglish,
                'has_tamil' => $hasTamil
            ];

            $formattedQuestions[] = $formatted;
        }

        http_response_code(200);
        echo json_encode([
            'success' => true,
            'is_trial' => true,
            'count' => count($formattedQuestions),
            'topics_count' => count($topicsIncluded),
            'topics' => $topicsIncluded,
            'language' => $language,
            'trial_info' => [
                'title' => 'Trial Test',
                'description' => 'Experience our test quality with ' . count($formattedQuestions) . ' questions from ' . count($topicsIncluded) . ' different topics',
                'duration' => 15, // 15 minutes for trial
                'total_marks' => count($formattedQuestions)
            ],
            'questions' => $formattedQuestions
        ]);

    } catch (PDOException $e) {
        error_log("Trial test error: " . $e->getMessage());
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Failed to generate trial test'
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
