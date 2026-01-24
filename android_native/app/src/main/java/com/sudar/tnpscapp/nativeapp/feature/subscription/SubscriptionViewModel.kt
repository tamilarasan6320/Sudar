package com.sudar.tnpscapp.nativeapp.feature.subscription

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.services.PremiumVideoService
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val isPremium: Boolean = false,
    val isTrial: Boolean = false,
    val trialDaysLeft: Int? = null,
    val daysLeft: Int? = null,
    val premiumSource: String? = null,
    val error: String? = null,
    val premiumVideoPath: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
    private val api: SudarApi,
    private val premiumVideoService: PremiumVideoService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        // Load cached premium video path on init
        viewModelScope.launch {
            val videoPath = premiumVideoService.getCachedVideoPath()
            _uiState.value = _uiState.value.copy(premiumVideoPath = videoPath)
        }
    }

    fun loadSubscriptionStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val userId = sessionStore.userId.first()
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Please login to continue"
                    )
                    return@launch
                }

                val response = api.subscriptionStatus(userId)
                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        val isPremium = data.isPremium ?: false
                        val trialDaysLeft = data.trialDaysLeft
                        val daysLeft = data.daysLeft
                        val isTrial = trialDaysLeft != null && trialDaysLeft > 0

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isPremium = isPremium,
                            isTrial = isTrial,
                            trialDaysLeft = trialDaysLeft,
                            daysLeft = daysLeft,
                            premiumSource = data.premiumSource
                        )

                        // Update session
                        sessionStore.setPremium(isPremium)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load subscription status"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Connection error. Please try again."
                )
            }
        }
    }

    fun startSubscription() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)

            try {
                val userId = sessionStore.userId.first()
                if (userId == null) {
                    showError("Please login to continue")
                    _uiState.value = _uiState.value.copy(isProcessing = false)
                    return@launch
                }

                // Create subscription via API
                val response = api.createSubscription(mapOf("user_id" to userId))
                if (response.isSuccessful) {
                    val data = response.body()
                    val success = data?.get("success") as? Boolean ?: false
                    if (success) {
                        // TODO: Integrate with Razorpay SDK
                        // For now, show a message
                        showMessage("Razorpay integration pending. Contact support for subscription.")
                    } else {
                        val message = data?.get("message")?.toString() ?: "Failed to create subscription"
                        showError(message)
                    }
                } else {
                    showError("Failed to create subscription")
                }
            } catch (e: Exception) {
                showError("Connection error: ${e.message}")
            }

            _uiState.value = _uiState.value.copy(isProcessing = false)
        }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
