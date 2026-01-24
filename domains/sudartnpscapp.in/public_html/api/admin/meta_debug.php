<?php
/**
 * Meta API Debug - Shows raw data from Meta
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../config/meta_ads.php';

// Date range (default: last 7 days)
$startDate = $_GET['start_date'] ?? date('Y-m-d', strtotime('-7 days'));
$endDate = $_GET['end_date'] ?? date('Y-m-d');

if (!defined('META_ACCESS_TOKEN') || !defined('META_AD_ACCOUNT_ID')) {
    echo json_encode(['error' => 'Meta credentials not configured']);
    exit;
}

$accessToken = META_ACCESS_TOKEN;
$adAccountId = META_AD_ACCOUNT_ID;
$apiUrl = META_GRAPH_API_URL;

// Fetch account-level summary (what we use for totals)
$fields = 'spend,impressions,reach,clicks,cpc,cpm,ctr,actions,cost_per_action_type';
$params = [
    'access_token' => $accessToken,
    'level' => 'account',
    'fields' => $fields,
    'time_range' => json_encode([
        'since' => $startDate,
        'until' => $endDate
    ])
];

$url = $apiUrl . '/' . $adAccountId . '/insights?' . http_build_query($params);

$ch = curl_init();
curl_setopt_array($ch, [
    CURLOPT_URL => $url,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_TIMEOUT => 60,
    CURLOPT_SSL_VERIFYPEER => true
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

$data = json_decode($response, true);

// Format actions and cost_per_action_type for easy reading
$output = [
    'date_range' => "$startDate to $endDate",
    'http_code' => $httpCode,
    'raw_response' => $data
];

if (isset($data['data'][0])) {
    $row = $data['data'][0];
    
    $output['summary'] = [
        'spend' => $row['spend'] ?? null,
        'impressions' => $row['impressions'] ?? null,
        'clicks' => $row['clicks'] ?? null,
        'cpc' => $row['cpc'] ?? null,
        'cpm' => $row['cpm'] ?? null,
        'ctr' => $row['ctr'] ?? null
    ];
    
    // List all actions
    $output['actions'] = [];
    if (isset($row['actions'])) {
        foreach ($row['actions'] as $action) {
            $output['actions'][$action['action_type']] = $action['value'];
        }
    }
    
    // List all cost_per_action_type
    $output['cost_per_action_type'] = [];
    if (isset($row['cost_per_action_type'])) {
        foreach ($row['cost_per_action_type'] as $cost) {
            $output['cost_per_action_type'][$cost['action_type']] = $cost['value'];
        }
    }
    
    // What Meta shows as "Results" in Ads Manager (based on objective)
    $output['meta_results_guide'] = [
        'mobile_app_install' => [
            'count' => $output['actions']['mobile_app_install'] ?? 'N/A',
            'cost' => $output['cost_per_action_type']['mobile_app_install'] ?? 'N/A'
        ],
        'omni_app_install' => [
            'count' => $output['actions']['omni_app_install'] ?? 'N/A',
            'cost' => $output['cost_per_action_type']['omni_app_install'] ?? 'N/A'
        ],
        'app_custom_event.fb_mobile_purchase' => [
            'count' => $output['actions']['app_custom_event.fb_mobile_purchase'] ?? 'N/A',
            'cost' => $output['cost_per_action_type']['app_custom_event.fb_mobile_purchase'] ?? 'N/A'
        ],
        'omni_purchase' => [
            'count' => $output['actions']['omni_purchase'] ?? 'N/A',
            'cost' => $output['cost_per_action_type']['omni_purchase'] ?? 'N/A'
        ]
    ];
}

echo json_encode($output, JSON_PRETTY_PRINT);
