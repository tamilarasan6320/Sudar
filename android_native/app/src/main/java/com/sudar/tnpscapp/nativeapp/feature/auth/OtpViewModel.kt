package com.sudar.tnpscapp.nativeapp.feature.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.SendOtpRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.VerifyOtpRequest
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

data class VerificationResult(
    val isNewUser: Boolean,
    val mobile: String,
    val token: String?
)

data class OtpUiState(
    val phoneNumber: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val isSendingOtp: Boolean = false,
    val isResending: Boolean = false,
    val resendTimer: Int = 60,
    val canResend: Boolean = false,
    val remainingAttempts: Int = 5,
    val error: String? = null,
    val snackbarMessage: String? = null,
    val verificationResult: VerificationResult? = null
)

@HiltViewModel
class OtpViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: SudarApi,
    private val sessionStore: SessionStore
) : ViewModel() {

    companion object {
        private const val WHATSAPP_NUMBER = "918300321814"
    }

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun setPhoneNumber(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone) }
    }

    fun updateOtp(otp: String) {
        _uiState.update { it.copy(otp = otp, error = null) }
    }

    fun sendInitialOtp() {
        if (_uiState.value.isSendingOtp) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingOtp = true) }

            try {
                val response = api.sendOtp(SendOtpRequest(_uiState.value.phoneNumber))

                if (response.isSuccessful && response.body()?.success == true) {
                    val maskedMobile = response.body()?.mobile ?: _uiState.value.phoneNumber
                    _uiState.update {
                        it.copy(
                            isSendingOtp = false,
                            snackbarMessage = "OTP sent to $maskedMobile"
                        )
                    }
                    startResendTimer()
                } else {
                    val errorMessage = response.body()?.message ?: "Failed to send OTP"
                    _uiState.update {
                        it.copy(
                            isSendingOtp = false,
                            canResend = true,
                            resendTimer = 0,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSendingOtp = false,
                        canResend = true,
                        resendTimer = 0,
                        error = "Failed to send OTP. Please try again."
                    )
                }
            }
        }
    }

    fun verifyOtp() {
        val otp = _uiState.value.otp
        if (otp.length != 6) {
            _uiState.update { it.copy(error = "Please enter complete 6-digit OTP") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val deviceId = sessionStore.ensureDeviceId()
                val response = api.verifyOtp(
                    VerifyOtpRequest(
                        mobile = _uiState.value.phoneNumber,
                        otp = otp,
                        deviceId = deviceId
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    val isNewUser = body.isNewUser ?: false
                    val token = body.token
                    val mobile = body.mobile ?: _uiState.value.phoneNumber

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

                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = "OTP verified successfully!",
                            verificationResult = VerificationResult(
                                isNewUser = isNewUser,
                                mobile = mobile,
                                token = token
                            )
                        )
                    }
                } else {
                    val body = response.body()
                    val errorCode = body?.errorCode
                    var errorMessage = body?.message ?: "Invalid or expired OTP"

                    if (errorCode == "INVALID_OTP") {
                        val remaining = body?.remainingAttempts
                        if (remaining != null) {
                            _uiState.update { it.copy(remainingAttempts = remaining) }
                        }
                        // Clear OTP on wrong attempt
                        _uiState.update { it.copy(otp = "") }
                    } else if (errorCode == "MAX_ATTEMPTS_EXCEEDED") {
                        _uiState.update { it.copy(otp = "") }
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Verification failed. Please try again."
                    )
                }
            }
        }
    }

    fun resendOtp() {
        if (!_uiState.value.canResend || _uiState.value.isResending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isResending = true) }

            try {
                val response = api.resendOtp(SendOtpRequest(_uiState.value.phoneNumber))

                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            remainingAttempts = 5,
                            otp = "",
                            error = null,
                            snackbarMessage = "New OTP sent to ${response.body()?.mobile ?: _uiState.value.phoneNumber}"
                        )
                    }

                    val resendIn = response.body()?.resendIn ?: 60
                    startResendTimer(resendIn)
                } else {
                    val body = response.body()
                    val errorCode = body?.errorCode
                    var errorMessage = body?.message ?: "Failed to resend OTP"

                    if (errorCode == "COOLDOWN_ACTIVE" || errorCode == "RATE_LIMITED") {
                        val retryAfter = body?.retryAfter
                        if (retryAfter != null && retryAfter > 0) {
                            startResendTimer(retryAfter)
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isResending = false,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isResending = false,
                        error = "Failed to resend OTP. Please try again."
                    )
                }
            }
        }
    }

    private fun startResendTimer(duration: Int = 60) {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                resendTimer = duration,
                canResend = false
            )
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.resendTimer > 0) {
                delay(1000)
                _uiState.update { it.copy(resendTimer = it.resendTimer - 1) }
            }
            _uiState.update { it.copy(canResend = true) }
        }
    }

    fun clearVerificationResult() {
        _uiState.update { it.copy(verificationResult = null) }
    }

    /**
     * Auto-verify OTP after SMS is read automatically.
     * Adds a small delay so user can see the OTP before verification.
     */
    fun autoVerifyAfterSmsRead() {
        viewModelScope.launch {
            delay(500) // Small delay to show OTP filled
            if (_uiState.value.otp.length == 6 && !_uiState.value.isLoading) {
                verifyOtp()
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun openWhatsAppSupport() {
        val phone = _uiState.value.phoneNumber
        val message = Uri.encode("Hi, I need help with OTP verification for mobile number $phone in the Sudar TNPSC App.")
        val whatsappUrl = "https://wa.me/$WHATSAPP_NUMBER?text=$message"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // WhatsApp not installed
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
