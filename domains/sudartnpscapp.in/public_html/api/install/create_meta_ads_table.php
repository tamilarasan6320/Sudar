<?php
/**
 * Create Meta Ads Insights Tables
 * 
 * Run this once to create the required database tables for Meta Ads integration
 * URL: https://sudartnpscapp.in/api/install/create_meta_ads_table.php
 */

require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

echo "<pre style='font-family: monospace; padding: 20px;'>";
echo "🚀 TNPSC App - Meta Ads Insights Database Setup\n";
echo "================================================\n\n";

try {
    // 1. Create meta_ads_insights_daily table
    $query1 = "CREATE TABLE IF NOT EXISTS meta_ads_insights_daily (
        id INT AUTO_INCREMENT PRIMARY KEY,
        
        -- Date and Level
        day DATE NOT NULL,
        level ENUM('campaign', 'adset', 'ad') DEFAULT 'campaign',
        
        -- Entity IDs (campaign/adset/ad) - use empty string as default for unique key
        campaign_id VARCHAR(50) NOT NULL DEFAULT '',
        campaign_name VARCHAR(255) NULL,
        adset_id VARCHAR(50) NOT NULL DEFAULT '',
        adset_name VARCHAR(255) NULL,
        ad_id VARCHAR(50) NOT NULL DEFAULT '',
        ad_name VARCHAR(255) NULL,
        
        -- Campaign objective/optimization
        objective VARCHAR(100) NULL,
        optimization_goal VARCHAR(100) NULL,
        
        -- Core Metrics
        spend DECIMAL(12,4) DEFAULT 0.0000,
        impressions INT DEFAULT 0,
        reach INT DEFAULT 0,
        clicks INT DEFAULT 0,
        
        -- Calculated Metrics (from Meta)
        cpc DECIMAL(10,4) NULL,
        cpm DECIMAL(10,4) NULL,
        ctr DECIMAL(10,6) NULL,
        
        -- Results (based on campaign objective)
        result_action_type VARCHAR(100) NULL,
        results INT DEFAULT 0,
        cost_per_result DECIMAL(12,4) NULL,
        
        -- Raw JSON data for debugging/audit
        actions_json LONGTEXT NULL,
        cost_per_action_type_json LONGTEXT NULL,
        raw_json LONGTEXT NULL,
        
        -- Sync metadata
        fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        
        -- Unique constraint to prevent duplicates
        UNIQUE KEY unique_daily_insight (day, level, campaign_id, adset_id, ad_id),
        
        -- Indexes for common queries
        INDEX idx_day (day),
        INDEX idx_level (level),
        INDEX idx_campaign (campaign_id),
        INDEX idx_day_level (day, level),
        INDEX idx_spend (spend)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    
    $db->exec($query1);
    echo "✅ Table 'meta_ads_insights_daily' created successfully!\n";

    // 2. Create meta_ads_sync_log table for tracking sync history
    $query2 = "CREATE TABLE IF NOT EXISTS meta_ads_sync_log (
        id INT AUTO_INCREMENT PRIMARY KEY,
        
        -- Sync parameters
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        level ENUM('campaign', 'adset', 'ad') DEFAULT 'campaign',
        
        -- Sync results
        status ENUM('started', 'completed', 'failed') DEFAULT 'started',
        rows_synced INT DEFAULT 0,
        total_spend DECIMAL(12,4) DEFAULT 0.0000,
        
        -- Error info
        error_message TEXT NULL,
        
        -- Timestamps
        started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        completed_at TIMESTAMP NULL,
        
        INDEX idx_dates (start_date, end_date),
        INDEX idx_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    
    $db->exec($query2);
    echo "✅ Table 'meta_ads_sync_log' created successfully!\n";

    // 3. Create meta_ads_settings table for storing account settings
    $query3 = "CREATE TABLE IF NOT EXISTS meta_ads_settings (
        id INT AUTO_INCREMENT PRIMARY KEY,
        setting_key VARCHAR(100) NOT NULL UNIQUE,
        setting_value TEXT NULL,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        
        INDEX idx_key (setting_key)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    
    $db->exec($query3);
    echo "✅ Table 'meta_ads_settings' created successfully!\n";

    // 4. Insert default settings
    $defaultSettings = [
        ['last_sync_date', null],
        ['auto_sync_enabled', '1'],
        ['default_level', 'campaign'],
        ['sync_days_back', '30']
    ];
    
    $insertStmt = $db->prepare("INSERT IGNORE INTO meta_ads_settings (setting_key, setting_value) VALUES (?, ?)");
    foreach ($defaultSettings as $setting) {
        $insertStmt->execute($setting);
    }
    echo "✅ Default settings inserted!\n";

    echo "\n================================================\n";
    echo "🎉 All Meta Ads tables created successfully!\n";
    echo "================================================\n\n";
    
    echo "📋 Tables Created:\n";
    echo "   • meta_ads_insights_daily - Daily campaign/adset/ad metrics\n";
    echo "   • meta_ads_sync_log - Sync history and status\n";
    echo "   • meta_ads_settings - Configuration settings\n\n";
    
    echo "🔧 Next Steps:\n";
    echo "   1. Update api/config/meta_ads.php with your credentials:\n";
    echo "      - META_AD_ACCOUNT_ID (e.g., act_123456789)\n";
    echo "      - META_ACCESS_TOKEN (System User token)\n";
    echo "   2. Get credentials from: https://business.facebook.com/settings/system-users\n";
    echo "   3. Required permissions: ads_read, read_insights\n";
    echo "   4. Test sync from Admin Panel → Marketing\n";

} catch (PDOException $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}

echo "</pre>";
?>
