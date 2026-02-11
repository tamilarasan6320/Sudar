package com.sudar.tnpscapp.nativeapp.feature.subscription

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.services.FirebaseService
import com.sudar.tnpscapp.nativeapp.core.services.MetaAppEventsService
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors
import com.sudar.tnpscapp.nativeapp.ui.theme.SudarTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * Activity to handle Razorpay subscription checkout flow.
 * 
 * Flow:
 * 1. Call backend subscriptions/create.php to get subscription_id
 * 2. Open Razorpay Checkout with subscription_id
 * 3. On success, call subscriptions/verify.php to verify payment
 * 4. Update SessionStore to mark user as premium
 * 5. Return RESULT_OK or RESULT_CANCELED
 */
@AndroidEntryPoint
class RazorpaySubscriptionActivity : ComponentActivity(), PaymentResultWithDataListener {

    @Inject
    lateinit var api: SudarApi

    @Inject
    lateinit var sessionStore: SessionStore

    @Inject
    lateinit var metaAppEventsService: MetaAppEventsService

    @Inject
    lateinit var firebaseService: FirebaseService

    private var subscriptionId: String? = null
    private var statusMessage = mutableStateOf("Initializing payment...")
    private var isLoading = mutableStateOf(true)
    private var checkoutPaymentType: String = "legacy_5"
    private var upfrontAmountPaise: Int = 0

    private enum class Flow {
        LegacySubscription,
        TrialFeeThenSubscription
    }

    private var flow: Flow = Flow.LegacySubscription

    companion object {
        private const val TAG = "RazorpaySubscription"

        const val EXTRA_FLOW = "payment_flow"
        const val FLOW_LEGACY_SUBSCRIPTION = "legacy_subscription"
        const val FLOW_TRIAL_FEE_THEN_SUBSCRIPTION = "trial_fee_then_subscription"

        // Default preferred payment method to surface first in Razorpay Checkout UI
        // (user can still switch methods in the checkout screen).
        private const val DEFAULT_PREFERRED_PAYMENT_METHOD = "upi"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        flow = when (intent.getStringExtra(EXTRA_FLOW)) {
            FLOW_TRIAL_FEE_THEN_SUBSCRIPTION -> Flow.TrialFeeThenSubscription
            else -> Flow.LegacySubscription
        }

        setContent {
            SudarTheme {
                LoadingScreen(
                    message = statusMessage.value,
                    isLoading = isLoading.value
                )
            }
        }

        // Start the subscription creation flow
        lifecycleScope.launch {
            when (flow) {
                Flow.LegacySubscription -> {
                    createSubscriptionAndOpenCheckout(paymentType = "legacy_5")
                }
                Flow.TrialFeeThenSubscription -> {
                    // Single checkout: collect ₹2 upfront via subscription addons + authorize mandate
                    createSubscriptionAndOpenCheckout(paymentType = "trial_fee_2")
                }
            }
        }
    }

