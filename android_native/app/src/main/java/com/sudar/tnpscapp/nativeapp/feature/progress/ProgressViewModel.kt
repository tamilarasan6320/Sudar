package com.sudar.tnpscapp.nativeapp.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.repo.TestsRepository
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class PerformanceTrendItem(
    val score: Double
)

data class StrengthWeaknessItem(
    val name: String,
    val avgScore: Double
)

data class TestHistoryItem(
    val sessionName: String,
    val completedAt: String,
    val percentage: Double,
    val totalQuestions: Int
)

data class ProgressUiState(
    val isLoading: Boolean = true,
    val testsTaken: Int = 0,
    val avgScore: Double = 0.0,
    val rank: Int = 0,
    val streak: Int = 0,
    val testHistory: List<TestHistoryItem> = emptyList(),
    val performanceTrend: List<PerformanceTrendItem> = emptyList(),
    val strengths: List<StrengthWeaknessItem> = emptyList(),
    val weaknesses: List<StrengthWeaknessItem> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val testsRepository: TestsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    fun loadProgressData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val userId = sessionStore.userId.first()

            if (userId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "User not logged in. Please login again."
                )
                return@launch
            }

            try {
                // Load analytics data
                val analyticsResult = testsRepository.getProgressAnalytics(userId)
                if (analyticsResult.isSuccess) {
                    val analytics = analyticsResult.getOrNull()
                    if (analytics != null) {
                        _uiState.value = _uiState.value.copy(
                            testsTaken = analytics.totalTests,
                            avgScore = analytics.avgScore,
                            rank = analytics.rank,
                            streak = analytics.streak,
                            performanceTrend = analytics.performanceTrend.map { 
                                PerformanceTrendItem(it.score) 
                            },
                            strengths = analytics.strengths.map { 
                                StrengthWeaknessItem(it.name, it.avgScore) 
                            },
                            weaknesses = analytics.weaknesses.map { 
                                StrengthWeaknessItem(it.name, it.avgScore) 
                            }
                        )
                    }
                }

                // Load test history
                val historyResult = testsRepository.getTestHistory(userId)
                if (historyResult.isSuccess) {
                    val history = historyResult.getOrNull() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        testHistory = history.map {
                            TestHistoryItem(
                                sessionName = it.sessionName,
                                completedAt = it.completedAt ?: "",
                                percentage = it.percentage,
                                totalQuestions = it.totalQuestions
                            )
                        }
                    )
                }

                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Connection error: ${e.message}"
                )
            }
        }
    }

    fun formatDate(dateStr: String?): String {
        if (dateStr == null) return "Unknown date"
        
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val date = LocalDateTime.parse(dateStr, formatter)
            val now = LocalDateTime.now()
            val days = ChronoUnit.DAYS.between(date, now)

            when {
                days == 0L -> "Today"
                days == 1L -> "Yesterday"
                days < 7L -> "$days days ago"
                else -> "${date.dayOfMonth}/${date.monthValue}/${date.year}"
            }
        } catch (e: Exception) {
            dateStr
        }
    }
}
