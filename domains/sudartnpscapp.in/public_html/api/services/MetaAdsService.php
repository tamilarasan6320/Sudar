<?php
/**
 * Meta Ads Service
 * 
 * Handles all Meta Marketing API interactions including:
 * - Fetching insights (campaign, adset, ad level)
 * - Pagination handling
 * - Rate limiting
 * - Result/action type mapping
 */

require_once __DIR__ . '/../config/meta_ads.php';

class MetaAdsService {
    private $db;
    private $accessToken;
    private $adAccountId;
    private $apiUrl;
    
    public function __construct($db) {
        $this->db = $db;
        $this->accessToken = META_ACCESS_TOKEN;
        $this->adAccountId = META_AD_ACCOUNT_ID;
        $this->apiUrl = META_GRAPH_API_URL;
    }
    
    /**
     * Check if Meta Ads is properly configured
     * 
     * @return bool
     */
    public function isConfigured() {
        return isMetaAdsConfigured();
    }
    
    /**
     * Make a request to Meta Graph API
     * 
     * @param string $endpoint - API endpoint (without base URL)
     * @param array $params - Query parameters
     * @return array - ['success' => bool, 'data' => array|null, 'error' => string|null, 'paging' => array|null]
     */
    public function apiRequest($endpoint, $params = []) {
        // Add access token
        $params['access_token'] = $this->accessToken;
        
        // Build URL
        $url = $this->apiUrl . '/' . $endpoint;
        if (!empty($params)) {
            $url .= '?' . http_build_query($params);
        }
        
        // Initialize cURL
        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 60,
            CURLOPT_HTTPHEADER => [
                'Content-Type: application/json'
            ],
            CURLOPT_SSL_VERIFYPEER => true
        ]);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlError = curl_error($ch);
        curl_close($ch);
        
        // Handle cURL errors
        if ($curlError) {
            return [
                'success' => false,
                'data' => null,
                'error' => 'cURL error: ' . $curlError,
                'http_code' => 0
            ];
        }
        
        // Parse response
        $data = json_decode($response, true);
        
        // Handle HTTP errors
        if ($httpCode >= 400) {
            $errorMsg = 'HTTP ' . $httpCode;
            if (isset($data['error']['message'])) {
                $errorMsg .= ': ' . $data['error']['message'];
            }
            if (isset($data['error']['error_user_msg'])) {
                $errorMsg .= ' - ' . $data['error']['error_user_msg'];
            }
            return [
                'success' => false,
                'data' => $data,
                'error' => $errorMsg,
                'http_code' => $httpCode
            ];
        }
        
        return [
            'success' => true,
            'data' => $data['data'] ?? $data,
            'paging' => $data['paging'] ?? null,
            'http_code' => $httpCode
        ];
    }
    
    /**
     * Fetch insights with automatic pagination
     * 
     * @param string $startDate - YYYY-MM-DD
     * @param string $endDate - YYYY-MM-DD
     * @param string $level - campaign, adset, or ad
     * @return array - ['success' => bool, 'insights' => array, 'error' => string|null]
     */
    public function fetchInsights($startDate, $endDate, $level = 'campaign') {
        if (!$this->isConfigured()) {
            return [
                'success' => false,
                'insights' => [],
                'error' => 'Meta Ads credentials not configured. Please update api/config/meta_ads.php'
            ];
        }
        
        $allInsights = [];
        $fields = getMetaInsightsFields($level);
        
        $params = [
            'level' => $level,
            'fields' => $fields,
            'time_range' => json_encode([
                'since' => $startDate,
                'until' => $endDate
            ]),
            'time_increment' => 1, // Daily breakdown
            'limit' => 500 // Max per page
        ];
        
        $endpoint = $this->adAccountId . '/insights';
        $hasMore = true;
        $pageCount = 0;
        $maxPages = 50; // Safety limit
        
        while ($hasMore && $pageCount < $maxPages) {
            $result = $this->apiRequest($endpoint, $params);
            
            if (!$result['success']) {
                return [
                    'success' => false,
                    'insights' => $allInsights,
                    'error' => $result['error']
                ];
            }
            
            // Add insights from this page
            if (is_array($result['data'])) {
                $allInsights = array_merge($allInsights, $result['data']);
            }
            
            // Check for next page
            if (isset($result['paging']['next'])) {
                // Use the full next URL directly
                $nextUrl = $result['paging']['next'];
                // Extract params from next URL and call again
                $parsedUrl = parse_url($nextUrl);
                parse_str($parsedUrl['query'] ?? '', $nextParams);
                $params = $nextParams;
                $pageCount++;
            } else {
                $hasMore = false;
            }
        }
        
        return [
            'success' => true,
            'insights' => $allInsights,
            'total_rows' => count($allInsights),
            'pages_fetched' => $pageCount + 1
        ];
    }
    
    /**
     * Determine the "result" action type based on campaign objective
     * 
     * @param array $insight - Single insight row from Meta API
     * @return string|null - Action type to use as "result"
     */
    /**
     * Get results and cost_per_result directly from Meta's data
     * Uses optimization_goal/objective to determine which action_type is the "Result"
     * 
     * @param array $insight - Single insight row from Meta API
     * @return array - ['action_type' => string, 'results' => int, 'cost_per_result' => float]
     */
    public function getResultsFromMeta($insight) {
        $objective = $insight['objective'] ?? '';
        $optimizationGoal = $insight['optimization_goal'] ?? '';
        $campaignName = $insight['campaign_name'] ?? '';
        $actions = $insight['actions'] ?? [];
        $costPerActionType = $insight['cost_per_action_type'] ?? [];
        
        // Build dictionaries from Meta response
        $availableActions = [];
        foreach ($actions as $action) {
            if (isset($action['action_type'])) {
                $availableActions[$action['action_type']] = (int)($action['value'] ?? 0);
            }
        }
        
        $costActions = [];
        foreach ($costPerActionType as $cost) {
            if (isset($cost['action_type'])) {
                $costActions[$cost['action_type']] = (float)($cost['value'] ?? 0);
            }
        }
        
        // If no actions at all, return null
        if (empty($availableActions)) {
            return [
                'action_type' => null,
                'results' => 0,
                'cost_per_result' => null
            ];
        }
        
        // Get the mapping from config
        $mapping = META_INSIGHTS_CONFIG['result_action_types'];
        
        $selectedActionType = null;
        
        // 0. FIRST: Check campaign name for "Purchase" keyword
        //    Meta Ads Manager uses different result types based on campaign optimization
        //    If campaign name contains "Purchase", use purchase metrics
        if (stripos($campaignName, 'Purchase') !== false) {
            // Purchase campaign - prioritize purchase actions
            $purchaseTypes = ['omni_purchase', 'purchase', 'app_custom_event.fb_mobile_purchase'];
            foreach ($purchaseTypes as $actionType) {
                if (isset($costActions[$actionType])) {
                    $selectedActionType = $actionType;
                    break;
                }
            }
        }
        
        // 1. Try optimization_goal (more specific)
        if (!$selectedActionType) {
            $optGoalKey = strtoupper($optimizationGoal);
            if (!empty($optGoalKey) && isset($mapping[$optGoalKey])) {
                foreach ($mapping[$optGoalKey] as $actionType) {
                    if (isset($costActions[$actionType])) {
                        $selectedActionType = $actionType;
                        break;
                    }
                }
            }
        }
        
        // 2. Try objective if optimization_goal didn't match
        if (!$selectedActionType) {
            $objectiveKey = strtoupper($objective);
            if (!empty($objectiveKey) && isset($mapping[$objectiveKey])) {
                foreach ($mapping[$objectiveKey] as $actionType) {
                    if (isset($costActions[$actionType])) {
                        $selectedActionType = $actionType;
                        break;
                    }
                }
            }
        }
        
        // 3. Try DEFAULT mapping
        if (!$selectedActionType && isset($mapping['DEFAULT'])) {
            foreach ($mapping['DEFAULT'] as $actionType) {
                if (isset($costActions[$actionType])) {
                    $selectedActionType = $actionType;
                    break;
                }
            }
        }
        
        // 4. Fallback: choose the action_type with highest action COUNT (not highest cost)
        if (!$selectedActionType && !empty($costActions)) {
            $maxCount = -1;
            foreach ($costActions as $actionType => $cost) {
                $count = $availableActions[$actionType] ?? 0;
                if ($count > $maxCount) {
                    $maxCount = $count;
                    $selectedActionType = $actionType;
                }
            }
        }
        
        // Get results and cost_per_result for selected action_type
        $results = $selectedActionType ? ($availableActions[$selectedActionType] ?? 0) : 0;
        $costPerResult = $selectedActionType ? ($costActions[$selectedActionType] ?? null) : null;
        
        return [
            'action_type' => $selectedActionType,
            'results' => $results,
            'cost_per_result' => $costPerResult
        ];
    }
    
    /**
     * @deprecated Use getResultsFromMeta instead
     */
    public function determineResultActionType($insight) {
        $result = $this->getResultsFromMeta($insight);
        return $result['action_type'];
    }
    
    /**
     * Extract results count for a given action type
     * 
     * @param array $actions - Actions array from Meta API
     * @param string $actionType - Action type to look for
     * @return int
     */
    public function getResultsCount($actions, $actionType) {
        if (!is_array($actions)) {
            return 0;
        }
        
        foreach ($actions as $action) {
            if (isset($action['action_type']) && $action['action_type'] === $actionType) {
                return (int)($action['value'] ?? 0);
            }
        }
        
        return 0;
    }
    
    /**
     * Extract cost per result for a given action type
     * 
     * @param array $costPerActionType - cost_per_action_type array from Meta API
     * @param string $actionType - Action type to look for
     * @return float|null
     */
    public function getCostPerResult($costPerActionType, $actionType) {
        if (!is_array($costPerActionType)) {
            return null;
        }
        
        foreach ($costPerActionType as $cost) {
            if (isset($cost['action_type']) && $cost['action_type'] === $actionType) {
                return (float)($cost['value'] ?? 0);
            }
        }
        
        return null;
    }
    
    /**
     * Process raw insight and extract all relevant fields
     * 
     * @param array $insight - Raw insight from Meta API
     * @param string $level - campaign, adset, or ad
     * @return array - Processed insight ready for DB insert
     */
    public function processInsight($insight, $level = 'campaign') {
        // Get results and cost_per_result directly from Meta's actions + cost_per_action_type
        // Uses optimization_goal/objective to select the correct action_type (matches Ads Manager)
        $metaResults = $this->getResultsFromMeta($insight);
        
        $resultActionType = $metaResults['action_type'];
        $results = $metaResults['results'];
        $costPerResult = $metaResults['cost_per_result'];
        
        $actions = $insight['actions'] ?? [];
        $costPerActionType = $insight['cost_per_action_type'] ?? [];
        
        // Parse date (date_start from Meta)
        $day = $insight['date_start'] ?? null;
        
        return [
            'day' => $day,
            'level' => $level,
            'campaign_id' => $insight['campaign_id'] ?? '',
            'campaign_name' => $insight['campaign_name'] ?? null,
            'adset_id' => $insight['adset_id'] ?? '',
            'adset_name' => $insight['adset_name'] ?? null,
            'ad_id' => $insight['ad_id'] ?? '',
            'ad_name' => $insight['ad_name'] ?? null,
            'objective' => $insight['objective'] ?? null,
            'optimization_goal' => $insight['optimization_goal'] ?? null,
            'spend' => (float)($insight['spend'] ?? 0),
            'impressions' => (int)($insight['impressions'] ?? 0),
            'reach' => (int)($insight['reach'] ?? 0),
            'clicks' => (int)($insight['clicks'] ?? 0),
            'cpc' => isset($insight['cpc']) ? (float)$insight['cpc'] : null,
            'cpm' => isset($insight['cpm']) ? (float)$insight['cpm'] : null,
            'ctr' => isset($insight['ctr']) ? (float)$insight['ctr'] : null,
            'result_action_type' => $resultActionType,
            'results' => $results,
            'cost_per_result' => $costPerResult,
            'actions_json' => json_encode($actions),
            'cost_per_action_type_json' => json_encode($costPerActionType),
            'raw_json' => json_encode($insight)
        ];
    }
    
    /**
     * Upsert a processed insight into the database
     * 
     * @param array $data - Processed insight data
     * @return bool
     */
    public function upsertInsight($data) {
        $query = "INSERT INTO meta_ads_insights_daily (
            day, level, campaign_id, campaign_name, adset_id, adset_name, ad_id, ad_name,
            objective, optimization_goal, spend, impressions, reach, clicks,
            cpc, cpm, ctr, result_action_type, results, cost_per_result,
            actions_json, cost_per_action_type_json, raw_json, fetched_at
        ) VALUES (
            :day, :level, :campaign_id, :campaign_name, :adset_id, :adset_name, :ad_id, :ad_name,
            :objective, :optimization_goal, :spend, :impressions, :reach, :clicks,
            :cpc, :cpm, :ctr, :result_action_type, :results, :cost_per_result,
            :actions_json, :cost_per_action_type_json, :raw_json, NOW()
        ) ON DUPLICATE KEY UPDATE
            campaign_name = VALUES(campaign_name),
            adset_name = VALUES(adset_name),
            ad_name = VALUES(ad_name),
            objective = VALUES(objective),
            optimization_goal = VALUES(optimization_goal),
            spend = VALUES(spend),
            impressions = VALUES(impressions),
            reach = VALUES(reach),
            clicks = VALUES(clicks),
            cpc = VALUES(cpc),
            cpm = VALUES(cpm),
            ctr = VALUES(ctr),
            result_action_type = VALUES(result_action_type),
            results = VALUES(results),
            cost_per_result = VALUES(cost_per_result),
            actions_json = VALUES(actions_json),
            cost_per_action_type_json = VALUES(cost_per_action_type_json),
            raw_json = VALUES(raw_json),
            updated_at = NOW()";
        
        try {
            $stmt = $this->db->prepare($query);
            return $stmt->execute([
                ':day' => $data['day'],
                ':level' => $data['level'],
                ':campaign_id' => $data['campaign_id'],
                ':campaign_name' => $data['campaign_name'],
                ':adset_id' => $data['adset_id'],
                ':adset_name' => $data['adset_name'],
                ':ad_id' => $data['ad_id'],
                ':ad_name' => $data['ad_name'],
                ':objective' => $data['objective'],
                ':optimization_goal' => $data['optimization_goal'],
                ':spend' => $data['spend'],
                ':impressions' => $data['impressions'],
                ':reach' => $data['reach'],
                ':clicks' => $data['clicks'],
                ':cpc' => $data['cpc'],
                ':cpm' => $data['cpm'],
                ':ctr' => $data['ctr'],
                ':result_action_type' => $data['result_action_type'],
                ':results' => $data['results'],
                ':cost_per_result' => $data['cost_per_result'],
                ':actions_json' => $data['actions_json'],
                ':cost_per_action_type_json' => $data['cost_per_action_type_json'],
                ':raw_json' => $data['raw_json']
            ]);
        } catch (PDOException $e) {
            error_log('MetaAdsService::upsertInsight error: ' . $e->getMessage());
            return false;
        }
    }
    
    /**
     * Sync insights for a date range
     * 
     * @param string $startDate - YYYY-MM-DD
     * @param string $endDate - YYYY-MM-DD
     * @param string $level - campaign, adset, or ad
     * @return array - ['success' => bool, 'synced' => int, 'total_spend' => float, 'error' => string|null]
     */
    public function syncInsights($startDate, $endDate, $level = 'campaign') {
        // Log sync start
        $logId = $this->logSyncStart($startDate, $endDate, $level);
        
        // Fetch insights from Meta
        $result = $this->fetchInsights($startDate, $endDate, $level);
        
        if (!$result['success']) {
            $this->logSyncComplete($logId, 'failed', 0, 0, $result['error']);
            return [
                'success' => false,
                'synced' => 0,
                'total_spend' => 0,
                'error' => $result['error']
            ];
        }
        
        // Process and upsert each insight
        $synced = 0;
        $totalSpend = 0;
        
        foreach ($result['insights'] as $insight) {
            $processed = $this->processInsight($insight, $level);
            
            if ($this->upsertInsight($processed)) {
                $synced++;
                $totalSpend += $processed['spend'];
            }
        }
        
        // Log sync completion
        $this->logSyncComplete($logId, 'completed', $synced, $totalSpend);
        
        // Update last sync date setting
        $this->updateSetting('last_sync_date', date('Y-m-d H:i:s'));
        
        return [
            'success' => true,
            'synced' => $synced,
            'total_spend' => $totalSpend,
            'total_fetched' => count($result['insights']),
            'pages_fetched' => $result['pages_fetched'] ?? 1
        ];
    }
    
    /**
     * Log sync start
     */
    private function logSyncStart($startDate, $endDate, $level) {
        $stmt = $this->db->prepare("
            INSERT INTO meta_ads_sync_log (start_date, end_date, level, status)
            VALUES (?, ?, ?, 'started')
        ");
        $stmt->execute([$startDate, $endDate, $level]);
        return $this->db->lastInsertId();
    }
    
    /**
     * Log sync completion
     */
    private function logSyncComplete($logId, $status, $rowsSynced, $totalSpend, $error = null) {
        $stmt = $this->db->prepare("
            UPDATE meta_ads_sync_log 
            SET status = ?, rows_synced = ?, total_spend = ?, error_message = ?, completed_at = NOW()
            WHERE id = ?
        ");
        $stmt->execute([$status, $rowsSynced, $totalSpend, $error, $logId]);
    }
    
    /**
     * Get insights from database
     * 
     * @param string $startDate - YYYY-MM-DD
     * @param string $endDate - YYYY-MM-DD
     * @param string $level - campaign, adset, or ad
     * @return array
     */
    public function getInsights($startDate, $endDate, $level = 'campaign') {
        $query = "SELECT 
            day, level, campaign_id, campaign_name, adset_id, adset_name, ad_id, ad_name,
            objective, optimization_goal, spend, impressions, reach, clicks,
            cpc, cpm, ctr, result_action_type, results, cost_per_result, fetched_at
        FROM meta_ads_insights_daily
        WHERE day BETWEEN ? AND ? AND level = ?
        ORDER BY day DESC, spend DESC";
        
        $stmt = $this->db->prepare($query);
        $stmt->execute([$startDate, $endDate, $level]);
        
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    /**
     * Get aggregated totals - uses Meta API directly (no calculations)
     * 
     * @param string $startDate - YYYY-MM-DD
     * @param string $endDate - YYYY-MM-DD
     * @param string $level - campaign, adset, or ad
     * @return array
     */
    public function getInsightsTotals($startDate, $endDate, $level = 'campaign') {
        // Check if there are multiple different result_action_types in DB
        $typeCheckQuery = "SELECT COUNT(DISTINCT result_action_type) as type_count 
            FROM meta_ads_insights_daily 
            WHERE day BETWEEN ? AND ? AND level = ? 
            AND result_action_type IS NOT NULL AND result_action_type != ''";
        $typeCheckStmt = $this->db->prepare($typeCheckQuery);
        $typeCheckStmt->execute([$startDate, $endDate, $level]);
        $typeCount = (int)$typeCheckStmt->fetchColumn();
        
        // Get campaign count from DB
        $countQuery = "SELECT COUNT(DISTINCT campaign_id) as campaigns FROM meta_ads_insights_daily WHERE day BETWEEN ? AND ? AND level = ?";
        $countStmt = $this->db->prepare($countQuery);
        $countStmt->execute([$startDate, $endDate, $level]);
        $campaigns = (int)$countStmt->fetchColumn();
        
        // Fetch account-level summary from Meta API (exact values, no calculation)
        $apiResult = $this->fetchAccountSummary($startDate, $endDate);
        
        if (!$apiResult['success'] || empty($apiResult['data'])) {
            // Fallback to DB if API fails
            return $this->getInsightsTotalsFromDB($startDate, $endDate, $level, $typeCount);
        }
        
        $apiData = $apiResult['data'];
        
        // Build totals from API data
        $totals = [
            'campaigns' => $campaigns,
            'total_spend' => (float)($apiData['spend'] ?? 0),
            'total_impressions' => (int)($apiData['impressions'] ?? 0),
            'total_reach' => (int)($apiData['reach'] ?? 0),
            'total_clicks' => (int)($apiData['clicks'] ?? 0),
            'avg_cpc' => isset($apiData['cpc']) ? (float)$apiData['cpc'] : null,
            'avg_cpm' => isset($apiData['cpm']) ? (float)$apiData['cpm'] : null,
            'avg_ctr' => isset($apiData['ctr']) ? (float)$apiData['ctr'] : null
        ];
        
        // Handle results and cost_per_result based on single/multiple conversion types
        if ($typeCount > 1) {
            // Multiple conversion types - like Meta Ads Manager shows "Multiple conversions"
            $totals['total_results'] = null;
            $totals['avg_cost_per_result'] = null;
            $totals['result_mode'] = 'multiple';
        } else {
            // Single conversion type - get exact values from API
            $singleActionType = $this->getSingleResultActionType($startDate, $endDate, $level);
            if ($singleActionType) {
                $extracted = $this->extractResultsFromApiRow($apiData, $singleActionType);
                $totals['total_results'] = $extracted['results'];
                $totals['avg_cost_per_result'] = $extracted['cost_per_result'];
            } else {
                $totals['total_results'] = 0;
                $totals['avg_cost_per_result'] = null;
            }
            $totals['result_mode'] = 'single';
        }
        
        return $totals;
    }
    
    /**
     * Fallback: Get totals from DB when API fails
     */
    private function getInsightsTotalsFromDB($startDate, $endDate, $level, $typeCount) {
        $query = "SELECT 
            COUNT(DISTINCT campaign_id) as campaigns,
            SUM(spend) as total_spend,
            SUM(impressions) as total_impressions,
            SUM(reach) as total_reach,
            SUM(clicks) as total_clicks,
            SUM(results) as total_results
        FROM meta_ads_insights_daily
        WHERE day BETWEEN ? AND ? AND level = ?";
        
        $stmt = $this->db->prepare($query);
        $stmt->execute([$startDate, $endDate, $level]);
        $totals = $stmt->fetch(PDO::FETCH_ASSOC);
        
        // Calculate averages from DB (fallback)
        $totals['avg_cpc'] = ($totals['total_clicks'] > 0) ? ($totals['total_spend'] / $totals['total_clicks']) : null;
        $totals['avg_cpm'] = ($totals['total_impressions'] > 0) ? (($totals['total_spend'] / $totals['total_impressions']) * 1000) : null;
        $totals['avg_ctr'] = ($totals['total_impressions'] > 0) ? (($totals['total_clicks'] / $totals['total_impressions']) * 100) : null;
        
        if ($typeCount > 1) {
            $totals['total_results'] = null;
            $totals['avg_cost_per_result'] = null;
            $totals['result_mode'] = 'multiple';
        } else {
            $totals['avg_cost_per_result'] = null; // No calculation, show as unavailable
            $totals['result_mode'] = 'single';
        }
        
        return $totals;
    }
    
    /**
     * Get daily totals - uses Meta API directly (no calculations)
     * 
     * @param string $startDate - YYYY-MM-DD
     * @param string $endDate - YYYY-MM-DD
     * @param string $level - campaign, adset, or ad
     * @return array
     */
    public function getDailyTotals($startDate, $endDate, $level = 'campaign') {
        // Check for single/multiple result types
        $singleActionType = $this->getSingleResultActionType($startDate, $endDate, $level);
        
        // Fetch account-level daily insights from Meta API
        $apiResult = $this->fetchAccountDaily($startDate, $endDate);
        
        if (!$apiResult['success'] || empty($apiResult['data'])) {
            // Fallback to DB if API fails
            return $this->getDailyTotalsFromDB($startDate, $endDate, $level);
        }
        
        $dailyData = [];
        foreach ($apiResult['data'] as $row) {
            $day = $row['date_start'] ?? null;
            if (!$day) continue;
            
            $dayRow = [
                'day' => $day,
                'spend' => (float)($row['spend'] ?? 0),
                'impressions' => (int)($row['impressions'] ?? 0),
                'clicks' => (int)($row['clicks'] ?? 0)
            ];
            
            // Get results and cost_per_result from API (no calculation)
            if ($singleActionType) {
                $extracted = $this->extractResultsFromApiRow($row, $singleActionType);
                $dayRow['results'] = $extracted['results'];
                $dayRow['cost_per_result'] = $extracted['cost_per_result'];
            } else {
                // Multiple conversion types
                $dayRow['results'] = null;
                $dayRow['cost_per_result'] = null;
            }
            
            $dailyData[] = $dayRow;
        }
        
        // Sort by day
        usort($dailyData, function($a, $b) {
            return strcmp($a['day'], $b['day']);
        });
        
        return $dailyData;
    }
    
    /**
     * Fallback: Get daily totals from DB when API fails
     */
    private function getDailyTotalsFromDB($startDate, $endDate, $level) {
        $query = "SELECT 
            day,
            SUM(spend) as spend,
            SUM(impressions) as impressions,
            SUM(clicks) as clicks,
            SUM(results) as results,
            NULL as cost_per_result
        FROM meta_ads_insights_daily
        WHERE day BETWEEN ? AND ? AND level = ?
        GROUP BY day
        ORDER BY day ASC";
        
        $stmt = $this->db->prepare($query);
        $stmt->execute([$startDate, $endDate, $level]);
        
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    /**
     * Get recent sync logs
     * 
     * @param int $limit
     * @return array
     */
    public function getSyncLogs($limit = 10) {
        $limit = (int) $limit;
        $stmt = $this->db->prepare("
            SELECT * FROM meta_ads_sync_log
            ORDER BY started_at DESC
            LIMIT {$limit}
        ");
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    /**
     * Get a setting value
     * 
     * @param string $key
     * @param mixed $default
     * @return mixed
     */
    public function getSetting($key, $default = null) {
        $stmt = $this->db->prepare("SELECT setting_value FROM meta_ads_settings WHERE setting_key = ?");
        $stmt->execute([$key]);
        $result = $stmt->fetchColumn();
        return $result !== false ? $result : $default;
    }
    
    /**
     * Update a setting value
     * 
     * @param string $key
     * @param mixed $value
     * @return bool
     */
    public function updateSetting($key, $value) {
        $stmt = $this->db->prepare("
            INSERT INTO meta_ads_settings (setting_key, setting_value) 
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)
        ");
        return $stmt->execute([$key, $value]);
    }
    
    /**
     * Fetch account-level insights from Meta API (summary for date range)
     * Returns aggregated totals for the entire account
     * 
     * @param string $startDate - YYYY-MM-DD
     * @param string $endDate - YYYY-MM-DD
     * @return array - ['success' => bool, 'data' => array, 'error' => string|null]
     */
    public function fetchAccountSummary($startDate, $endDate) {
        if (!$this->isConfigured()) {
            return [
                'success' => false,
                'data' => null,
                'error' => 'Meta Ads credentials not configured'
            ];
        }
        
        $fields = 'spend,impressions,reach,clicks,cpc,cpm,ctr,actions,cost_per_action_type';
        
        $params = [
            'level' => 'account',
            'fields' => $fields,
            'time_range' => json_encode([
                'since' => $startDate,
                'until' => $endDate
            ])
        ];
        
        $endpoint = $this->adAccountId . '/insights';
        $result = $this->apiRequest($endpoint, $params);
        
        if (!$result['success']) {
            return [
                'success' => false,
                'data' => null,
                'error' => $result['error']
            ];
        }
        
        // API returns array, get first row (summary)
        $data = is_array($result['data']) && isset($result['data'][0]) ? $result['data'][0] : $result['data'];
        
        return [
            'success' => true,
            'data' => $data
        ];
    }
    
    /**
     * Fetch account-level daily insights from Meta API
     * Returns daily breakdown for the account
     * 
     * @param string $startDate - YYYY-MM-DD
     * @param string $endDate - YYYY-MM-DD
     * @return array - ['success' => bool, 'data' => array, 'error' => string|null]
     */
    public function fetchAccountDaily($startDate, $endDate) {
        if (!$this->isConfigured()) {
            return [
                'success' => false,
                'data' => [],
                'error' => 'Meta Ads credentials not configured'
            ];
        }
        
        $fields = 'spend,impressions,reach,clicks,cpc,cpm,ctr,actions,cost_per_action_type,date_start,date_stop';
        
        $params = [
            'level' => 'account',
            'fields' => $fields,
            'time_range' => json_encode([
                'since' => $startDate,
                'until' => $endDate
            ]),
            'time_increment' => 1 // Daily breakdown
        ];
        
        $endpoint = $this->adAccountId . '/insights';
        $allData = [];
        $hasMore = true;
        $pageCount = 0;
        $maxPages = 50;
        
        while ($hasMore && $pageCount < $maxPages) {
            $result = $this->apiRequest($endpoint, $params);
            
            if (!$result['success']) {
                return [
                    'success' => false,
                    'data' => $allData,
                    'error' => $result['error']
                ];
            }
            
            if (is_array($result['data'])) {
                $allData = array_merge($allData, $result['data']);
            }
            
            if (isset($result['paging']['next'])) {
                $parsedUrl = parse_url($result['paging']['next']);
                parse_str($parsedUrl['query'] ?? '', $nextParams);
                $params = $nextParams;
                $pageCount++;
            } else {
                $hasMore = false;
            }
        }
        
        return [
            'success' => true,
            'data' => $allData
        ];
    }
    
    /**
     * Extract results count and cost_per_result from API actions/cost_per_action_type
     * for a specific action_type
     * 
     * @param array $apiRow - Single row from Meta API with actions and cost_per_action_type
     * @param string $actionType - The action_type to extract
     * @return array - ['results' => int, 'cost_per_result' => float|null]
     */
    public function extractResultsFromApiRow($apiRow, $actionType) {
        $actions = $apiRow['actions'] ?? [];
        $costPerActionType = $apiRow['cost_per_action_type'] ?? [];
        
        $results = 0;
        $costPerResult = null;
        
        // Get results count
        foreach ($actions as $action) {
            if (isset($action['action_type']) && $action['action_type'] === $actionType) {
                $results = (int)($action['value'] ?? 0);
                break;
            }
        }
        
        // Get cost per result
        foreach ($costPerActionType as $cost) {
            if (isset($cost['action_type']) && $cost['action_type'] === $actionType) {
                $costPerResult = (float)($cost['value'] ?? 0);
                break;
            }
        }
        
        return [
            'results' => $results,
            'cost_per_result' => $costPerResult
        ];
    }
    
    /**
     * Get the single result_action_type used in a date range (if only one type exists)
     * 
     * @param string $startDate
     * @param string $endDate
     * @param string $level
     * @return string|null - action_type or null if multiple/none
     */
    public function getSingleResultActionType($startDate, $endDate, $level = 'campaign') {
        $query = "SELECT DISTINCT result_action_type 
            FROM meta_ads_insights_daily 
            WHERE day BETWEEN ? AND ? AND level = ? 
            AND result_action_type IS NOT NULL AND result_action_type != ''";
        $stmt = $this->db->prepare($query);
        $stmt->execute([$startDate, $endDate, $level]);
        $types = $stmt->fetchAll(PDO::FETCH_COLUMN);
        
        if (count($types) === 1) {
            return $types[0];
        }
        return null;
    }
}
?>
