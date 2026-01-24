<?php
/**
 * Create Razorpay Subscription
 * 
 * Creates a subscription with:
 * - Plan: ₹299/month
 * - Offer: ₹297 discount on first payment
 * - First payment: ₹2 (₹299 - ₹297)
 * - Trial: 7 days before next ₹299 charge
 * 
 * POST /api/subscriptions/create.php
 * Body: { "user_id": 123 }
 */

require_once '../config/cors.php';
require_once '../config/database.php';
require_once '../config/razorpay.php';
require_once '../models/Subscription.php';

header('Content-Type: application/json');

// Only allow POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

$data = json_decode(file_get_contents('php://input'));

if (empty($data->user_id)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'User ID is required']);
    exit;
}

$database = new Database();
$db = $database->getConnection();

try {
    $subscription = new Subscription($db);
    $config = SUBSCRIPTION_CONFIG;
    
    // Check if user already has active subscription
    $existing = $subscription->getActiveByUserId($data->user_id);
    if ($existing) {
        // If payment not completed (UPI pending / user closed), allow resuming the same subscription
        if (in_array($existing['status'], ['created', 'pending'])) {
            http_response_code(200);
            echo json_encode([
                'success' => true,
                'message' => 'Continue your pending payment',
                'data' => [
                    'key' => RAZORPAY_KEY_ID,
                    'subscription_id' => $existing['razorpay_subscription_id'],
                    'name' => 'TNPSC Study App',
                    'description' => $config['description'],
                    'image' => 'https://sudartnpscapp.in/assets/logo.png',
                    'theme' => [
                        'color' => '#6C63FF'
                    ],
                    'notes' => [
                        'user_id' => (string)$data->user_id
                    ],
                    'short_url' => $existing['short_url'] ?? null,
                    'amount_due' => $config['first_payment'],
                    'amount_due_display' => '₹' . ($config['first_payment'] / 100),
                    'plan_amount' => $config['amount'],
                    'plan_amount_display' => '₹' . ($config['amount'] / 100),
                    'trial_days' => $config['trial_days'],
                    'currency' => $config['currency'],
                    'mode' => 'resume',
                    'status' => $existing['status']
                ]
            ]);
            exit;
        }

        // Otherwise, prevent creating duplicate subscriptions
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'You already have an active subscription',
            'subscription_id' => $existing['razorpay_subscription_id'],
            'status' => $existing['status']
        ]);
        exit;
    }

    // Get user details
    $userQuery = "SELECT * FROM users WHERE id = ? LIMIT 1";
    $stmt = $db->prepare($userQuery);
    $stmt->execute([$data->user_id]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user) {
        http_response_code(404);
        echo json_encode(['success' => false, 'message' => 'User not found']);
        exit;
    }

    // Step 1: Create or get Razorpay Customer
    $customerId = $user['razorpay_customer_id'] ?? null;
    
    if (!$customerId) {
        $customerResponse = razorpayApiRequest('customers', 'POST', [
            'name' => $user['name'],
            'email' => $user['email'] ?? ($user['mobile'] . '@sudartnpsc.app'),
            'contact' => $user['mobile'],
            'notes' => [
                'user_id' => (string)$user['id'],
                'source' => 'flutter_app'
            ]
        ]);

        if ($customerResponse['code'] === 200 || $customerResponse['code'] === 201) {
            $customerId = $customerResponse['data']['id'];
        } else if (isset($customerResponse['data']['error']['description']) && 
                   strpos($customerResponse['data']['error']['description'], 'already exists') !== false) {
            // Customer already exists - fetch by contact number
            $searchResponse = razorpayApiRequest('customers?contact=' . urlencode($user['mobile']), 'GET');
            
            if ($searchResponse['code'] === 200 && !empty($searchResponse['data']['items'])) {
                $customerId = $searchResponse['data']['items'][0]['id'];
            } else {
                throw new Exception('Customer exists but could not be retrieved');
            }
        } else {
            throw new Exception('Failed to create customer: ' . json_encode($customerResponse['data']));
        }
        
        // Save customer ID to user
        $updateUser = $db->prepare("UPDATE users SET razorpay_customer_id = ? WHERE id = ?");
        $updateUser->execute([$customerId, $user['id']]);
    }

    // Step 2: Create Subscription
    // Option A: 7-day FREE trial then ₹299/month
    $trialDays = isset($config['trial_days']) ? (int)$config['trial_days'] : 7;
    $startAt = time() + ($trialDays * 86400); // Razorpay uses epoch seconds
    
    $subscriptionData = [
        'plan_id' => RAZORPAY_PLAN_ID,
        'customer_id' => $customerId,
        'total_count' => 120, // 120 months = 10 years (Razorpay requires at least 1)
        'quantity' => 1,
        'customer_notify' => 1,
        'start_at' => $startAt,
        'notes' => [
            'user_id' => (string)$user['id'],
            'user_name' => $user['name'],
            'user_mobile' => $user['mobile']
        ]
    ];

    // No offer/discount/₹2 logic. Trial is handled by start_at.

    $subResponse = razorpayApiRequest('subscriptions', 'POST', $subscriptionData);
    
    if ($subResponse['code'] !== 200 && $subResponse['code'] !== 201) {
        throw new Exception('Failed to create subscription: ' . json_encode($subResponse['data']));
    }

    $razorpaySub = $subResponse['data'];

    // Step 3: Save to database
    $subscription->user_id = $data->user_id;
    $subscription->razorpay_subscription_id = $razorpaySub['id'];
    $subscription->razorpay_plan_id = RAZORPAY_PLAN_ID;
    $subscription->razorpay_customer_id = $customerId;
    $subscription->razorpay_offer_id = null;
    $subscription->plan_name = $config['plan_name'];
    $subscription->amount = $config['amount'] / 100;
    $subscription->currency = $config['currency'];
    $subscription->status = 'created';
    // Do not mark trial in DB until Razorpay mandate is authenticated.
    $subscription->is_trial = false;
    $subscription->trial_start = null;
    $subscription->trial_end = null;
    $subscription->short_url = $razorpaySub['short_url'] ?? null;

    if (!$subscription->create()) {
        throw new Exception('Failed to save subscription to database');
    }

    // Return data for Flutter app to open Razorpay checkout
    http_response_code(201);
    echo json_encode([
        'success' => true,
        'message' => 'Subscription created. Start your 7-day free trial.',
        'data' => [
            // Razorpay Checkout options
            'key' => RAZORPAY_KEY_ID,
            'subscription_id' => $razorpaySub['id'],
            'name' => 'TNPSC Study App',
            'description' => $config['description'],
            'image' => 'https://sudartnpscapp.in/assets/logo.png',
            'prefill' => [
                'name' => $user['name'],
                'email' => $user['email'] ?? '',
                'contact' => $user['mobile']
            ],
            'theme' => [
                'color' => '#6C63FF'
            ],
            'notes' => [
                'user_id' => (string)$user['id']
            ],
            
            // Additional info for app
            'short_url' => $razorpaySub['short_url'] ?? null,
            'plan_amount' => $config['amount'],
            'plan_amount_display' => '₹' . ($config['amount'] / 100),
            'trial_days' => $config['trial_days'],
            'currency' => $config['currency'],
            'start_at' => $startAt
        ]
    ]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error creating subscription',
        'error' => $e->getMessage()
    ]);
}
?>

