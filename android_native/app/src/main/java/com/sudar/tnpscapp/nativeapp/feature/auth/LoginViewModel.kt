package com.sudar.tnpscapp.nativeapp.feature.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.services.OneSignalService
import com.sudar.tnpscapp.nativeapp.core.services.TruecallerService
import com.sudar.tnpscapp.nativeapp.core.services.TruecallerResult as TcResult
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import com.sudar.tnpscapp.nativeapp.core.session.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TruecallerLoginResult(
    val isNewUser: Boolean,
    val mobile: String,
    val token: String?
)

data class LoginUiState(
    val isLoading: Boolean = false,
    val truecallerLoading: Boolean = false,
    val truecallerUsable: Boolean = false,
    val guestLoginEnabled: Boolean = false,
    val showSlowConnection: Boolean = false,
    val error: String? = null,
    val truecallerResult: TruecallerLoginResult? = null,
    val successMessage: String? = null,
    val showPhoneHint: Boolean = false // Trigger phone picker after Truecaller cancelled
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: SudarApi,
    private val sessionStore: SessionStore,
    private val truecallerService: TruecallerService,
    private val oneSignalService: OneSignalService
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
        private const val WHATSAPP_NUMBER = "918300321814"
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var slowConnectionJob: Job? = null
    private var truecallerInitialized = false

    init {
        loadGuestLoginSetting()
    }

    /**
     * Initialize Truecaller SDK with the activity and launcher
     * Must be called from the screen's LaunchedEffect
     */
    fun initTruecaller(activity: FragmentActivity, launcher: ActivityResultLauncher<Intent>) {
        if (truecallerInitialized) return
        
        viewModelScope.launch {
            try {
                truecallerService.initialize(activity, launcher)
                truecallerInitialized = true
                
                // Check if Truecaller is usable
                val isUsable = truecallerService.isUsable()
                Log.d(TAG, "Truecaller isUsable = $isUsable")
                
                _uiState.update { it.copy(truecallerUsable = isUsable) }
                
                // Auto-trigger Truecaller if available (like Flutter)
                if (isUsable) {
                    // Small delay to let the UI settle
                    delay(500)
                    handleTruecallerLogin()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Truecaller: ${e.message}")
                _uiState.update { it.copy(truecallerUsable = false) }
            }
        }
    }

    private fun loadGuestLoginSetting() {
        viewModelScope.launch {
            try {
                // Try to load from API
                val response = api.getPublicSettings("guest_login_enabled")
                if (response.isSuccessful) {
                    val value = response.body()?.get("value")?.toString()?.lowercase() ?: "false"
                    val enabled = value == "1" || value == "true" || value == "yes" || value == "on"
                    _uiState.update { it.copy(guestLoginEnabled = enabled) }
                }
            } catch (e: Exception) {
                // Ignore - keep default (false)
            }
        }
    }

    /**
     * Handle Truecaller login flow (callback-based, no coroutines for Truecaller)
     */
    fun handleTruecallerLogin() {
        Log.d(TAG, "handleTruecallerLogin called")
        
        if (_uiState.value.truecallerLoading || _uiState.value.isLoading) {
            Log.d(TAG, "Already loading, returning")
            return
        }

        Log.d(TAG, "Starting Truecaller login")
        _uiState.update { it.copy(truecallerLoading = true) }

        // Use callback-based approach - no coroutine suspension
        truecallerService.requestProfile { result ->
            Log.d(TAG, "Got result from requestProfile callback: ${result::class.simpleName}")
            _uiState.update { it.copy(truecallerLoading = false) }

            when (result) {
                is TcResult.Success -> {
                    Log.d(TAG, "=== TRUECALLER RESULT: SUCCESS ===")
                    Log.d(TAG, "Access token length: ${result.accessToken.length}")
                    processTruecallerLogin(result.accessToken)
                }
                is TcResult.Cancelled -> {
                    Log.d(TAG, "=== TRUECALLER RESULT: CANCELLED ===")
                    // User cancelled - show phone picker dialog
                    _uiState.update { it.copy(showPhoneHint = true) }
                }
                is TcResult.NotUsable -> {
                    Log.d(TAG, "=== TRUECALLER RESULT: NOT USABLE ===")
                    _uiState.update { it.copy(truecallerUsable = false) }
                }
                is TcResult.Error -> {
                    Log.e(TAG, "=== TRUECALLER RESULT: ERROR ===")
                    Log.e(TAG, "Error message: ${result.message}")
                    _uiState.update {
                        it.copy(error = result.message)
                    }
                }
            }
        }
    }

    /**
     * Process Truecaller login with backend verification
     */
    fun processTruecallerLogin(accessToken: String) {
        Log.d(TAG, "=== BACKEND TRUECALLER LOGIN START ===")
        Log.d(TAG, "Access token length: ${accessToken.length}")
        
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, showSlowConnection = false)
            }

            // Start slow connection timer (show message after 8 seconds)
            slowConnectionJob?.cancel()
            slowConnectionJob = viewModelScope.launch {
                delay(8000)
                Log.w(TAG, "Slow connection detected - showing message")
                _uiState.update { it.copy(showSlowConnection = true) }
            }

            try {
                // Get device ID (required by backend)
                val deviceId = sessionStore.ensureDeviceId()
                Log.d(TAG, "Device ID: $deviceId")
                
                Log.d(TAG, "Calling backend API: truecallerLogin")
                val startTime = System.currentTimeMillis()
                
                val response = api.truecallerLogin(mapOf(
                    "access_token" to accessToken,
                    "device_id" to deviceId
                ))
                
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "Backend API responded in ${duration}ms")

                slowConnectionJob?.cancel()
                
                Log.d(TAG, "Response code: ${response.code()}")
                Log.d(TAG, "Response success: ${response.body()?.success}")

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    val isNewUser = body.isNewUser ?: false
                    val token = body.token
                    val mobile = body.mobile ?: ""
                    
                    Log.d(TAG, "=== BACKEND LOGIN SUCCESS ===")
                    Log.d(TAG, "isNewUser: $isNewUser, mobile: $mobile")

                    if (!isNewUser && body.user != null) {
                        // Existing user - save session
                        val user = body.user
                        sessionStore.setLoggedIn(
                            UserSession(
                                id = user.id,
                                name = user.name,
                                mobile = user.mobile
                            ),
                            token ?: ""
                        )
                        
                        // Set OneSignal user ID for push notifications
                        try {
                            oneSignalService.setUserId(user.id.toString())
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting OneSignal user ID: ${e.message}")
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showSlowConnection = false,
                            successMessage = "Truecaller verified successfully!",
                            truecallerResult = TruecallerLoginResult(
                                isNewUser = isNewUser,
                                mobile = mobile,
                                token = token
                            )
                        )
                    }
                } else {
                    val errorMessage = response.body()?.message ?: "Truecaller login failed"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showSlowConnection = false,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                slowConnectionJob?.cancel()
                
                val isTimeout = e.message?.contains("timeout", ignoreCase = true) == true
                val errorMessage = if (isTimeout) {
                    "Connection timeout. Please check your internet and try again."
                } else {
                    "Login failed. Please try again."
                }
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showSlowConnection = false,
                        error = errorMessage
                    )
                }
            }
        }
    }

    fun cancelLogin() {
        slowConnectionJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = false,
                truecallerLoading = false,
                showSlowConnection = false
            )
        }
        Toast.makeText(context, "Login cancelled", Toast.LENGTH_SHORT).show()
    }

    fun clearTruecallerResult() {
        _uiState.update { it.copy(truecallerResult = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearPhoneHint() {
        _uiState.update { it.copy(showPhoneHint = false) }
    }

    fun openWhatsAppSupport() {
        val message = Uri.encode("Hi, I need help with login/OTP in the Sudar TNPSC App.")
        val whatsappUrl = "https://wa.me/$WHATSAPP_NUMBER?text=$message"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // WhatsApp not installed - show toast
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }
}
