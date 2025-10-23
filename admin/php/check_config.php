<?php
/**
 * PHP Configuration Checker
 * Diagnoses issues with scraping setup
 */

header('Content-Type: application/json');

$diagnostics = [
    'php_version' => phpversion(),
    'curl_available' => function_exists('curl_init'),
    'allow_url_fopen' => ini_get('allow_url_fopen') ? true : false,
    'max_execution_time' => ini_get('max_execution_time'),
    'memory_limit' => ini_get('memory_limit'),
    'extensions' => [],
    'curl_test' => null,
    'file_get_contents_test' => null
];

// Check required extensions
$requiredExtensions = ['curl', 'mbstring', 'json', 'dom'];
foreach ($requiredExtensions as $ext) {
    $diagnostics['extensions'][$ext] = extension_loaded($ext);
}

// Test cURL
if (function_exists('curl_init')) {
    $testUrl = 'https://civilserviceaspirants.in';
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $testUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    curl_setopt($ch, CURLOPT_NOBODY, true); // HEAD request only
    
    $result = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);
    
    $diagnostics['curl_test'] = [
        'success' => ($httpCode >= 200 && $httpCode < 400),
        'http_code' => $httpCode,
        'error' => $error
    ];
}

// Test file_get_contents
if (ini_get('allow_url_fopen')) {
    $testUrl = 'https://www.google.com';
    $context = stream_context_create([
        'http' => ['timeout' => 10],
        'ssl' => ['verify_peer' => false]
    ]);
    $result = @file_get_contents($testUrl, false, $context);
    
    $diagnostics['file_get_contents_test'] = [
        'success' => ($result !== false),
        'content_length' => $result ? strlen($result) : 0
    ];
}

// Overall status
$diagnostics['overall_status'] = 'ok';
$diagnostics['issues'] = [];

if (!$diagnostics['curl_available']) {
    $diagnostics['overall_status'] = 'error';
    $diagnostics['issues'][] = 'cURL extension is not available';
}

if (!$diagnostics['allow_url_fopen']) {
    $diagnostics['issues'][] = 'allow_url_fopen is disabled (fallback unavailable)';
}

if ($diagnostics['curl_test'] && !$diagnostics['curl_test']['success']) {
    $diagnostics['overall_status'] = 'warning';
    $diagnostics['issues'][] = 'cURL test failed: ' . $diagnostics['curl_test']['error'];
}

if (!$diagnostics['extensions']['mbstring']) {
    $diagnostics['issues'][] = 'mbstring extension not loaded (required for Tamil text)';
}

echo json_encode($diagnostics, JSON_PRETTY_PRINT);
?>


