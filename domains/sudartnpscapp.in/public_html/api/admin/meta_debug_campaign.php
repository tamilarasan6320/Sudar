<?php
/**
 * Meta API Debug - Campaign Level Data
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

require_once __DIR__ . '/../config/meta_ads.php';

$startDate = $_GET['start_date'] ?? date('Y-m-d');
$endDate = $_GET['end_date'] ?? date('Y-m-d');

$accessToken = META_ACCESS_TOKEN;
$adAccountId = META_AD_ACCOUNT_ID;
$apiUrl = META_GRAPH_API_URL;

// Fetch CAMPAIGN-level data (what we sync to DB)
$fields = 'campaign_id,campaign_name,objective,optimization_goal,spend,impressions,clicks,cpc,cpm,ctr,actions,cost_per_action_type';
$params = [
    'access_token' => $accessToken,
    'level' => 'campaign',
    'fields' => $fields,
    'time_range' => json_encode([
        'since' => $startDate,
        'until' => $endDate
    ]),
    'time_increment' => 1
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
curl_close($ch);

$data = json_decode($response, true);

$output = [
    'date_range' => "$startDate to $endDate",
    'campaigns' => []
];

if (isset($data['data'])) {
    foreach ($data['data'] as $row) {
        $campaign = [
            'campaign_id' => $row['campaign_id'] ?? null,
            'campaign_name' => $row['campaign_name'] ?? null,
            'objective' => $row['objective'] ?? null,
            'optimization_goal' => $row['optimization_goal'] ?? null,
            'date' => $row['date_start'] ?? null,
            'spend' => $row['spend'] ?? 0,
            'impressions' => $row['impressions'] ?? 0,
            'clicks' => $row['clicks'] ?? 0,
            'cpc' => $row['cpc'] ?? null,
            'actions' => [],
            'cost_per_action_type' => []
        ];
        
        // Parse actions
        if (isset($row['actions'])) {
            foreach ($row['actions'] as $action) {
                $campaign['actions'][$action['action_type']] = $action['value'];
            }
        }
        
        // Parse cost_per_action_type
        if (isset($row['cost_per_action_type'])) {
            foreach ($row['cost_per_action_type'] as $cost) {
                $campaign['cost_per_action_type'][$cost['action_type']] = $cost['value'];
            }
        }
        
        // Show what we SHOULD use for Results/Cost
        $campaign['recommended'] = [
            'omni_app_install' => [
                'results' => $campaign['actions']['omni_app_install'] ?? 'N/A',
                'cost' => $campaign['cost_per_action_type']['omni_app_install'] ?? 'N/A'
            ],
            'omni_purchase' => [
                'results' => $campaign['actions']['omni_purchase'] ?? 'N/A',
                'cost' => $campaign['cost_per_action_type']['omni_purchase'] ?? 'N/A'
            ]
        ];
        
        $output['campaigns'][] = $campaign;
    }
}

echo json_encode($output, JSON_PRETTY_PRINT);
