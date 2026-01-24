<?php
/**
 * Subscription Model
 * 
 * Handles all subscription-related database operations
 */

class Subscription {
    private $conn;
    private $table = "subscriptions";

    // Properties
    public $id;
    public $user_id;
    public $razorpay_subscription_id;
    public $razorpay_plan_id;
    public $razorpay_customer_id;
    public $razorpay_offer_id;
    public $plan_name;
    public $amount;
    public $currency;
    public $status;
    public $is_trial;
    public $trial_start;
    public $trial_end;
    public $current_period_start;
    public $current_period_end;
    public $next_billing_date;
    public $total_payments;
    public $last_payment_amount;
    public $last_payment_date;
    public $auto_renew;
    public $short_url;

    public function __construct($db) {
        $this->conn = $db;
    }

    /**
     * Create new subscription
     */
    public function create() {
        $query = "INSERT INTO " . $this->table . " 
                  (user_id, razorpay_subscription_id, razorpay_plan_id, razorpay_customer_id, 
                   razorpay_offer_id, plan_name, amount, currency, status, is_trial, 
                   trial_start, trial_end, short_url)
                  VALUES 
                  (:user_id, :razorpay_subscription_id, :razorpay_plan_id, :razorpay_customer_id,
                   :razorpay_offer_id, :plan_name, :amount, :currency, :status, :is_trial,
                   :trial_start, :trial_end, :short_url)";

        $stmt = $this->conn->prepare($query);
        
        $stmt->bindParam(':user_id', $this->user_id, PDO::PARAM_INT);
        $stmt->bindParam(':razorpay_subscription_id', $this->razorpay_subscription_id);
        $stmt->bindParam(':razorpay_plan_id', $this->razorpay_plan_id);
        $stmt->bindParam(':razorpay_customer_id', $this->razorpay_customer_id);
        $stmt->bindParam(':razorpay_offer_id', $this->razorpay_offer_id);
        $stmt->bindParam(':plan_name', $this->plan_name);
        $stmt->bindParam(':amount', $this->amount);
        $stmt->bindParam(':currency', $this->currency);
        $stmt->bindParam(':status', $this->status);
        $stmt->bindParam(':is_trial', $this->is_trial, PDO::PARAM_BOOL);
        $stmt->bindParam(':trial_start', $this->trial_start);
        $stmt->bindParam(':trial_end', $this->trial_end);
        $stmt->bindParam(':short_url', $this->short_url);

        if ($stmt->execute()) {
            $this->id = $this->conn->lastInsertId();
            return true;
        }
        return false;
    }

    /**
     * Update subscription after payment authentication
     */
    public function updateAfterAuth() {
        $query = "UPDATE " . $this->table . " 
                  SET status = :status,
                      is_trial = :is_trial,
                      trial_start = :trial_start,
                      trial_end = :trial_end,
                      current_period_start = :current_period_start,
                      current_period_end = :current_period_end,
                      next_billing_date = :next_billing_date,
                      last_payment_date = NOW(),
                      last_payment_amount = :last_payment_amount,
                      total_payments = total_payments + 1
                  WHERE id = :id";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':status', $this->status);
        $stmt->bindParam(':is_trial', $this->is_trial, PDO::PARAM_BOOL);
        $stmt->bindParam(':trial_start', $this->trial_start);
        $stmt->bindParam(':trial_end', $this->trial_end);
        $stmt->bindParam(':current_period_start', $this->current_period_start);
        $stmt->bindParam(':current_period_end', $this->current_period_end);
        $stmt->bindParam(':next_billing_date', $this->next_billing_date);
        $stmt->bindParam(':last_payment_amount', $this->last_payment_amount);
        $stmt->bindParam(':id', $this->id, PDO::PARAM_INT);
        
        return $stmt->execute();
    }

