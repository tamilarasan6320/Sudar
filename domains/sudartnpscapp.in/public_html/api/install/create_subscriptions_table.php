<?php
/**
 * Create Subscription Tables
 * 
 * Run this once to create the required database tables
 * URL: https://sudartnpscapp.in/api/install/create_subscriptions_table.php
 */

require_once '../config/database.php';

$database = new Database();
$db = $database->getConnection();

echo "<pre style='font-family: monospace; padding: 20px;'>";
echo "🚀 TNPSC App - Subscription Database Setup\n";
echo "==========================================\n\n";

try {
    // 1. Create subscriptions table
    $query1 = "CREATE TABLE IF NOT EXISTS subscriptions (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        
        -- Razorpay IDs
        razorpay_subscription_id VARCHAR(100) UNIQUE,
        razorpay_plan_id VARCHAR(100),
        razorpay_customer_id VARCHAR(100),
        razorpay_offer_id VARCHAR(100),
        
        -- Plan Details
        plan_name VARCHAR(100) DEFAULT 'TNPSC Monthly Premium',
        amount DECIMAL(10,2) DEFAULT 299.00,
        currency VARCHAR(10) DEFAULT 'INR',
        
        -- Status
        status ENUM('created', 'authenticated', 'active', 'pending', 'halted', 'paused', 'cancelled', 'completed', 'expired') DEFAULT 'created',
        razorpay_status VARCHAR(32) NULL,
        
        -- Trial Period
        is_trial TINYINT(1) DEFAULT 1,
        trial_start DATETIME NULL,
        trial_end DATETIME NULL,
        
        -- Billing Period
        current_period_start DATETIME NULL,
        current_period_end DATETIME NULL,
        next_billing_date DATETIME NULL,
        
        -- Payment Info
        total_payments INT DEFAULT 0,
        last_payment_amount DECIMAL(10,2) NULL,
        last_payment_date DATETIME NULL,
        
        -- Settings
        auto_renew TINYINT(1) DEFAULT 1,
        short_url VARCHAR(255) NULL,
        
        -- Timestamps
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        cancelled_at DATETIME NULL,
        
        -- Foreign Key
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        INDEX idx_user_status (user_id, status),
        INDEX idx_razorpay_sub (razorpay_subscription_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    
    $db->exec($query1);
    echo "✅ Table 'subscriptions' created successfully!\n";

    // 2. Create subscription_payments table
    $query2 = "CREATE TABLE IF NOT EXISTS subscription_payments (
        id INT AUTO_INCREMENT PRIMARY KEY,
        subscription_id INT NOT NULL,
        user_id INT NOT NULL,
        
        -- Razorpay Payment IDs
        razorpay_payment_id VARCHAR(100) UNIQUE,
        razorpay_order_id VARCHAR(100),
        razorpay_signature VARCHAR(255),
        razorpay_invoice_id VARCHAR(100),
        
        -- Payment Details
        amount DECIMAL(10,2) NOT NULL,
        currency VARCHAR(10) DEFAULT 'INR',
        status ENUM('created', 'authorized', 'captured', 'refunded', 'failed') DEFAULT 'created',
        
        -- Payment Method
        payment_method VARCHAR(50),
        bank VARCHAR(100),
        wallet VARCHAR(50),
        vpa VARCHAR(100),
        
        -- Error Info
        error_code VARCHAR(50),
        error_description TEXT,
        error_reason VARCHAR(100),
        
        -- Timestamps
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        
        -- Foreign Keys
        FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        INDEX idx_sub_payment (subscription_id),
        INDEX idx_razorpay_payment (razorpay_payment_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    
    $db->exec($query2);
    echo "✅ Table 'subscription_payments' created successfully!\n";

    // 3. Add premium fields to users table
    $query3 = "ALTER TABLE users 
               ADD COLUMN IF NOT EXISTS is_premium TINYINT(1) DEFAULT 0,
               ADD COLUMN IF NOT EXISTS premium_expires_at DATETIME NULL,
               ADD COLUMN IF NOT EXISTS subscription_id INT NULL,
               ADD COLUMN IF NOT EXISTS razorpay_customer_id VARCHAR(100) NULL";
    
    $db->exec($query3);
    echo "✅ Users table updated with premium fields!\n";

    // 4. Add razorpay_status column to subscriptions table (for existing installs)
    $query4 = "ALTER TABLE subscriptions 
               ADD COLUMN IF NOT EXISTS razorpay_status VARCHAR(32) NULL AFTER status";
    
    $db->exec($query4);
    echo "✅ Subscriptions table updated with razorpay_status column!\n";

    // 5. Add last_payment_status column to subscriptions table
    $query5 = "ALTER TABLE subscriptions 
               ADD COLUMN IF NOT EXISTS last_payment_status VARCHAR(32) NULL AFTER razorpay_status";
    
    $db->exec($query5);
    echo "✅ Subscriptions table updated with last_payment_status column!\n";

    // 6. Add meta_trial_cancel_event_sent_at column for idempotency (Meta App Events)
    $query6 = "ALTER TABLE subscriptions 
               ADD COLUMN IF NOT EXISTS meta_trial_cancel_event_sent_at DATETIME NULL AFTER cancelled_at";
    
    $db->exec($query6);
    echo "✅ Subscriptions table updated with meta_trial_cancel_event_sent_at column!\n";

    // 7. Add meta_trial_paid_event_sent_at column for idempotency (trial -> paid ₹299 event)
    $query7 = "ALTER TABLE subscriptions
               ADD COLUMN IF NOT EXISTS meta_trial_paid_event_sent_at DATETIME NULL AFTER meta_trial_cancel_event_sent_at";

    $db->exec($query7);
    echo "✅ Subscriptions table updated with meta_trial_paid_event_sent_at column!\n";

    echo "\n==========================================\n";
    echo "🎉 All tables created successfully!\n";
    echo "==========================================\n\n";
    
    echo "📋 Tables Created:\n";
    echo "   • subscriptions - Stores subscription info\n";
    echo "   • subscription_payments - Payment history\n";
    echo "   • users (updated) - Added premium fields\n\n";
    
    echo "🔧 Next Steps:\n";
    echo "   1. Update razorpay.php with your API keys\n";
    echo "   2. Create Plan in Razorpay Dashboard\n";
    echo "   3. Create Offer for ₹297 discount\n";
    echo "   4. Add webhook URL in Razorpay Dashboard\n";

} catch (PDOException $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}

echo "</pre>";
?>

