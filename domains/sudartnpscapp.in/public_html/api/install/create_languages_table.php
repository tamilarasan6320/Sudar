<?php
require_once '../config/database.php';

header('Content-Type: text/html; charset=utf-8');

echo "<!DOCTYPE html>";
echo "<html><head><title>Create Languages Table</title>";
echo "<style>
    body { font-family: Arial, sans-serif; max-width: 800px; margin: 50px auto; padding: 20px; }
    .success { color: green; padding: 10px; background: #d4edda; border: 1px solid green; margin: 10px 0; }
    .error { color: red; padding: 10px; background: #f8d7da; border: 1px solid red; margin: 10px 0; }
    .info { color: blue; padding: 10px; background: #d1ecf1; border: 1px solid blue; margin: 10px 0; }
    pre { background: #f4f4f4; padding: 10px; border-radius: 5px; }
</style></head><body>";

echo "<h1>🗣️ Create Languages Table</h1>";

try {
    $database = new Database();
    $conn = $database->getConnection();
    
    if (!$conn) {
        throw new Exception("Database connection failed");
    }
    
    echo "<div class='info'>✅ Database connection successful!</div>";
    
    // Drop table if exists
    echo "<h3>Step 1: Dropping existing table (if any)...</h3>";
    $conn->exec("DROP TABLE IF EXISTS languages");
    echo "<div class='success'>✅ Existing table dropped</div>";
    
    // Create languages table
    echo "<h3>Step 2: Creating languages table...</h3>";
    $sql = "CREATE TABLE languages (
        id INT AUTO_INCREMENT PRIMARY KEY,
        exam_category_id INT NOT NULL,
        name VARCHAR(100) NOT NULL,
        code VARCHAR(10) NOT NULL,
        icon VARCHAR(50) DEFAULT '🌐',
        is_active TINYINT(1) DEFAULT 1,
        display_order INT DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (exam_category_id) REFERENCES exam_categories(id) ON DELETE CASCADE,
        UNIQUE KEY unique_exam_language (exam_category_id, code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    
    $conn->exec($sql);
    echo "<div class='success'>✅ Languages table created successfully!</div>";
    
    // Insert sample languages
    echo "<h3>Step 3: Inserting sample languages...</h3>";
    
    // First, check if we have exam categories
    $stmt = $conn->query("SELECT id, name FROM exam_categories LIMIT 1");
    $examCategory = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($examCategory) {
        $examCategoryId = $examCategory['id'];
        $examCategoryName = $examCategory['name'];
        
        echo "<div class='info'>Found exam category: {$examCategoryName} (ID: {$examCategoryId})</div>";
        
        $sampleLanguages = [
            ['name' => 'English', 'code' => 'en', 'icon' => '🇬🇧', 'order' => 1],
            ['name' => 'தமிழ் (Tamil)', 'code' => 'ta', 'icon' => '🇮🇳', 'order' => 2]
        ];
        
        $insertStmt = $conn->prepare("
            INSERT INTO languages (exam_category_id, name, code, icon, display_order) 
            VALUES (?, ?, ?, ?, ?)
        ");
        
        foreach ($sampleLanguages as $lang) {
            $insertStmt->execute([
                $examCategoryId,
                $lang['name'],
                $lang['code'],
                $lang['icon'],
                $lang['order']
            ]);
            echo "<div class='success'>✅ Added language: {$lang['name']} ({$lang['code']})</div>";
        }
    } else {
        echo "<div class='info'>⚠️ No exam categories found. Please add exam categories first to see sample languages.</div>";
    }
    
    // Show table structure
    echo "<h3>Step 4: Table Structure</h3>";
    echo "<pre>";
    echo "Table: languages\n";
    echo "Columns:\n";
    echo "  - id (INT, PRIMARY KEY, AUTO_INCREMENT)\n";
    echo "  - exam_category_id (INT, FOREIGN KEY)\n";
    echo "  - name (VARCHAR(100)) - e.g., 'English', 'தமிழ்'\n";
    echo "  - code (VARCHAR(10)) - e.g., 'en', 'ta'\n";
    echo "  - icon (VARCHAR(50)) - e.g., '🇬🇧', '🇮🇳'\n";
    echo "  - is_active (TINYINT(1)) - 1=active, 0=inactive\n";
    echo "  - display_order (INT) - for sorting\n";
    echo "  - created_at (TIMESTAMP)\n";
    echo "  - updated_at (TIMESTAMP)\n";
    echo "</pre>";
    
    // Show current data
    echo "<h3>Step 5: Current Languages</h3>";
    $stmt = $conn->query("
        SELECT l.*, ec.name as exam_name 
        FROM languages l 
        LEFT JOIN exam_categories ec ON l.exam_category_id = ec.id
        ORDER BY l.exam_category_id, l.display_order
    ");
    $languages = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    if (count($languages) > 0) {
        echo "<table border='1' cellpadding='10' style='border-collapse: collapse; width: 100%;'>";
        echo "<tr style='background: #f0f0f0;'>
                <th>ID</th>
                <th>Exam</th>
                <th>Language</th>
                <th>Code</th>
                <th>Icon</th>
                <th>Active</th>
                <th>Order</th>
              </tr>";
        foreach ($languages as $lang) {
            $active = $lang['is_active'] ? '✅' : '❌';
            echo "<tr>
                    <td>{$lang['id']}</td>
                    <td>{$lang['exam_name']}</td>
                    <td>{$lang['name']}</td>
                    <td>{$lang['code']}</td>
                    <td>{$lang['icon']}</td>
                    <td>{$active}</td>
                    <td>{$lang['display_order']}</td>
                  </tr>";
        }
        echo "</table>";
    } else {
        echo "<div class='info'>No languages found. Add exam categories first!</div>";
    }
    
    echo "<div class='success' style='margin-top: 30px;'>
            <h3>✅ Languages table setup complete!</h3>
            <p><a href='../admin/index.html?v=" . time() . "#languages'>Go to Admin Panel - Languages</a></p>
          </div>";
    
} catch (Exception $e) {
    echo "<div class='error'>❌ Error: " . $e->getMessage() . "</div>";
    echo "<div class='error'><pre>" . $e->getTraceAsString() . "</pre></div>";
}

echo "</body></html>";
?>


