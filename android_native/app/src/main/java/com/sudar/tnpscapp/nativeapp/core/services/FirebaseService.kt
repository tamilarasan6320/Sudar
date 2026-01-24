package com.sudar.tnpscapp.nativeapp.core.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Firebase Service
 * Handles Firebase Analytics and Cloud Messaging
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
    private var fcmToken: String? = null

    /**
     * Initialize Firebase
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

            // Get FCM token
            getFCMToken()

            // Set up token refresh listener
            setupTokenRefreshListener()

            isInitialized = true
            Log.d(TAG, "Initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Initialization error: ${e.message}")
        }
    }

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
            Log.d(TAG, "Event logged: $eventName")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging event: ${e.message}")
        }
    }

    /**
     * Set user ID for analytics
     */
    fun setUserId(userId: String?) {
        analytics?.setUserId(userId)
        Log.d(TAG, "User ID set: $userId")
    }

    /**
     * Set user property
     */
    fun setUserProperty(name: String, value: String?) {
        analytics?.setUserProperty(name, value)
        Log.d(TAG, "User property set: $name = $value")
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
