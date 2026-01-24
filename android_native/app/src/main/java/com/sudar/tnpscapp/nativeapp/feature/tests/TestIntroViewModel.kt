package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.repo.StartupRepository
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PremiumCheckResult {
    object NotChecked : PremiumCheckResult()
    object Checking : PremiumCheckResult()
    data class Checked(val isPremium: Boolean) : PremiumCheckResult()
}

data class TestIntroUiState(
    val premiumCheckResult: PremiumCheckResult = PremiumCheckResult.NotChecked
)

/**
 * ViewModel for TestIntroScreen that handles premium status gate.
 * Matches Flutter behavior: check cached premium, refresh from server if not premium,
 * then block test start if still not premium.
 */
@HiltViewModel
class TestIntroViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val startupRepository: StartupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestIntroUiState())
    val uiState: StateFlow<TestIntroUiState> = _uiState.asStateFlow()

    /**
     * Called when user clicks "Start Exam" button.
     * ALWAYS refreshes premium status from server to catch expired trials.
     * This ensures users whose trial ended are properly blocked.
     */
    fun onStartExamClicked() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(premiumCheckResult = PremiumCheckResult.Checking)

            // Always check server for latest premium status (trial may have expired)
            var isPremium = false
            val userId = sessionStore.userId.first()
            
            if (userId != null) {
                try {
                    val response = startupRepository.subscriptionStatus(userId)
                    if (response.isSuccessful) {
                        isPremium = response.body()?.isPremium == true
                        // Update cached value with server response
                        sessionStore.setPremium(isPremium)
                    } else {
                        // Server error - fall back to cached value
                        isPremium = sessionStore.isPremium.first()
                    }
                } catch (_: Exception) {
                    // Network error - fall back to cached value
                    isPremium = sessionStore.isPremium.first()
                }
            }

            _uiState.value = _uiState.value.copy(
                premiumCheckResult = PremiumCheckResult.Checked(isPremium)
            )
        }
    }

    /**
     * Reset check result after navigation (to subscription offer or test)
     */
    fun resetCheckResult() {
        _uiState.value = _uiState.value.copy(premiumCheckResult = PremiumCheckResult.NotChecked)
    }
}
