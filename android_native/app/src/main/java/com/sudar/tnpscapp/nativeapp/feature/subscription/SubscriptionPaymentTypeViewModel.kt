package com.sudar.tnpscapp.nativeapp.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SubscriptionPaymentType(val value: String) {
    LegacyMandate5Refunded("legacy_5"),
    TrialFee2NonRefundable("trial_fee_2");

    companion object {
        fun fromRaw(raw: String?): SubscriptionPaymentType {
            val v = raw?.trim()?.lowercase()
            return when (v) {
                TrialFee2NonRefundable.value -> TrialFee2NonRefundable
                else -> LegacyMandate5Refunded
            }
        }
    }
}

data class SubscriptionPaymentTypeState(
    val isLoading: Boolean = true,
    val paymentType: SubscriptionPaymentType = SubscriptionPaymentType.LegacyMandate5Refunded,
)

@HiltViewModel
class SubscriptionPaymentTypeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionPaymentTypeState())
    val uiState: StateFlow<SubscriptionPaymentTypeState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = authRepository.getPublicSettings("subscription_payment_type")
            val raw = res.getOrNull()
            val value = when (raw) {
                is String -> raw
                is Map<*, *> -> raw["value"]?.toString() ?: raw["setting_value"]?.toString()
                else -> raw?.toString()
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    paymentType = SubscriptionPaymentType.fromRaw(value),
                )
            }
        }
    }
}

