<?php
/**
 * BULK SETUP SCRIPT - Auto-create Test Categories & Question Sessions
 * Upload this file to your server and run once
 * 
 * Usage: https://sudartnpscapp.in/api/admin/bulk_setup.php
 */

require_once '../config/cors.php';
require_once '../config/database.php';

header('Content-Type: application/json');

$database = new Database();
$db = $database->getConnection();

if (!$db) {
    die(json_encode(['success' => false, 'message' => 'Database connection failed']));
}

// Get TNPSC Group 4 exam_category_id
$stmt = $db->query("SELECT id FROM exam_categories WHERE name LIKE '%TNPSC Group 4%' OR name LIKE '%Group 4%' LIMIT 1");
$exam = $stmt->fetch(PDO::FETCH_ASSOC);
$exam_category_id = $exam ? $exam['id'] : 1;

echo "<pre>";
echo "🔧 BULK SETUP SCRIPT\n";
echo "════════════════════════════════════════════\n";
echo "📍 Exam Category ID: $exam_category_id\n\n";

// ═══════════════════════════════════════════════════════════════
// STEP 1: CREATE TEST CATEGORIES
// ═══════════════════════════════════════════════════════════════

$testCategories = [
    ['name' => 'Botany & Zoology', 'description' => 'Life Sciences MCQ Questions', 'icon' => 'fas fa-leaf', 'color' => '#4CAF50'],
    ['name' => 'Geography', 'description' => 'Geography of India MCQ Questions', 'icon' => 'fas fa-globe', 'color' => '#2196F3'],
    ['name' => 'History', 'description' => 'History & Culture of India MCQ Questions', 'icon' => 'fas fa-landmark', 'color' => '#FF9800'],
    ['name' => 'Indian Polity', 'description' => 'Constitution & Polity MCQ Questions', 'icon' => 'fas fa-balance-scale', 'color' => '#9C27B0'],
];

echo "📁 STEP 1: Creating Test Categories...\n";
echo "────────────────────────────────────────────\n";

$categoryIds = [];

foreach ($testCategories as $cat) {
    // Check if already exists
    $check = $db->prepare("SELECT id FROM test_categories WHERE name = ? AND exam_category_id = ?");
    $check->execute([$cat['name'], $exam_category_id]);
    $existing = $check->fetch(PDO::FETCH_ASSOC);
    
    if ($existing) {
        $categoryIds[$cat['name']] = $existing['id'];
        echo "⏭️  {$cat['name']} already exists (ID: {$existing['id']})\n";
    } else {
        $stmt = $db->prepare("INSERT INTO test_categories (exam_category_id, name, description, icon, color, is_active, display_order) VALUES (?, ?, ?, ?, ?, 1, 0)");
        if ($stmt->execute([$exam_category_id, $cat['name'], $cat['description'], $cat['icon'], $cat['color']])) {
            $categoryIds[$cat['name']] = $db->lastInsertId();
            echo "✅ Created: {$cat['name']} (ID: {$categoryIds[$cat['name']]})\n";
        } else {
            echo "❌ Failed: {$cat['name']}\n";
        }
    }
}

// Also get existing categories (Chemistry, Physics, Previous Year)
$existingCats = $db->query("SELECT id, name FROM test_categories WHERE exam_category_id = $exam_category_id");
while ($row = $existingCats->fetch(PDO::FETCH_ASSOC)) {
    if (!isset($categoryIds[$row['name']])) {
        $categoryIds[$row['name']] = $row['id'];
    }
}

echo "\n📊 Category IDs:\n";
print_r($categoryIds);

// ═══════════════════════════════════════════════════════════════
// STEP 2: CREATE QUESTION SESSIONS
// ═══════════════════════════════════════════════════════════════

echo "\n📁 STEP 2: Creating Question Sessions...\n";
echo "────────────────────────────────────────────\n";

// Time formula: Questions × 0.9 = minutes (200 Qs = 180 mins)
function calcDuration($questions) {
    return round($questions * 0.9);
}

