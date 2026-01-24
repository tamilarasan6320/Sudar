<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Include database connection
require_once 'api/config/database.php';

$database = new Database();
$db = $database->getConnection();

$response = [
    'success' => false,
    'message' => '',
    'columns' => [],
    'table_data_exists' => false
];

try {
    // Check if questions table exists
    $query = "SHOW TABLES LIKE 'questions'";
    $stmt = $db->prepare($query);
    $stmt->execute();
    
    if ($stmt->rowCount() === 0) {
        $response['message'] = 'Questions table does not exist!';
        echo json_encode($response);
        exit;
    }
    
    // Get table structure
    $query = "DESCRIBE questions";
    $stmt = $db->prepare($query);
    $stmt->execute();
    
    $columns = [];
    $table_data_exists = false;
    
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $columns[] = [
            'Field' => $row['Field'],
            'Type' => $row['Type'],
            'Null' => $row['Null'],
            'Key' => $row['Key'],
            'Default' => $row['Default'],
            'Extra' => $row['Extra']
        ];
        
        if ($row['Field'] === 'table_data') {
            $table_data_exists = true;
        }
    }
    
    $response['success'] = true;
    $response['message'] = 'Table structure retrieved successfully';
    $response['columns'] = $columns;
    $response['table_data_exists'] = $table_data_exists;
    
    if ($table_data_exists) {
        $response['status'] = '✅ table_data column EXISTS - Tables will work!';
    } else {
        $response['status'] = '❌ table_data column MISSING - Run ALTER TABLE command!';
        $response['fix_sql'] = 'ALTER TABLE questions ADD COLUMN table_data TEXT;';
    }
    
} catch (Exception $e) {
    $response['message'] = 'Error: ' . $e->getMessage();
}

echo json_encode($response, JSON_PRETTY_PRINT);
?>

