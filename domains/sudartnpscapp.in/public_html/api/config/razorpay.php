<?php
/**
 * Razorpay Configuration
 * 
 * TNPSC Study App - Subscription Configuration
 * 
 * Plan: ₹299/month with 7-day FREE trial (₹0 during trial)
 */

// Set correct timezone for India
date_default_timezone_set('Asia/Kolkata');

// ============================================
// 🔐 RAZORPAY API CREDENTIALS (LIVE)
// ============================================

define('RAZORPAY_KEY_ID', 'rzp_live_RqKzojP50VITEX');
define('RAZORPAY_KEY_SECRET', 'qVnjrZKAJjuHE80sxaSmtXQB');

// ============================================
// 📋 SUBSCRIPTION PLAN ID
// ============================================

// Plan ID - ₹299/month plan created in Razorpay Dashboard
// Updated as per latest Razorpay plan (screenshot)
define('RAZORPAY_PLAN_ID', 'plan_RugOglJfaIW0Yl');

// Offer ID is NOT used (no discounted first cycle)
define('RAZORPAY_OFFER_ID', '');

// ============================================
// 💰 PLAN CONFIGURATION
// ============================================

define('SUBSCRIPTION_CONFIG', [
    'plan_name'        => 'TNPSC Monthly Premium',
    'amount'           => 29900,          // ₹299.00 in paise
    'currency'         => 'INR',
    'period'           => 'monthly',
    'interval'         => 1,
    'trial_days'       => 7,              // First ₹299 charge after 7 days
    'total_count'      => 0,              // 0 = Unlimited billing cycles
    'description'      => 'TNPSC Study App Premium - 7-day FREE trial',
]);

// ============================================
// 🔗 WEBHOOK SECRET
// ============================================

// Get this from Razorpay Dashboard → Settings → Webhooks
define('RAZORPAY_WEBHOOK_SECRET', 'tnpsc_webhook_secret_2025');

// ============================================
// 🌐 CALLBACK URLs
// ============================================

define('APP_URL', 'https://sudartnpscapp.in');

/**
 * Make Razorpay API Request
 * 
 * @param string $endpoint API endpoint
 * @param string $method HTTP method (GET, POST, PATCH)
 * @param array|null $data Request body
 * @return array Response with 'code' and 'data'
 */
function razorpayApiRequest($endpoint, $method = 'GET', $data = null) {
    $url = 'https://api.razorpay.com/v1/' . $endpoint;
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_USERPWD, RAZORPAY_KEY_ID . ':' . RAZORPAY_KEY_SECRET);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json'
    ]);
    
    if ($method === 'POST') {
        curl_setopt($ch, CURLOPT_POST, true);
        if ($data) {
            curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
        }
    } elseif ($method === 'PATCH') {
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PATCH');
        if ($data) {
            curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
        }
    } elseif ($method === 'DELETE') {
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'DELETE');
    }
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);
    
    if ($error) {
        return [
            'code' => 0,
            'data' => ['error' => $error]
        ];
    }
    
    return [
        'code' => $httpCode,
        'data' => json_decode($response, true)
    ];
}

/**
 * Verify Razorpay Signature
 * 
 * @param string $payload Raw request body
 * @param string $signature Razorpay signature header
 * @return bool
 */
function verifyRazorpaySignature($payload, $signature) {
    $expectedSignature = hash_hmac('sha256', $payload, RAZORPAY_WEBHOOK_SECRET);
    return hash_equals($expectedSignature, $signature);
}

/**
 * Verify Payment Signature
 * 
 * @param string $subscriptionId Razorpay subscription ID
 * @param string $paymentId Razorpay payment ID
 * @param string $signature Razorpay signature
 * @return bool
 */
function verifyPaymentSignature($subscriptionId, $paymentId, $signature) {
    $payload = $paymentId . '|' . $subscriptionId;
    $expectedSignature = hash_hmac('sha256', $payload, RAZORPAY_KEY_SECRET);
    return hash_equals($expectedSignature, $signature);
}
?>