$sessions = [
    // Chemistry Sessions
    ['category' => 'Chemistry', 'name' => 'Elements and Compounds', 'questions' => 125],
    ['category' => 'Chemistry', 'name' => 'Acids Base and Salts', 'questions' => 85],
    ['category' => 'Chemistry', 'name' => 'Petroleum Products', 'questions' => 51],
    ['category' => 'Chemistry', 'name' => 'Fertilizers and Pesticides', 'questions' => 61],
    ['category' => 'Chemistry', 'name' => 'Metallurgy and Food', 'questions' => 50],
    
    // Botany & Zoology Sessions
    ['category' => 'Botany & Zoology', 'name' => 'Main Concepts of Life Science', 'questions' => 245],
    ['category' => 'Botany & Zoology', 'name' => 'Classification of Living Organism', 'questions' => 195],
    ['category' => 'Botany & Zoology', 'name' => 'Evolution', 'questions' => 28],
    ['category' => 'Botany & Zoology', 'name' => 'Genetics', 'questions' => 45],
    ['category' => 'Botany & Zoology', 'name' => 'Physiology', 'questions' => 44],
    ['category' => 'Botany & Zoology', 'name' => 'Nutrition', 'questions' => 48],
    ['category' => 'Botany & Zoology', 'name' => 'Health and Hygiene', 'questions' => 44],
    ['category' => 'Botany & Zoology', 'name' => 'Human Diseases', 'questions' => 40],
    ['category' => 'Botany & Zoology', 'name' => 'Environmental Science', 'questions' => 111],
    
    // Geography Sessions
    ['category' => 'Geography', 'name' => 'Location', 'questions' => 40],
    ['category' => 'Geography', 'name' => 'Physical Features', 'questions' => 69],
    ['category' => 'Geography', 'name' => 'Monsoon Weather Climate', 'questions' => 178],
    ['category' => 'Geography', 'name' => 'Water Resources', 'questions' => 80],
    ['category' => 'Geography', 'name' => 'Rivers in India', 'questions' => 80],
    ['category' => 'Geography', 'name' => 'Soil Minerals Natural Resources', 'questions' => 121],
    ['category' => 'Geography', 'name' => 'Forest and Wildlife', 'questions' => 55],
    ['category' => 'Geography', 'name' => 'Agricultural Pattern', 'questions' => 104],
    ['category' => 'Geography', 'name' => 'Transport Communication', 'questions' => 85],
    ['category' => 'Geography', 'name' => 'Population Density', 'questions' => 93],
    ['category' => 'Geography', 'name' => 'Natural Calamity', 'questions' => 84],
    ['category' => 'Geography', 'name' => 'Disaster Management', 'questions' => 45],
    ['category' => 'Geography', 'name' => 'Environmental Pollution', 'questions' => 64],
    ['category' => 'Geography', 'name' => 'Climate Change', 'questions' => 88],
    
    // History Sessions
    ['category' => 'History', 'name' => 'Indus Valley Civilization', 'questions' => 155],
    ['category' => 'History', 'name' => 'Guptas Delhi Sultans', 'questions' => 243],
    ['category' => 'History', 'name' => 'Mughals and Marathas', 'questions' => 154],
    ['category' => 'History', 'name' => 'South Indian History', 'questions' => 143],
    ['category' => 'History', 'name' => 'Indian Culture', 'questions' => 111],
    ['category' => 'History', 'name' => 'Unity in Diversity', 'questions' => 72],
    ['category' => 'History', 'name' => 'Secular State', 'questions' => 50],
    ['category' => 'History', 'name' => 'National Renaissance', 'questions' => 99],
    ['category' => 'History', 'name' => 'Early Uprising Against British', 'questions' => 124],
    ['category' => 'History', 'name' => 'Indian National Congress', 'questions' => 59],
    ['category' => 'History', 'name' => 'Emergence of Leaders', 'questions' => 129],
    ['category' => 'History', 'name' => 'Different Modes of Agitation', 'questions' => 130],
    
    // Indian Polity Sessions
    ['category' => 'Indian Polity', 'name' => 'Constitution of India', 'questions' => 93],
    ['category' => 'Indian Polity', 'name' => 'Preamble', 'questions' => 30],
    ['category' => 'Indian Polity', 'name' => 'Salient Features', 'questions' => 126],
    ['category' => 'Indian Polity', 'name' => 'Union State Territory', 'questions' => 65],
    ['category' => 'Indian Polity', 'name' => 'Citizenship', 'questions' => 67],
    ['category' => 'Indian Polity', 'name' => 'Fundamental Rights', 'questions' => 35],
    ['category' => 'Indian Polity', 'name' => 'Fundamental Duties', 'questions' => 37],
    ['category' => 'Indian Polity', 'name' => 'Directive Principles', 'questions' => 50],
    ['category' => 'Indian Polity', 'name' => 'Union Executive', 'questions' => 63],
    ['category' => 'Indian Polity', 'name' => 'Union Legislature', 'questions' => 74],
    ['category' => 'Indian Polity', 'name' => 'State Executive', 'questions' => 53],
    ['category' => 'Indian Polity', 'name' => 'State Legislature', 'questions' => 48],
    ['category' => 'Indian Polity', 'name' => 'Local Government', 'questions' => 49],
    ['category' => 'Indian Polity', 'name' => 'Panchayat Raj', 'questions' => 31],
    ['category' => 'Indian Polity', 'name' => 'Federalism', 'questions' => 28],
    ['category' => 'Indian Polity', 'name' => 'Elections', 'questions' => 29],
    ['category' => 'Indian Polity', 'name' => 'Judiciary', 'questions' => 140],
    ['category' => 'Indian Polity', 'name' => 'Rule of Law', 'questions' => 29],
    ['category' => 'Indian Polity', 'name' => 'Corruption', 'questions' => 61],
    ['category' => 'Indian Polity', 'name' => 'Anti-corruption', 'questions' => 60],
    ['category' => 'Indian Polity', 'name' => 'Lokpal LokAyukta', 'questions' => 48],
    ['category' => 'Indian Polity', 'name' => 'RTI', 'questions' => 29],
    ['category' => 'Indian Polity', 'name' => 'Women Empowerment', 'questions' => 41],
    ['category' => 'Indian Polity', 'name' => 'Consumer Protection', 'questions' => 51],
    ['category' => 'Indian Polity', 'name' => 'Human Rights', 'questions' => 120],
    ['category' => 'Indian Polity', 'name' => 'Political Parties', 'questions' => 46],
];

