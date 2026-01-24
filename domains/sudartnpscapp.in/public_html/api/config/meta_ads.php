<?php
/**
 * Meta Ads (Facebook/Instagram) Marketing API Configuration
 * 
 * TNPSC Study App - Meta Ads Insights Integration
 * 
 * To get these credentials:
 * 1. Go to https://business.facebook.com/settings/system-users
 * 2. Create a System User with "Admin" role
 * 3. Generate access token with permissions: ads_read, read_insights
 * 4. Get your Ad Account ID from Ads Manager (format: act_XXXXXXXXXX)
 */

// Set correct timezone for India
date_default_timezone_set('Asia/Kolkata');

// ============================================
// 🔐 META ADS API CREDENTIALS
// ============================================

// Ad Account ID (get from Meta Ads Manager → Account Overview)
// Format: act_XXXXXXXXXXXXXXXXX
define('META_AD_ACCOUNT_ID', 'act_1179324677654852');

// System User Access Token (with ads_read + read_insights permissions)
// Generate from: https://business.facebook.com/settings/system-users
// Token should be a "System User Token" for server-to-server calls
define('META_ACCESS_TOKEN', 'EAAdh4u8lGJEBQYJeoj13rYsgoUFAEdXzzWgx8eO48kBSzTXs1OlvZAb5v5Sa316FvUrZCIxGYFsk2NrndHT8Yi9jL7uw7i34ZCaymVnEX886rMceZCix7QNNDzAGdZCVEvr9fqAAQmGO5SQYeCcfPbviIxoZCPZCYZChbCZCTYSwIT4HFkjYRNq7IEEqwQcBhR6AnEQZDZD');

// ============================================
// 🌐 META GRAPH API SETTINGS
// ============================================

// Graph API version (use latest stable)
define('META_GRAPH_API_VERSION', 'v21.0');

// Graph API base URL
define('META_GRAPH_API_URL', 'https://graph.facebook.com/' . META_GRAPH_API_VERSION);

// ============================================
// 📊 DEFAULT INSIGHTS SETTINGS
// ============================================

define('META_INSIGHTS_CONFIG', [
    // Default reporting level (campaign, adset, ad)
    'default_level' => 'campaign',
    
    // Time increment for daily breakdown
    'time_increment' => 1,
    
    // Maximum days to fetch in one request
    'max_days_per_request' => 90,
    
    // Fields to fetch from insights
    'fields' => [
        'campaign_id',
        'campaign_name',
        'adset_id',
        'adset_name',
        'ad_id',
        'ad_name',
        'objective',
        'optimization_goal',
        'spend',
        'impressions',
        'reach',
        'clicks',
        'cpc',
        'cpm',
        'ctr',
        'actions',
        'cost_per_action_type',
        'date_start',
        'date_stop'
    ],
    
    // Action types that typically represent "results" by objective/optimization_goal
    // Order matters: first match wins
    // IMPORTANT: Use action types that have BOTH count AND cost_per_action_type
    // omni_app_install has cost, mobile_app_install often doesn't
    // omni_purchase has cost, app_custom_event.fb_mobile_purchase often doesn't
    'result_action_types' => [
        // App Install campaigns - use omni_app_install (has cost_per_action_type)
        'APP_INSTALLS' => ['omni_app_install', 'mobile_app_install', 'app_install'],
        'OUTCOME_APP_PROMOTION' => ['omni_app_install', 'mobile_app_install', 'app_install'],
        
        // Purchase/Conversion campaigns - use omni_purchase (has cost_per_action_type)
        'CONVERSIONS' => ['omni_purchase', 'purchase', 'app_custom_event.fb_mobile_purchase'],
        'OUTCOME_SALES' => ['omni_purchase', 'purchase', 'app_custom_event.fb_mobile_purchase'],
        'OFFSITE_CONVERSIONS' => ['omni_purchase', 'purchase', 'app_custom_event.fb_mobile_purchase', 'complete_registration'],
        'VALUE' => ['omni_purchase', 'purchase', 'app_custom_event.fb_mobile_purchase'],
        
        // App Engagement
        'APP_ENGAGEMENT' => ['app_engagement', 'mobile_app_custom_event', 'app_custom_event'],
        
        // Traffic/clicks
        'LINK_CLICKS' => ['link_click', 'outbound_click'],
        'LANDING_PAGE_VIEWS' => ['landing_page_view'],
        'OUTCOME_TRAFFIC' => ['link_click', 'landing_page_view'],
        
        // Leads
        'LEAD_GENERATION' => ['lead', 'leadgen_grouped', 'on_facebook_lead'],
        'OUTCOME_LEADS' => ['lead', 'leadgen_grouped', 'on_facebook_lead'],
        
        // Engagement
        'ENGAGEMENT' => ['post_engagement', 'page_engagement', 'video_view'],
        'OUTCOME_ENGAGEMENT' => ['post_engagement', 'page_engagement', 'video_view'],
        'REACH' => ['reach'],
        'OUTCOME_AWARENESS' => ['reach', 'impressions'],
        
        // Default fallback - use omni_ types (they have cost_per_action_type)
        'DEFAULT' => ['omni_app_install', 'omni_purchase', 'purchase', 'link_click', 'mobile_app_install']
    ]
]);

// ============================================
// 🔄 RATE LIMITING
// ============================================

// Meta API rate limits (per hour)
define('META_API_RATE_LIMIT', [
    'calls_per_hour' => 200,
    'retry_delay_seconds' => 60
]);

/**
 * Check if Meta Ads credentials are configured
 * 
 * @return bool
 */
function isMetaAdsConfigured() {
    return META_AD_ACCOUNT_ID !== 'act_XXXXXXXXXXXXXXXXX' 
        && META_ACCESS_TOKEN !== 'YOUR_ACCESS_TOKEN_HERE'
        && !empty(META_AD_ACCOUNT_ID)
        && !empty(META_ACCESS_TOKEN);
}

/**
 * Get Meta Ads insights fields as comma-separated string
 * 
 * @param string $level - campaign, adset, or ad
 * @return string
 */
function getMetaInsightsFields($level = 'campaign') {
    $fields = META_INSIGHTS_CONFIG['fields'];
    
    // Filter fields based on level
    if ($level === 'campaign') {
        $fields = array_filter($fields, function($f) {
            return !in_array($f, ['adset_id', 'adset_name', 'ad_id', 'ad_name']);
        });
    } elseif ($level === 'adset') {
        $fields = array_filter($fields, function($f) {
            return !in_array($f, ['ad_id', 'ad_name']);
        });
    }
    
    return implode(',', array_values($fields));
}
?>