    private suspend fun createSubscriptionAndOpenCheckout(paymentType: String) {
        try {
            statusMessage.value = "Creating subscription..."
            checkoutPaymentType = paymentType
            
            // Log begin checkout event for analytics
            try {
                val beginValue = if (paymentType == "trial_fee_2") 2.0 else 299.0
                firebaseService.logBeginCheckout(
                    value = beginValue,
                    currency = "INR",
                    itemName = "Premium Monthly"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Begin checkout analytics failed: ${e.message}")
            }
            
            val userId = sessionStore.userId.first()
            if (userId == null) {
                showErrorAndFinish("User not logged in")
                return
            }

            // Step 1: Call backend to create subscription
            val req = mutableMapOf<String, Any?>(
                "user_id" to userId,
                "payment_type" to paymentType,
            )
            val response = withContext(Dispatchers.IO) {
                api.createSubscription(req)
            }

            if (!response.isSuccessful) {
                showErrorAndFinish("Failed to create subscription: ${response.code()}")
                return
            }

            val body = response.body()
            val success = body?.get("success") as? Boolean ?: false
            
            if (!success) {
                val message = body?.get("message") as? String ?: "Unknown error"
                showErrorAndFinish(message)
                return
            }

            @Suppress("UNCHECKED_CAST")
            val data = body["data"] as? Map<String, Any?> ?: run {
                showErrorAndFinish("Invalid response from server")
                return
            }

            // Cache dynamic amounts from backend (used for analytics values)
            upfrontAmountPaise = (data["upfront_amount"] as? Number)?.toInt()
                ?: if (paymentType == "trial_fee_2") 200 else 0

            // Extract checkout options from response
            val key = data["key"] as? String ?: run {
                showErrorAndFinish("Missing Razorpay key")
                return
            }
            subscriptionId = data["subscription_id"] as? String ?: run {
                showErrorAndFinish("Missing subscription ID")
                return
            }

            statusMessage.value = "Opening payment..."

            // Step 2: Open Razorpay Checkout
            openRazorpayCheckout(key, subscriptionId!!, data)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating subscription", e)
            showErrorAndFinish("Error: ${e.message}")
        }
    }

    private fun openRazorpayCheckout(key: String, subscriptionId: String, data: Map<String, Any?>) {
        try {
            val checkout = Checkout()
            checkout.setKeyID(key)

            val options = JSONObject()
            options.put("subscription_id", subscriptionId)
            options.put("name", data["name"] as? String ?: "TNPSC Study App")
            options.put("description", data["description"] as? String ?: "Premium Subscription")
            options.put("currency", data["currency"] as? String ?: "INR")
            
            // Add image if available
            (data["image"] as? String)?.let { options.put("image", it) }

            // Add prefill if available
            @Suppress("UNCHECKED_CAST")
            (data["prefill"] as? Map<String, Any?>)?.let { prefill ->
                val prefillObj = JSONObject()
                (prefill["name"] as? String)?.let { prefillObj.put("name", it) }
                (prefill["email"] as? String)?.let { prefillObj.put("email", it) }
                (prefill["contact"] as? String)?.let { prefillObj.put("contact", it) }
                options.put("prefill", prefillObj)
            }

            // Add theme if available
            @Suppress("UNCHECKED_CAST")
            (data["theme"] as? Map<String, Any?>)?.let { theme ->
                val themeObj = JSONObject()
                (theme["color"] as? String)?.let { themeObj.put("color", it) }
                options.put("theme", themeObj)
            }

            // Add notes if available
            @Suppress("UNCHECKED_CAST")
            (data["notes"] as? Map<String, Any?>)?.let { notes ->
                val notesObj = JSONObject()
                notes.forEach { (k, v) -> notesObj.put(k, v) }
                options.put("notes", notesObj)
            }

            // Prefer a specific payment method (ex: UPI first) in Razorpay Checkout UI.
            // Docs: Razorpay Checkout "Payment Methods Configuration" (config.display.sequence)
            applyPreferredPaymentMethod(options, DEFAULT_PREFERRED_PAYMENT_METHOD)

            Log.d(TAG, "Opening Razorpay checkout with options: $options")
            
            // Hide loading and open checkout
            isLoading.value = false
            checkout.open(this, options)

        } catch (e: Exception) {
            Log.e(TAG, "Error opening Razorpay checkout", e)
            Toast.makeText(this, "Error opening payment: ${e.message}", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun applyPreferredPaymentMethod(options: JSONObject, preferredMethod: String) {
        try {
            val preferred = preferredMethod.trim().lowercase()
            if (preferred.isBlank()) return

            val methods = listOf(preferred, "upi", "card", "netbanking", "wallet")
                .distinct()
                .filter { it.isNotBlank() }

            val sequence = JSONArray()
            methods.forEach { sequence.put(it) }

            val preferences = JSONObject().put("show_default_blocks", true)
            val display = JSONObject()
                .put("sequence", sequence)
                .put("preferences", preferences)

            val config = JSONObject().put("display", display)
            options.put("config", config)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply preferred payment method config: ${e.message}")
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        Log.d(TAG, "Payment success: paymentId=$razorpayPaymentId")
        
        isLoading.value = true
        statusMessage.value = "Verifying payment..."

        lifecycleScope.launch {
            verifySubscriptionMandate(paymentData)
        }
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        Log.e(TAG, "Payment error: code=$code, description=$description")
        
        val message = when (code) {
            Checkout.NETWORK_ERROR -> "Network error. Please check your connection."
            Checkout.INVALID_OPTIONS -> "Invalid payment options."
            Checkout.PAYMENT_CANCELED -> "Payment cancelled."
            Checkout.TLS_ERROR -> "Security error. Please try again."
            else -> description ?: "Payment failed"
        }

        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private suspend fun verifySubscriptionMandate(paymentData: PaymentData?) {
        try {
            val paymentId = paymentData?.paymentId
            val signature = paymentData?.signature
            val subId = subscriptionId // Use the class member variable

            if (paymentId == null || signature == null || subId == null) {
                showErrorAndFinish("Missing payment verification data")
                return
            }

            Log.d(TAG, "Verifying payment: paymentId=$paymentId, subscriptionId=$subId")

            // Step 3: Call backend to verify payment
            val response = withContext(Dispatchers.IO) {
                api.verifySubscription(
                    mapOf(
                        "razorpay_payment_id" to paymentId,
                        "razorpay_subscription_id" to subId,
                        "razorpay_signature" to signature
                    )
                )
            }

            if (!response.isSuccessful) {
                showErrorAndFinish("Verification failed: ${response.code()}")
                return
            }

            val body = response.body()
            val success = body?.get("success") as? Boolean ?: false

            if (!success) {
                val message = body?.get("message") as? String ?: "Verification failed"
                showErrorAndFinish(message)
                return
            }

            // Step 4: Update session store to mark user as premium
            sessionStore.setPremium(true)

            // Step 4.5: Log analytics events (Firebase + Meta) for attribution
            try {
                val userId = sessionStore.userId.first()
                val purchaseAmountInr = when {
                    upfrontAmountPaise > 0 -> upfrontAmountPaise / 100.0
                    checkoutPaymentType == "legacy_5" -> 5.0
                    else -> 299.0
                }
                val transactionId = "${checkoutPaymentType}_${subId}_${paymentId}"
                
                // Firebase Analytics
                if (userId != null) {
                    firebaseService.setUserId(userId.toString())
                }
                firebaseService.logPurchase(
                    amount = purchaseAmountInr,
                    currency = "INR",
                    transactionId = transactionId,
                    itemName = if (checkoutPaymentType == "trial_fee_2") {
                        "Premium Monthly Trial (₹2 verification)"
                    } else {
                        "Premium Monthly Trial"
                    }
                )
                
                // Meta (Facebook) App Events
                if (userId != null) {
                    metaAppEventsService.setUserId(userId.toString())
                }
                metaAppEventsService.logPurchase(
                    amount = purchaseAmountInr,
                    currency = "INR",
                    contentId = if (checkoutPaymentType == "trial_fee_2") {
                        "premium_monthly_trial_fee_2"
                    } else {
                        "premium_monthly_trial"
                    },
                    contentType = "subscription",
                    orderId = transactionId
                )
            } catch (e: Exception) {
                Log.e(TAG, "Analytics purchase event failed: ${e.message}")
            }

            Log.d(TAG, "Payment verified successfully!")
            
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@RazorpaySubscriptionActivity,
                    "🎉 Subscription activated! Your 7-day free trial has started.",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Step 5: Return success
            setResult(Activity.RESULT_OK)
            finish()

        } catch (e: Exception) {
            Log.e(TAG, "Error verifying payment", e)
            showErrorAndFinish("Verification error: ${e.message}")
        }
    }

    private suspend fun showErrorAndFinish(message: String) {
        Log.e(TAG, "Error: $message")
        withContext(Dispatchers.Main) {
            Toast.makeText(this@RazorpaySubscriptionActivity, message, Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}

@Composable
private fun LoadingScreen(
    message: String,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = AppColors.Primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            Text(
                text = message,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