$created = 0;
$skipped = 0;

foreach ($sessions as $sess) {
    $catName = $sess['category'];
    
    if (!isset($categoryIds[$catName])) {
        echo "⚠️  Category not found: {$catName}\n";
        continue;
    }
    
    $catId = $categoryIds[$catName];
    $duration = calcDuration($sess['questions']);
    
    // Check if session already exists
    $check = $db->prepare("SELECT id FROM question_sessions WHERE name = ? AND test_category_id = ?");
    $check->execute([$sess['name'], $catId]);
    $existing = $check->fetch(PDO::FETCH_ASSOC);
    
    if ($existing) {
        echo "⏭️  {$sess['name']} already exists\n";
        $skipped++;
    } else {
        $stmt = $db->prepare("INSERT INTO question_sessions (test_category_id, name, description, total_questions, duration, difficulty, is_active) VALUES (?, ?, ?, ?, ?, 'medium', 1)");
        if ($stmt->execute([$catId, $sess['name'], "{$sess['name']} MCQ Questions", $sess['questions'], $duration])) {
            echo "✅ Created: {$sess['name']} ({$sess['questions']} Qs, {$duration} mins)\n";
            $created++;
        } else {
            echo "❌ Failed: {$sess['name']}\n";
        }
    }
}

echo "\n════════════════════════════════════════════\n";
echo "📊 SUMMARY\n";
echo "────────────────────────────────────────────\n";
echo "✅ Sessions Created: $created\n";
echo "⏭️  Sessions Skipped: $skipped\n";
echo "════════════════════════════════════════════\n";
echo "\n🎉 SETUP COMPLETE!\n";
echo "\n📌 NEXT STEP: Go to Admin Panel → Add Question → Upload JSON files to each session\n";
echo "</pre>";

// Return JSON for API calls
if (isset($_GET['json'])) {
    echo json_encode([
        'success' => true,
        'categories_created' => count($testCategories),
        'sessions_created' => $created,
        'sessions_skipped' => $skipped
    ]);
}
?>