    /**
     * Update status only
     */
    public function updateStatus($status) {
        $query = "UPDATE " . $this->table . " SET status = :status WHERE id = :id";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':status', $status);
        $stmt->bindParam(':id', $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    /**
     * Get subscription by ID
     */
    public function getById($id) {
        $query = "SELECT * FROM " . $this->table . " WHERE id = :id LIMIT 1";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':id', $id, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetch(PDO::FETCH_ASSOC);
    }

    /**
     * Update Razorpay status only (fetched from Razorpay API)
     */
    public function updateRazorpayStatus($razorpayStatus) {
        $query = "UPDATE " . $this->table . " SET razorpay_status = :razorpay_status WHERE id = :id";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':razorpay_status', $razorpayStatus);
        $stmt->bindParam(':id', $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    /**
     * Record recurring payment
     */
    public function recordPayment($amount) {
        $query = "UPDATE " . $this->table . " 
                  SET is_trial = 0,
                      status = 'active',
                      current_period_start = NOW(),
                      current_period_end = DATE_ADD(NOW(), INTERVAL 1 MONTH),
                      next_billing_date = DATE_ADD(NOW(), INTERVAL 1 MONTH),
                      last_payment_date = NOW(),
                      last_payment_amount = :amount,
                      total_payments = total_payments + 1
                  WHERE id = :id";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':amount', $amount);
        $stmt->bindParam(':id', $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    /**
     * Cancel subscription
     */
    public function cancel() {
        $query = "UPDATE " . $this->table . " 
                  SET status = 'cancelled', 
                      auto_renew = 0,
                      cancelled_at = NOW()
                  WHERE id = :id";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':id', $this->id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    /**
     * Get by Razorpay Subscription ID
     */
    public function getByRazorpayId($razorpay_subscription_id) {
        $query = "SELECT * FROM " . $this->table . " 
                  WHERE razorpay_subscription_id = :razorpay_subscription_id 
                  LIMIT 1";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':razorpay_subscription_id', $razorpay_subscription_id);
        $stmt->execute();
        return $stmt->fetch(PDO::FETCH_ASSOC);
    }

    /**
     * Get active subscription for user
     * 
     * Returns subscription if still has valid access:
     * - Trial (is_trial=1): Valid until trial_end (7 days)
     * - Paid (is_trial=0): Valid until current_period_end (1 month)
     */
    public function getActiveByUserId($user_id) {
        // Get latest subscription
        $query = "SELECT * FROM " . $this->table . " 
                  WHERE user_id = :user_id 
                  ORDER BY created_at DESC 
                  LIMIT 1";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
        $stmt->execute();
        $sub = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$sub) {
            return null;
        }
        
        $now = time();
        $isTrial = (bool)$sub['is_trial'];
        $trialEnd = $sub['trial_end'] ? strtotime($sub['trial_end']) : null;
        $periodEnd = $sub['current_period_end'] ? strtotime($sub['current_period_end']) : null;
        $razorpayStatus = $sub['razorpay_status'] ?? null;
        $internalStatus = $sub['status'] ?? null;
        
        // Cancelled/Expired - check if still within valid period
        if ($razorpayStatus && in_array($razorpayStatus, ['cancelled', 'expired', 'halted', 'completed'])) {
            if ($isTrial) {
                // Trial: only trial_end matters (7 days)
                return ($trialEnd && $trialEnd > $now) ? $sub : null;
            } else {
                // Paid: current_period_end matters (1 month)
                return ($periodEnd && $periodEnd > $now) ? $sub : null;
            }
        }
        
        if ($internalStatus === 'cancelled') {
            if ($isTrial) {
                return ($trialEnd && $trialEnd > $now) ? $sub : null;
            } else {
                return ($periodEnd && $periodEnd > $now) ? $sub : null;
            }
        }
        
        // Active subscriptions - check validity
        if (in_array($internalStatus, ['created', 'authenticated', 'active', 'pending'])) {
            if ($isTrial) {
                return ($trialEnd && $trialEnd > $now) ? $sub : null;
            } else {
                return ($periodEnd && $periodEnd > $now) ? $sub : null;
            }
        }
        
        return null;
    }

    /**
     * Get user's subscription history
     */
    public function getByUserId($user_id) {
        $query = "SELECT * FROM " . $this->table . " 
                  WHERE user_id = :user_id 
                  ORDER BY created_at DESC";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    /**
     * Check if user has premium access
     * 
     * LOGIC:
     * - Trial user (is_trial=1, no ₹299 paid): Access until trial_end (7 days)
     * - Paid user (is_trial=0, ₹299 paid): Access until current_period_end (1 month)
     * - Cancelled: Still has access until their period ends
     */
    public function isUserPremium($user_id) {
        // Get latest subscription
        $query = "SELECT * FROM " . $this->table . " 
                  WHERE user_id = :user_id 
                  ORDER BY created_at DESC 
                  LIMIT 1";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
        $stmt->execute();
        $sub = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$sub) {
            // No subscription - check manual premium in users table
            $query = "SELECT is_premium, premium_expires_at FROM users WHERE id = :user_id";
            $stmt = $this->conn->prepare($query);
            $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
            $stmt->execute();
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
            
            if ($user && $user['is_premium']) {
                if (!$user['premium_expires_at'] || strtotime($user['premium_expires_at']) > time()) {
                    return true;
                }
            }
            return false;
        }

        $now = time();
        $isTrial = (bool)$sub['is_trial']; // 1 = trial (₹5 only), 0 = paid (₹299)
        $trialEnd = $sub['trial_end'] ? strtotime($sub['trial_end']) : null;
        $periodEnd = $sub['current_period_end'] ? strtotime($sub['current_period_end']) : null;
        $razorpayStatus = $sub['razorpay_status'] ?? null;
        $internalStatus = $sub['status'] ?? null;
        
        // CANCELLED or EXPIRED - check remaining access
        if ($razorpayStatus && in_array($razorpayStatus, ['cancelled', 'expired', 'halted', 'completed'])) {
            if ($isTrial) {
                // Trial user: ONLY check trial_end (7 days max)
                return $trialEnd && $trialEnd > $now;
            } else {
                // Paid user: Check current_period_end (1 month)
                return $periodEnd && $periodEnd > $now;
            }
        }

        // Internal cancelled (webhook not received yet)
        if ($internalStatus === 'cancelled') {
            if ($isTrial) {
                return $trialEnd && $trialEnd > $now;
            } else {
                return $periodEnd && $periodEnd > $now;
            }
        }

        // ACTIVE subscription
        if (in_array($internalStatus, ['authenticated', 'active', 'pending', 'created'])) {
            if ($isTrial) {
                // Trial: 7 days only
                return $trialEnd && $trialEnd > $now;
            } else {
                // Paid: 1 month
                return $periodEnd && $periodEnd > $now;
            }
        }

        return false;
    }

    /**
     * Update user premium status
     */
    public function updateUserPremiumStatus($user_id, $is_premium, $expires_at = null) {
        $query = "UPDATE users 
                  SET is_premium = :is_premium, 
                      premium_expires_at = :expires_at,
                      subscription_id = :subscription_id
                  WHERE id = :user_id";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':is_premium', $is_premium, PDO::PARAM_BOOL);
        $stmt->bindParam(':expires_at', $expires_at);
        $stmt->bindParam(':subscription_id', $this->id, PDO::PARAM_INT);
        $stmt->bindParam(':user_id', $user_id, PDO::PARAM_INT);
        return $stmt->execute();
    }

    /**
     * Get all subscriptions for admin
     */
    public function getAllForAdmin($limit = 100, $offset = 0) {
        $query = "SELECT s.*, 
                         u.name AS user_name, 
                         u.mobile AS user_mobile, 
                         u.email AS user_email
                  FROM " . $this->table . " s
                  LEFT JOIN users u ON s.user_id = u.id
                  ORDER BY s.created_at DESC
                  LIMIT :limit OFFSET :offset";

        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
        $stmt->bindParam(':offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    /**
     * Get subscription statistics
     */
    public function getStats() {
        $query = "SELECT 
                    COUNT(*) AS total_subscriptions,
                    SUM(CASE WHEN status = 'active' THEN 1 ELSE 0 END) AS active_count,
                    SUM(CASE WHEN status = 'authenticated' THEN 1 ELSE 0 END) AS authenticated_count,
                    SUM(CASE WHEN status = 'cancelled' THEN 1 ELSE 0 END) AS cancelled_count,
                    SUM(CASE WHEN is_trial = 1 AND status IN ('active', 'authenticated') THEN 1 ELSE 0 END) AS trial_count,
                    SUM(CASE WHEN status = 'active' AND is_trial = 0 THEN amount ELSE 0 END) AS monthly_revenue,
                    SUM(total_payments) AS total_payments_count
                  FROM " . $this->table;

        $stmt = $this->conn->prepare($query);
        $stmt->execute();
        return $stmt->fetch(PDO::FETCH_ASSOC);
    }
}
?>

