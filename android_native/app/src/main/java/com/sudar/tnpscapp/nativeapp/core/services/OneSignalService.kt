package com.sudar.tnpscapp.nativeapp.core.services

import android.content.Context
import android.util.Log
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationWillDisplayEvent
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.notifications.INotificationLifecycleListener
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OneSignal Push Notification Service
 * Handles all push notification functionality matching Flutter implementation
 */
@Singleton
class OneSignalService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "OneSignalService"
        private const val ONESIGNAL_APP_ID = "4ead7a7e-e376-41bf-8219-dd1a04e36da4"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isInitialized = false
    private var playerId: String? = null
    private var userId: String? = null

    // Callback for navigation on notification click
    var onNotificationClick: ((type: String?, targetId: String?) -> Unit)? = null

    /**
     * Initialize OneSignal
     * Call this in Application.onCreate() or main activity
     */
    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return
        }

        try {
            // Enable verbose logging in debug
            OneSignal.Debug.logLevel = LogLevel.VERBOSE

            // Initialize OneSignal
            OneSignal.initWithContext(context, ONESIGNAL_APP_ID)

            // Set up notification handlers
            setupNotificationHandlers()

            // Get player ID
            playerId = OneSignal.User.pushSubscription.id
            Log.d(TAG, "Player ID: $playerId")

            // Request notification permission using OneSignal's built-in method
            // This shows a system dialog on Android 13+
            serviceScope.launch {
                try {
                    val permissionGranted = OneSignal.Notifications.requestPermission(true)
                    Log.d(TAG, "OneSignal permission request result: $permissionGranted")
                } catch (e: Exception) {
                    Log.e(TAG, "Permission request error: ${e.message}")
                }
            }

            isInitialized = true
            Log.d(TAG, "Initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Initialization error: ${e.message}")
        }
    }

    private fun setupNotificationHandlers() {
        // Handle notification received while app is in foreground
        OneSignal.Notifications.addForegroundLifecycleListener(object : INotificationLifecycleListener {
            override fun onWillDisplay(event: INotificationWillDisplayEvent) {
                Log.d(TAG, "Notification received in foreground")
                Log.d(TAG, "Title: ${event.notification.title}")
                Log.d(TAG, "Body: ${event.notification.body}")
                // Let notification display
            }
        })

        // Handle notification clicked/tapped
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                Log.d(TAG, "Notification clicked")
                Log.d(TAG, "Title: ${event.notification.title}")
                Log.d(TAG, "Body: ${event.notification.body}")

                // Handle notification click action
                handleNotificationClick(event)
            }
        })

        // Handle subscription changes
        OneSignal.User.pushSubscription.addObserver(object : IPushSubscriptionObserver {
            override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                Log.d(TAG, "Subscription changed")
                val newId = state.current.id
                if (newId != null && newId.isNotEmpty()) {
                    val previousId = playerId
                    playerId = newId
                    Log.d(TAG, "Player ID updated: $newId")

                    // Send to backend if changed
                    if (newId != previousId) {
                        serviceScope.launch {
                            // TODO: Send player ID to backend
                        }
                    }
                }
            }
        })
    }

    private fun handleNotificationClick(event: INotificationClickEvent) {
        val additionalData = event.notification.additionalData
        if (additionalData != null) {
            val type = additionalData.optString("type", null)
            val targetId = additionalData.optString("target_id", null)

            Log.d(TAG, "Notification type: $type")
            Log.d(TAG, "Target ID: $targetId")

            onNotificationClick?.invoke(type, targetId)
        }
    }

    /**
     * Set user ID for targeted notifications
     * Call this after user logs in
     */
    suspend fun setUserId(newUserId: String) {
        try {
            userId = newUserId
            OneSignal.login(newUserId)
            Log.d(TAG, "User ID set: $newUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user ID: ${e.message}")
        }
    }

    /**
     * Remove user ID (logout)
     * Call this when user logs out
     */
    suspend fun removeUserId() {
        try {
            OneSignal.logout()
            userId = null
            Log.d(TAG, "User ID removed")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing user ID: ${e.message}")
        }
    }

    /**
     * Get current player ID
     */
    fun getPlayerId(): String? = playerId

    /**
     * Get current user ID
     */
    fun getUserId(): String? = userId

    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return OneSignal.Notifications.permission
    }

    /**
     * Request notification permission
     */
    fun requestPermission(onComplete: ((Boolean) -> Unit)? = null) {
        serviceScope.launch {
            val result = OneSignal.Notifications.requestPermission(true)
            onComplete?.invoke(result)
        }
    }

    /**
     * Send tags to OneSignal (for segmentation)
     */
    fun sendTags(tags: Map<String, String>) {
        try {
            tags.forEach { (key, value) ->
                OneSignal.User.addTag(key, value)
            }
            Log.d(TAG, "Tags sent: $tags")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending tags: ${e.message}")
        }
    }

    /**
     * Remove tags from OneSignal
     */
    fun removeTags(tagKeys: List<String>) {
        try {
            OneSignal.User.removeTags(tagKeys)
            Log.d(TAG, "Tags removed: $tagKeys")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing tags: ${e.message}")
        }
    }
}
