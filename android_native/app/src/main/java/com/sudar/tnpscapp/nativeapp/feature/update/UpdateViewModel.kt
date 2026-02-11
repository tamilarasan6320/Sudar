package com.sudar.tnpscapp.nativeapp.feature.update

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.services.UpdateService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateUiState(
    val checking: Boolean = false,
    val updateAvailable: Boolean = false,
    val showFallbackDialog: Boolean = false,
    val availableVersionCode: Int = 0,
    val downloaded: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for coordinating Play In-App Update checks and UI state.
 * Matches Flutter's UpdateService behavior.
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateService: UpdateService
) : ViewModel() {

    companion object {
        private const val TAG = "UpdateViewModel"
    }

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        // Listen for flexible update download completion
        updateService.onUpdateDownloaded = {
            Log.d(TAG, "Update downloaded, showing restart prompt")
            _uiState.update { it.copy(downloaded = true) }
        }
    }

    /**
     * Check for updates from Play Store.
     * Call this on MainScreen composition.
     */
    fun check(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(checking = true, error = null) }

            try {
                val hasUpdate = updateService.checkForUpdate(activity)
                val versionCode = updateService.getAvailableVersionCode()

                Log.d(TAG, "Update check complete: hasUpdate=$hasUpdate, versionCode=$versionCode")

                _uiState.update {
                    it.copy(
                        checking = false,
                        updateAvailable = hasUpdate,
                        availableVersionCode = versionCode
                    )
                }

                // Auto-start immediate update if available (like Flutter)
                if (hasUpdate) {
                    startImmediate(activity)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed: ${e.message}")
                _uiState.update {
                    it.copy(
                        checking = false,
                        error = e.message
                    )
                }
            }
        }
    }

    /**
     * Start immediate (forced) update.
     * If not allowed, show fallback dialog.
     */
    fun startImmediate(activity: Activity) {
        Log.d(TAG, "Starting immediate update...")
        val started = updateService.startImmediateUpdate(activity)

        if (!started) {
            Log.d(TAG, "Immediate update not allowed, showing fallback dialog")
            // Immediate not allowed - show fallback dialog
            _uiState.update { it.copy(showFallbackDialog = true) }
        }
    }

    /**
     * Start flexible update (background download).
     */
    fun startFlexible(activity: Activity) {
        Log.d(TAG, "Starting flexible update...")
        val started = updateService.startFlexibleUpdate(activity)

        if (!started) {
            Log.d(TAG, "Flexible update not allowed, showing fallback dialog")
            _uiState.update { it.copy(showFallbackDialog = true) }
        } else {
            // Hide dialog while downloading
            _uiState.update { it.copy(showFallbackDialog = false) }
        }
    }

    /**
     * Open Play Store page for manual update.
     */
    fun openPlayStore() {
        Log.d(TAG, "Opening Play Store...")
        updateService.openPlayStore()
        dismissDialog()
    }

    /**
     * Complete the flexible update (restart app).
     */
    fun completeFlexibleUpdate() {
        Log.d(TAG, "Completing flexible update (restarting)...")
        updateService.completeFlexibleUpdate()
    }

    /**
     * Dismiss the fallback dialog (user chose "Later").
     */
    fun dismissDialog() {
        _uiState.update { it.copy(showFallbackDialog = false) }
    }

    /**
     * Dismiss the downloaded snackbar.
     */
    fun dismissDownloadedPrompt() {
        _uiState.update { it.copy(downloaded = false) }
    }

    override fun onCleared() {
        super.onCleared()
        updateService.dispose()
    }
}
