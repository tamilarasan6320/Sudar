<?php
/**
 * Meta Ads Insights Endpoint
 * 
 * Returns cached insights from database
 * 
 * GET /api/admin/marketing/meta_ads_insights.php
 * Params:
 *   - start_date: YYYY-MM-DD (optional, defaults to 30 days ago)
 *   - end_date: YYYY-MM-DD (optional, defaults to today)
 *   - level: campaign|adset|ad (optional, defaults to campaign)
 *   - view: list|totals|daily (optional, defaults to list)
 */

require_once '../../config/cors.php';
require_once '../../config/database.php';
require_once '../auth/middleware.php'; // Require admin authentication
require_once '../../services/MetaAdsService.php';

header('Content-Type: application/json');

$database = new Database();
$db = $database->getConnection();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    try {
        // Get parameters with defaults
        $startDate = $_GET['start_date'] ?? date('Y-m-d', strtotime('-30 days'));
        $endDate = $_GET['end_date'] ?? date('Y-m-d');
        $level = $_GET['level'] ?? 'campaign';
        $view = $_GET['view'] ?? 'list'; // list, totals, daily, all
        
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
        
        // Initialize service
        $metaService = new MetaAdsService($db);
        
        // Check if configured
        $isConfigured = $metaService->isConfigured();
        
        // Build response based on view type
        $response = [
            'success' => true,
            'configured' => $isConfigured,
            'params' => [
                'start_date' => $startDate,
                'end_date' => $endDate,
                'level' => $level,
                'view' => $view
            ]
        ];
        
        // Get last sync info
        $lastSync = $metaService->getSetting('last_sync_date');
        $response['last_sync'] = $lastSync;
        
        // Return data based on view type
        if ($view === 'totals' || $view === 'all') {
            $response['totals'] = $metaService->getInsightsTotals($startDate, $endDate, $level);
        }
        
        if ($view === 'daily' || $view === 'all') {
            $response['daily'] = $metaService->getDailyTotals($startDate, $endDate, $level);
        }
        
        if ($view === 'list' || $view === 'all') {
            $insights = $metaService->getInsights($startDate, $endDate, $level);
            $response['insights'] = $insights;
            $response['count'] = count($insights);
        }
        
        // Get recent sync logs (for all views)
        if ($view === 'all') {
            $response['sync_logs'] = $metaService->getSyncLogs(5);
        }
        
        http_response_code(200);
        echo json_encode($response);
        
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
        'message' => 'Method not allowed. Use GET.'
    ]);
}
?>
