package com.sudar.tnpscapp.nativeapp.core.services

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.sudar.tnpscapp.nativeapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Meta (Facebook) App Events Service
 * 
 * Wraps Facebook SDK AppEventsLogger for analytics parity with Flutter.
 * 
 * Events tracked:
 * - fb_mobile_activate_app (app open)
 * - fb_mobile_complete_registration (new user profile created)
 * - fb_mobile_purchase (subscription/trial activated)
 * 
 * Verify in Meta Events Manager > Test Events
 */
@Singleton
class MetaAppEventsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MetaAppEvents"
    }

    private var logger: AppEventsLogger? = null
    private var initialized = false

    /**
     * Initialize the Facebook SDK and AppEventsLogger.
     * Call this once at app startup (non-fatal if fails).
     */
    fun initialize() {
        if (initialized) return
        
        try {
            Log.d(TAG, "════════════════════════════════════════════════════════════")
            Log.d(TAG, "[META SDK] INITIALIZING...")
            Log.d(TAG, "════════════════════════════════════════════════════════════")
            
            // SDK should auto-init via manifest, but ensure it's ready
            if (!FacebookSdk.isInitialized()) {
                FacebookSdk.sdkInitialize(context)
                Log.d(TAG, "[META SDK] ✅ FacebookSdk.sdkInitialize() called")
            } else {
                Log.d(TAG, "[META SDK] ✅ FacebookSdk already initialized")
            }

            // Debug logs (helpful for verifying events in Logcat)
            if (BuildConfig.DEBUG) {
                try {
                    FacebookSdk.setIsDebugEnabled(true)
                    FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
                    Log.d(TAG, "[META SDK] 🐞 Debug enabled (LoggingBehavior.APP_EVENTS)")
                } catch (e: Exception) {
                    Log.w(TAG, "[META SDK] Debug enable failed: ${e.message}")
                }
            }
            
            // Enable auto-logging of app events
            FacebookSdk.setAutoLogAppEventsEnabled(true)
            Log.d(TAG, "[META SDK] ✅ setAutoLogAppEventsEnabled(true)")
            
            // Enable advertiser ID collection for attribution
            FacebookSdk.setAdvertiserIDCollectionEnabled(true)
            Log.d(TAG, "[META SDK] ✅ setAdvertiserIDCollectionEnabled(true)")
            
            // Create the logger instance
            logger = AppEventsLogger.newLogger(context)
            Log.d(TAG, "[META SDK] ✅ AppEventsLogger created")
            
            // Log app activation on init
            AppEventsLogger.activateApp(context.applicationContext as android.app.Application)
            Log.d(TAG, "[META SDK] ✅ activateApp() called")
            
            initialized = true
            Log.d(TAG, "════════════════════════════════════════════════════════════")
            Log.d(TAG, "[META SDK] ✅ INITIALIZED SUCCESSFULLY")
            Log.d(TAG, "════════════════════════════════════════════════════════════")
            
            // Also trigger install event explicitly (like Flutter does)
            logInstall()
        } catch (e: Exception) {
            Log.e(TAG, "════════════════════════════════════════════════════════════")
            Log.e(TAG, "[META SDK] ❌ INIT FAILED: ${e.message}", e)
            Log.e(TAG, "════════════════════════════════════════════════════════════")
        }
    }

    /**
     * Set user ID for better attribution (call after login).
     */
    fun setUserId(userId: String) {
        try {
            Log.d(TAG, "[META SDK] setUserId($userId)...")
            AppEventsLogger.setUserID(userId)
            Log.d(TAG, "[META SDK] ✅ User ID set: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "[META SDK] ❌ setUserId FAILED: ${e.message}", e)
        }
    }

    /**
     * Clear user ID (call on logout).
     */
    fun clearUserId() {
        try {
            AppEventsLogger.clearUserID()
            Log.d(TAG, "[META SDK] ✅ User ID cleared")
        } catch (e: Exception) {
            Log.e(TAG, "[META SDK] ❌ clearUserId FAILED: ${e.message}", e)
        }
    }

    /**
     * Log app activation event (fb_mobile_activate_app).
     * Call this on each app open for better tracking.
     */
    fun logActivateApp() {
        try {
            Log.d(TAG, "[fb_mobile_activate_app] TRIGGERING...")
            AppEventsLogger.activateApp(context.applicationContext as Application)
            // Also log custom event for explicit tracking
            logger?.logEvent("fb_mobile_activate_app")
            logger?.flush()
            Log.d(TAG, "[fb_mobile_activate_app] ✅ SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "[fb_mobile_activate_app] ❌ FAILED: ${e.message}", e)
        }
    }

    /**
     * Log install event (fb_mobile_install).
     * Note: SDK auto-logs this via manifest config, but this can be called
     * explicitly for testing or to ensure the event fires.
     */
    fun logInstall() {
        try {
            Log.d(TAG, "════════════════════════════════════════════════════════════")
            Log.d(TAG, "[fb_mobile_install] TRIGGERING...")
            Log.d(TAG, "════════════════════════════════════════════════════════════")
            
            val params = Bundle().apply {
                putString("fb_mobile_launch_source", "Organic")
            }
            logger?.logEvent("fb_mobile_install", params)
            logger?.flush()
            
            Log.d(TAG, "[fb_mobile_install] ✅ SUCCESS - Event sent to Facebook")
            Log.d(TAG, "════════════════════════════════════════════════════════════")
        } catch (e: Exception) {
            Log.e(TAG, "[fb_mobile_install] ❌ FAILED: ${e.message}", e)
        }
    }

    /**
     * Test install event - for debugging/verification.
     * Same as logInstall() but with extra logging.
     */
    fun testInstallEvent() {
        logInstall()
    }

    /**
     * Log CompleteRegistration standard event (fb_mobile_complete_registration).
     * Call this after a new user completes profile setup.
     * 
     * @param registrationMethod e.g. "otp", "truecaller", "phone"
     */
    fun logCompleteRegistration(registrationMethod: String) {
        try {
            Log.d(TAG, "[fb_mobile_complete_registration] TRIGGERING... (method: $registrationMethod)")
            
            val params = Bundle().apply {
                putString(AppEventsConstants.EVENT_PARAM_REGISTRATION_METHOD, registrationMethod)
            }
            
            logger?.logEvent(AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION, params)
            logger?.flush()
            Log.d(TAG, "[fb_mobile_complete_registration] ✅ SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "[fb_mobile_complete_registration] ❌ FAILED: ${e.message}", e)
        }
    }

    /**
     * Log Purchase standard event (fb_mobile_purchase).
     * Call this after payment verification succeeds.
     * 
     * @param amount Total amount (e.g. 299.0)
     * @param currency ISO 4217 currency code (e.g. "INR")
     * @param contentId Optional product/plan ID
     * @param contentType Optional type (e.g. "subscription")
     * @param orderId Optional order/transaction ID for deduplication
     */
    fun logPurchase(
        amount: Double,
        currency: String,
        contentId: String? = null,
        contentType: String? = null,
        orderId: String? = null
    ) {
        try {
            if (amount <= 0.0) {
                Log.e(TAG, "[fb_mobile_purchase] ❌ Skipped: amount must be > 0 (got $amount)")
                return
            }

            Log.d(TAG, "════════════════════════════════════════════════════════════")
            Log.d(TAG, "[fb_mobile_purchase] TRIGGERING...")
            Log.d(TAG, "[fb_mobile_purchase] Amount: $amount $currency")
            Log.d(TAG, "[fb_mobile_purchase] ContentId: $contentId")
            Log.d(TAG, "[fb_mobile_purchase] OrderId: $orderId")
            Log.d(TAG, "════════════════════════════════════════════════════════════")
            
            val params = Bundle().apply {
                // Explicit value/currency params for Meta Diagnostics
                putDouble(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM, amount)
                putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currency)
                // Alternative field names Meta may also check
                putDouble("value", amount)
                putString("currency", currency)
                // Standard content params
                contentId?.let { putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, it) }
                contentType?.let { putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, it) }
                orderId?.let { putString(AppEventsConstants.EVENT_PARAM_ORDER_ID, it) }
            }
            
            logger?.logPurchase(
                BigDecimal.valueOf(amount),
                Currency.getInstance(currency),
                params
            )
            logger?.flush()
            
            Log.d(TAG, "[fb_mobile_purchase] ✅ SUCCESS")
            Log.d(TAG, "════════════════════════════════════════════════════════════")
        } catch (e: Exception) {
            Log.e(TAG, "[fb_mobile_purchase] ❌ FAILED: ${e.message}", e)
        }
    }

    /**
     * Log a custom event (for any non-standard tracking needs).
     * 
     * @param eventName Custom event name
     * @param parameters Optional key-value parameters
     * @param valueToSum Optional numeric value to aggregate
     */
    fun logCustomEvent(
        eventName: String,
        parameters: Bundle? = null,
        valueToSum: Double? = null
    ) {
        try {
            Log.d(TAG, "[$eventName] TRIGGERING...")
            
            if (valueToSum != null) {
                logger?.logEvent(eventName, valueToSum, parameters)
            } else {
                logger?.logEvent(eventName, parameters)
            }
            logger?.flush()
            
            Log.d(TAG, "[$eventName] ✅ SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "[$eventName] ❌ FAILED: ${e.message}", e)
        }
    }
}
