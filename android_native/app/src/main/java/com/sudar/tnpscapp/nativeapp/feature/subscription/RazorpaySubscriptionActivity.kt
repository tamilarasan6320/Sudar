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
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors
import com.sudar.tnpscapp.nativeapp.ui.theme.SudarTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var subscriptionId: String? = null
    private var statusMessage = mutableStateOf("Initializing payment...")
    private var isLoading = mutableStateOf(true)

    companion object {
        private const val TAG = "RazorpaySubscription"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            createSubscriptionAndOpenCheckout()
        }
    }

    private suspend fun createSubscriptionAndOpenCheckout() {
        try {
            statusMessage.value = "Creating subscription..."
            
            val userId = sessionStore.userId.first()
            if (userId == null) {
                showErrorAndFinish("User not logged in")
                return
            }

            // Step 1: Call backend to create subscription
            val response = withContext(Dispatchers.IO) {
                api.createSubscription(mapOf("user_id" to userId))
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

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        Log.d(TAG, "Payment success: paymentId=$razorpayPaymentId")
        
        isLoading.value = true
        statusMessage.value = "Verifying payment..."

        lifecycleScope.launch {
            verifyPayment(paymentData)
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

    private suspend fun verifyPayment(paymentData: PaymentData?) {
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
