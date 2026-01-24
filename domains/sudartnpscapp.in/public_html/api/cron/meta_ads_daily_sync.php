<?php
/**
 * Meta Ads Daily Sync Cron Script
 * 
 * This script syncs Meta Ads insights daily.
 * 
 * Setup options:
 * 
 * 1. Hostinger Cron Job:
 *    Command: php /home/u747149096/domains/sudartnpscapp.in/public_html/api/cron/meta_ads_daily_sync.php
 *    Schedule: Daily at 6:00 AM IST
 * 
 * 2. Windows Task Scheduler:
 *    Command: C:\xampp\php\php.exe C:\xampp\htdocs\MockTest\public_html\api\cron\meta_ads_daily_sync.php
 * 
 * 3. cURL call (with secret key):
 *    GET /api/cron/meta_ads_daily_sync.php?key=YOUR_CRON_SECRET_KEY
 */

// Set timezone
date_default_timezone_set('Asia/Kolkata');

// CLI or HTTP mode
$isCli = php_sapi_name() === 'cli';

// Security: Verify cron key for HTTP requests
if (!$isCli) {
    header('Content-Type: application/json');
    
    $cronKey = $_GET['key'] ?? '';
    $expectedKey = 'tnpsc_meta_cron_2025'; // Change this to a secure random key
    
    if ($cronKey !== $expectedKey) {
        http_response_code(403);
        echo json_encode(['success' => false, 'message' => 'Invalid cron key']);
        exit;
    }
}

// Load dependencies
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../services/MetaAdsService.php';

// Output helper
function output($message, $isError = false) {
    global $isCli;
    $timestamp = date('Y-m-d H:i:s');
    
    if ($isCli) {
        echo "[$timestamp] " . ($isError ? "ERROR: " : "") . $message . "\n";
    } else {
        // Store messages for JSON response
        global $logMessages;
        $logMessages[] = [
            'time' => $timestamp,
            'message' => $message,
            'error' => $isError
        ];
    }
}

$logMessages = [];

try {
    output("Starting Meta Ads daily sync...");
    
    // Connect to database
    $database = new Database();
    $db = $database->getConnection();
    
    // Initialize service
    $metaService = new MetaAdsService($db);
    
    // Check if configured
    if (!$metaService->isConfigured()) {
        throw new Exception('Meta Ads not configured. Please update api/config/meta_ads.php');
    }
    
    // Get sync settings
    $daysBack = (int)$metaService->getSetting('sync_days_back', 7);
    $autoSyncEnabled = (int)$metaService->getSetting('auto_sync_enabled', 1);
    
    if (!$autoSyncEnabled) {
        output("Auto sync is disabled. Skipping.");
        exit;
    }
    
    // Calculate date range (last N days)
    $endDate = date('Y-m-d');
    $startDate = date('Y-m-d', strtotime("-{$daysBack} days"));
    
    output("Date range: {$startDate} to {$endDate}");
    output("Syncing campaign-level insights...");
    
    // Run sync
    $result = $metaService->syncInsights($startDate, $endDate, 'campaign');
    
    if (!$result['success']) {
        throw new Exception('Sync failed: ' . ($result['error'] ?? 'Unknown error'));
    }
    
    output("Sync completed successfully!");
    output("Rows synced: {$result['synced']}");
    output("Total spend: ₹" . number_format($result['total_spend'], 2));
    output("Pages fetched: {$result['pages_fetched']}");
    
    // Return success response for HTTP
    if (!$isCli) {
        echo json_encode([
            'success' => true,
            'message' => 'Daily sync completed',
            'data' => [
                'start_date' => $startDate,
                'end_date' => $endDate,
                'rows_synced' => $result['synced'],
                'total_spend' => $result['total_spend']
            ],
            'logs' => $logMessages
        ]);
    }
    
} catch (Exception $e) {
    output($e->getMessage(), true);
    
    if (!$isCli) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => $e->getMessage(),
            'logs' => $logMessages
        ]);
    }
    
    exit(1);
}
?>
