<?php
header('Content-Type: application/json');
require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

$output = [];

// Check table structure
try {
    $columns = $db->query("DESCRIBE subscriptions")->fetchAll(PDO::FETCH_ASSOC);
    $output['columns'] = array_column($columns, 'Field');
} catch (Exception $e) {
    $output['error'] = $e->getMessage();
}

// Get sample data
try {
    $sample = $db->query("SELECT * FROM subscriptions LIMIT 5")->fetchAll(PDO::FETCH_ASSOC);
    $output['sample_data'] = $sample;
} catch (Exception $e) {
    $output['sample_error'] = $e->getMessage();
}

// Get counts
try {
    $output['total_rows'] = $db->query("SELECT COUNT(*) FROM subscriptions")->fetchColumn();
    $output['authenticated_count'] = $db->query("SELECT COUNT(*) FROM subscriptions WHERE status = 'authenticated'")->fetchColumn();
    $output['active_count'] = $db->query("SELECT COUNT(*) FROM subscriptions WHERE status = 'active'")->fetchColumn();
} catch (Exception $e) {
    $output['count_error'] = $e->getMessage();
}

echo json_encode($output, JSON_PRETTY_PRINT);
