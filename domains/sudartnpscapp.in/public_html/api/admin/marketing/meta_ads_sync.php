<?php
/**
 * Meta Ads Sync Endpoint
 * 
 * Fetches insights from Meta Marketing API and stores in database
 * 
 * POST /api/admin/marketing/meta_ads_sync.php
 * Body: {
 *   "start_date": "YYYY-MM-DD",  // Optional, defaults to 30 days ago
 *   "end_date": "YYYY-MM-DD",    // Optional, defaults to today
 *   "level": "campaign"          // Optional: campaign, adset, ad
 * }
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../auth/middleware.php'; // Require admin authentication
require_once '../../services/MetaAdsService.php';

header('Content-Type: application/json');

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        // Parse request body
        $data = json_decode(file_get_contents('php://input'), true);
        
        // Get parameters with defaults
        $startDate = $data['start_date'] ?? date('Y-m-d', strtotime('-30 days'));
        $endDate = $data['end_date'] ?? date('Y-m-d');
        $level = $data['level'] ?? 'campaign';
        
        // Validate dates
        $startObj = DateTime::createFromFormat('Y-m-d', $startDate);
        $endObj = DateTime::createFromFormat('Y-m-d', $endDate);
        
        if (!$startObj || $startObj->format('Y-m-d') !== $startDate) {
            throw new Exception('Invalid start_date. Use YYYY-MM-DD format.');
        }
        if (!$endObj || $endObj->format('Y-m-d') !== $endDate) {
            throw new Exception('Invalid end_date. Use YYYY-MM-DD format.');
        }
        if ($startObj > $endObj) {
            throw new Exception('start_date cannot be after end_date.');
        }
        
        // Validate level
        $validLevels = ['campaign', 'adset', 'ad'];
        if (!in_array($level, $validLevels)) {
            throw new Exception('Invalid level. Must be one of: ' . implode(', ', $validLevels));
        }
        
        // Check date range (max 90 days per request)
        $diffDays = (int)$startObj->diff($endObj)->format('%a');
        if ($diffDays > 90) {
            throw new Exception('Date range too large. Maximum 90 days per sync request.');
        }
        
        // Initialize service and run sync
        $metaService = new MetaAdsService($db);
        
        // Check if configured
        if (!$metaService->isConfigured()) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Meta Ads not configured. Please update api/config/meta_ads.php with your credentials.',
                'config_required' => [
                    'META_AD_ACCOUNT_ID' => 'Your Ad Account ID (e.g., act_123456789)',
                    'META_ACCESS_TOKEN' => 'System User access token with ads_read, read_insights permissions'
                ]
            ]);
            exit;
        }
        
        // Run sync
        $result = $metaService->syncInsights($startDate, $endDate, $level);
        
        if (!$result['success']) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Sync failed: ' . $result['error'],
                'synced' => $result['synced'] ?? 0
            ]);
            exit;
        }
        
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => 'Sync completed successfully',
            'data' => [
                'start_date' => $startDate,
                'end_date' => $endDate,
                'level' => $level,
                'rows_synced' => $result['synced'],
                'total_fetched' => $result['total_fetched'],
                'total_spend' => round($result['total_spend'], 2),
                'pages_fetched' => $result['pages_fetched']
            ]
        ]);
        
    } catch (Exception $e) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => $e->getMessage()
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed. Use POST.'
    ]);
}
?>
