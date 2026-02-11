package com.sudar.tnpscapp.nativeapp.core.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Firebase Service
 * Handles Firebase Analytics, Crashlytics, and Cloud Messaging
 * 
 * Analytics events tracked (same as Flutter GoogleAnalyticsService):
 * - app_open: on every app launch
 * - sign_up: on new user registration
 * - login: on existing user login
 * - purchase: on subscription/trial activation
 * - begin_checkout: when user starts subscription flow
 * 
 * View events in Firebase Console > Analytics > Events
 * View crashes in Firebase Console > Crashlytics
 */
@Singleton
class FirebaseService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FirebaseService"
    }

    private var isInitialized = false
    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null
    private var fcmToken: String? = null

    /**
     * Initialize Firebase Analytics, Crashlytics, and Messaging
     */
    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return
        }

        try {
            // Initialize Analytics
            analytics = FirebaseAnalytics.getInstance(context)
            analytics?.setAnalyticsCollectionEnabled(true)
            Log.d(TAG, "📊 [GA] Analytics initialized")

            // Initialize Crashlytics
            crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics?.setCrashlyticsCollectionEnabled(true)
            Log.d(TAG, "🔥 [Crashlytics] Initialized")

            // Get FCM token
            getFCMToken()

            // Set up token refresh listener
            setupTokenRefreshListener()

            isInitialized = true
            Log.d(TAG, "✅ Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase initialization error: ${e.message}")
            recordException(e)
        }
    }

    // ==========================================
    // ANALYTICS METHODS (matching Flutter)
    // ==========================================

    /**
     * Log app open event. Call this on each app open.
     */
    fun logAppOpen() {
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
            Log.d(TAG, "📊 [GA] App open logged")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [GA] Failed to log app open: ${e.message}")
        }
    }

    /**
     * Log sign up / complete registration event.
     * Call this after a new user completes profile setup.
     * 
     * @param signUpMethod e.g. "otp", "truecaller", "phone"
     */
    fun logSignUp(signUpMethod: String) {
        try {
            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, signUpMethod)
            }
            analytics?.logEvent(FirebaseAnalytics.Event.SIGN_UP, params)
            Log.d(TAG, "📊 [GA] SignUp logged (method: $signUpMethod)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [GA] Failed to log SignUp: ${e.message}")
        }
    }

    /**
     * Log login event for existing users.
     * 
     * @param loginMethod e.g. "otp", "truecaller", "phone"
     */
    fun logLogin(loginMethod: String) {
        try {
            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, loginMethod)
            }
            analytics?.logEvent(FirebaseAnalytics.Event.LOGIN, params)
            Log.d(TAG, "📊 [GA] Login logged (method: $loginMethod)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [GA] Failed to log login: ${e.message}")
        }
    }

    /**
     * Log Purchase event for trial/subscription activation.
     * Call this when 7-day trial or subscription is activated.
     * 
     * @param amount Total amount (e.g. 299.0)
     * @param currency ISO 4217 currency code (e.g. "INR")
     * @param transactionId Optional order/transaction ID
     * @param itemName Optional item name (e.g. "Premium Monthly")
     */
    fun logPurchase(
        amount: Double,
        currency: String,
        transactionId: String? = null,
        itemName: String? = null
    ) {
        try {
            val params = Bundle().apply {
                putDouble(FirebaseAnalytics.Param.VALUE, amount)
                putString(FirebaseAnalytics.Param.CURRENCY, currency)
                transactionId?.let { putString(FirebaseAnalytics.Param.TRANSACTION_ID, it) }
                itemName?.let { putString(FirebaseAnalytics.Param.ITEM_NAME, it) }
            }
            analytics?.logEvent(FirebaseAnalytics.Event.PURCHASE, params)
            Log.d(TAG, "📊 [GA] Purchase logged (amount: $amount $currency, txnId: $transactionId)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [GA] Failed to log Purchase: ${e.message}")
        }
    }

    /**
     * Log begin checkout event.
     * Call this when user starts the subscription flow.
     * 
     * @param value Checkout value
     * @param currency ISO 4217 currency code
     * @param itemName Optional item name
     */
    fun logBeginCheckout(
        value: Double,
        currency: String,
        itemName: String? = null
    ) {
        try {
            val params = Bundle().apply {
                putDouble(FirebaseAnalytics.Param.VALUE, value)
                putString(FirebaseAnalytics.Param.CURRENCY, currency)
                itemName?.let { putString(FirebaseAnalytics.Param.ITEM_NAME, it) }
            }
            analytics?.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, params)
            Log.d(TAG, "📊 [GA] BeginCheckout logged (value: $value $currency)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [GA] Failed to log BeginCheckout: ${e.message}")
        }
    }

    /**
     * Log a custom event to Firebase Analytics
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        try {
            val bundle = Bundle()
            params?.forEach { (key, value) ->
                when (value) {
                    is String -> bundle.putString(key, value)
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                }
            }
            analytics?.logEvent(eventName, bundle)
            Log.d(TAG, "📊 [GA] Event logged: $eventName")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [GA] Error logging event: ${e.message}")
        }
    }

    /**
     * Set user ID for analytics (call after login)
     */
    fun setUserId(userId: String?) {
        try {
            analytics?.setUserId(userId)
            crashlytics?.setUserId(userId ?: "")
            Log.d(TAG, "📊 [GA] User ID set: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [GA] Failed to set user ID: ${e.message}")
        }
    }

    /**
     * Clear user ID (call on logout)
     */
    fun clearUserId() {
        try {
            analytics?.setUserId(null)
            crashlytics?.setUserId("")
            Log.d(TAG, "📊 [GA] User ID cleared")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [GA] Failed to clear user ID: ${e.message}")
        }
    }

    /**
     * Set user property for segmentation
     */
    fun setUserProperty(name: String, value: String?) {
        try {
            analytics?.setUserProperty(name, value)
            crashlytics?.setCustomKey(name, value ?: "")
            Log.d(TAG, "📊 [GA] User property set: $name = $value")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [GA] Failed to set user property: ${e.message}")
        }
    }

    // ==========================================
    // CRASHLYTICS METHODS
    // ==========================================

    /**
     * Record a non-fatal exception to Crashlytics
     */
    fun recordException(exception: Throwable) {
        try {
            crashlytics?.recordException(exception)
            Log.d(TAG, "🔥 [Crashlytics] Exception recorded: ${exception.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Crashlytics] Failed to record exception: ${e.message}")
        }
    }

    /**
     * Log a message to Crashlytics (appears in crash reports)
     */
    fun log(message: String) {
        try {
            crashlytics?.log(message)
            Log.d(TAG, "🔥 [Crashlytics] Log: $message")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Crashlytics] Failed to log: ${e.message}")
        }
    }

    /**
     * Set a custom key-value pair (appears in crash reports)
     */
    fun setCustomKey(key: String, value: String) {
        try {
            crashlytics?.setCustomKey(key, value)
            Log.d(TAG, "🔥 [Crashlytics] Custom key set: $key = $value")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Crashlytics] Failed to set custom key: ${e.message}")
        }
    }

    /**
     * Force a test crash (only use in debug builds!)
     */
    fun testCrash() {
        throw RuntimeException("Test Crash from FirebaseService")
    }

    // ==========================================
    // FCM METHODS
    // ==========================================

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "Failed to get FCM token", task.exception)
                return@addOnCompleteListener
            }

            fcmToken = task.result
            Log.d(TAG, "FCM Token: $fcmToken")
        }
    }

    private fun setupTokenRefreshListener() {
        // Token refresh is handled automatically by FirebaseMessaging service
    }

    /**
     * Get the current FCM token
     */
    fun getFCMTokenSync(): String? = fcmToken

    /**
     * Get FCM token asynchronously
     */
    suspend fun getFCMTokenAsync(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
                cont.resume(fcmToken)
            } else {
                cont.resume(null)
            }
        }
    }

    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required on older versions
        }
    }

    /**
     * Subscribe to FCM topic
     */
    fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to topic: $topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to topic: $topic")
                }
            }
    }

    /**
     * Unsubscribe from FCM topic
     */
    fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Unsubscribed from topic: $topic")
                } else {
                    Log.e(TAG, "Failed to unsubscribe from topic: $topic")
                }
            }
    }
}
