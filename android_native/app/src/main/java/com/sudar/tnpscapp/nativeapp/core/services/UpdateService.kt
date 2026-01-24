package com.sudar.tnpscapp.nativeapp.core.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * In-App Update Service
 * Uses Google Play's in-app update API for Android
 */
@Singleton
class UpdateService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "UpdateService"
        private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.sudar.tnpscapp"
        const val REQUEST_CODE_UPDATE = 100
    }

    private var appUpdateManager: AppUpdateManager? = null
    private var updateInfo: AppUpdateInfo? = null
    private var activityRef: WeakReference<Activity>? = null

    // Callback for when update download completes (flexible update)
    var onUpdateDownloaded: (() -> Unit)? = null

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                Log.d(TAG, "Update downloaded")
                onUpdateDownloaded?.invoke()
            }
            InstallStatus.INSTALLED -> {
                Log.d(TAG, "Update installed")
                unregisterListener()
            }
            InstallStatus.FAILED -> {
                Log.e(TAG, "Update failed")
            }
            else -> {
                Log.d(TAG, "Install status: ${state.installStatus()}")
            }
        }
    }

    /**
     * Check for updates from Play Store
     */
    suspend fun checkForUpdate(activity: Activity): Boolean = suspendCancellableCoroutine { cont ->
        activityRef = WeakReference(activity)
        
        try {
            appUpdateManager = AppUpdateManagerFactory.create(context)
            val appUpdateInfoTask = appUpdateManager?.appUpdateInfo

            appUpdateInfoTask?.addOnSuccessListener { info ->
                updateInfo = info
                val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE

                if (isUpdateAvailable) {
                    Log.d(TAG, "Update available!")
                    Log.d(TAG, "Available version code: ${info.availableVersionCode()}")
                    Log.d(TAG, "Update priority: ${info.updatePriority()}")
                    cont.resume(true)
                } else {
                    Log.d(TAG, "App is up to date")
                    cont.resume(false)
                }
            }?.addOnFailureListener { e ->
                Log.e(TAG, "Error checking for update: ${e.message}")
                cont.resume(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update: ${e.message}")
            cont.resume(false)
        }
    }

    /**
     * Start immediate (forced) update
     * This will block the app until update is complete
     */
    fun startImmediateUpdate(activity: Activity): Boolean {
        val info = updateInfo ?: return false
        val manager = appUpdateManager ?: return false

        if (!info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            Log.e(TAG, "Immediate update not allowed")
            return false
        }

        try {
            manager.startUpdateFlowForResult(
                info,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                REQUEST_CODE_UPDATE
            )
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting immediate update: ${e.message}")
            return false
        }
    }

    /**
     * Start flexible update
     * This downloads in the background
     */
    fun startFlexibleUpdate(activity: Activity): Boolean {
        val info = updateInfo ?: return false
        val manager = appUpdateManager ?: return false

        if (!info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            Log.e(TAG, "Flexible update not allowed")
            return false
        }

        try {
            manager.registerListener(installStateUpdatedListener)
            manager.startUpdateFlowForResult(
                info,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                REQUEST_CODE_UPDATE
            )
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting flexible update: ${e.message}")
            return false
        }
    }

    /**
     * Complete the flexible update (restart app)
     */
    fun completeFlexibleUpdate() {
        appUpdateManager?.completeUpdate()
    }

    /**
     * Open Play Store page for manual update
     */
    fun openPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Play Store: ${e.message}")
        }
    }

    /**
     * Check if update is already downloaded and ready to install
     */
    fun isUpdateReadyToInstall(): Boolean {
        return updateInfo?.installStatus() == InstallStatus.DOWNLOADED
    }

    /**
     * Get the available version code
     */
    fun getAvailableVersionCode(): Int {
        return updateInfo?.availableVersionCode() ?: 0
    }

    /**
     * Handle activity result from update flow
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == REQUEST_CODE_UPDATE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.e(TAG, "Update flow failed or cancelled: resultCode=$resultCode")
            }
        }
    }

    private fun unregisterListener() {
        appUpdateManager?.unregisterListener(installStateUpdatedListener)
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        unregisterListener()
        activityRef?.clear()
        activityRef = null
    }
}
