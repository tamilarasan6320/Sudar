package com.sudar.tnpscapp.nativeapp.feature.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.repo.AuthRepository
import com.sudar.tnpscapp.nativeapp.core.repo.TestsRepository
import com.sudar.tnpscapp.nativeapp.core.services.OneSignalService
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userName: String = "",
    val userMobile: String = "",
    val isPremium: Boolean = false,
    val testsTaken: Int = 0,
    val rank: Int = 0,
    val avgScore: Double = 0.0,
    val isLoadingStats: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
    private val testsRepository: TestsRepository,
    private val oneSignalService: OneSignalService
) : ViewModel() {

    companion object {
        private const val WHATSAPP_NUMBER = "918300321814"
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStats = true)

            // Load basic info from session
            val userName = sessionStore.userName.first() ?: "User"
            val userMobile = sessionStore.userMobile.first() ?: ""
            val isPremium = sessionStore.isPremium.first()

            _uiState.value = _uiState.value.copy(
                userName = userName,
                userMobile = userMobile,
                isPremium = isPremium
            )

            // Load stats
            try {
                val userId = sessionStore.userId.first()
                if (userId != null) {
                    // Get test history
                    val historyResult = testsRepository.getTestHistory(userId)
                    if (historyResult.isSuccess) {
                        val history = historyResult.getOrNull() ?: emptyList()
                        val testsTaken = history.size
                        val avgScore = if (history.isNotEmpty()) {
                            history.map { it.percentage }.average()
                        } else 0.0

                        _uiState.value = _uiState.value.copy(
                            testsTaken = testsTaken,
                            avgScore = avgScore
                        )
                    }

                    // Get rankings
                    val rankingsResult = testsRepository.getRankings()
                    if (rankingsResult.isSuccess) {
                        val rankings = rankingsResult.getOrNull() ?: emptyList()
                        val userRankIndex = rankings.indexOfFirst { it.userId == userId }
                        if (userRankIndex >= 0) {
                            _uiState.value = _uiState.value.copy(rank = userRankIndex + 1)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore errors, keep default values
            }

            _uiState.value = _uiState.value.copy(isLoadingStats = false)
        }
    }

    fun openWhatsAppSupport() {
        val whatsappUrl = "https://wa.me/$WHATSAPP_NUMBER?text=${Uri.encode("Hi, I need help with the Sudar TNPSC App.")}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // WhatsApp not installed - handle error
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Remove OneSignal user
            oneSignalService.removeUserId()
            // Clear session
            sessionStore.clearSession()
        }
    }
}
