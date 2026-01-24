package com.sudar.tnpscapp.nativeapp.feature.progress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PerformanceUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedPeriod: String = "all",
    
    // Overall stats
    val avgScore: Float = 0f,
    val totalTests: Int = 0,
    val avgTimeMinutes: Float = 0f,
    val bestScore: Float = 0f,
    val passed: Int = 0,
    val failed: Int = 0,
    
    // Trends data (for chart)
    val trends: List<TrendItem> = emptyList(),
    
    // Subject performance
    val subjects: List<SubjectPerformance> = emptyList(),
    
    // Strengths & weaknesses
    val strengths: List<AnalysisItem> = emptyList(),
    val weaknesses: List<AnalysisItem> = emptyList()
)

data class TrendItem(
    val testName: String,
    val score: Float,
    val date: String
)

data class SubjectPerformance(
    val categoryName: String,
    val avgScore: Float,
    val passed: Int,
    val testCount: Int
)

data class AnalysisItem(
    val category: String,
    val score: Float
)

@HiltViewModel
class PerformanceViewModel @Inject constructor(
    private val api: SudarApi,
    private val sessionStore: SessionStore
) : ViewModel() {

    var uiState by mutableStateOf(PerformanceUiState())
        private set

    init {
        loadPerformance()
    }

    fun loadPerformance(period: String = uiState.selectedPeriod) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null, selectedPeriod = period)
            
            try {
                val userId = sessionStore.getUserId()
                if (userId == null) {
                    uiState = uiState.copy(isLoading = false, error = "User not logged in")
                    return@launch
                }

                val response = api.getPerformance(userId, period)
                if (response.isSuccessful && response.body()?.get("success") == true) {
                    val data = response.body()!!
                    
                    // Parse overall stats
                    val overall = data["overall"] as? Map<*, *> ?: emptyMap<String, Any>()
                    
                    // Parse trends
                    val trendsRaw = data["trends"] as? List<*> ?: emptyList<Any>()
                    val trends = trendsRaw.mapNotNull { item ->
                        val map = item as? Map<*, *> ?: return@mapNotNull null
                        TrendItem(
                            testName = (map["test_name"] as? String) ?: "Test",
                            score = ((map["score"] as? Number) ?: 0).toFloat(),
                            date = (map["date"] as? String) ?: ""
                        )
                    }
                    
                    // Parse subjects
                    val subjectsRaw = data["subjects"] as? List<*> ?: emptyList<Any>()
                    val subjects = subjectsRaw.mapNotNull { item ->
                        val map = item as? Map<*, *> ?: return@mapNotNull null
                        SubjectPerformance(
                            categoryName = (map["category_name"] as? String) ?: "Unknown",
                            avgScore = ((map["avg_score"] as? Number) ?: 0).toFloat(),
                            passed = ((map["passed"] as? Number) ?: 0).toInt(),
                            testCount = ((map["test_count"] as? Number) ?: 0).toInt()
                        )
                    }
                    
                    // Parse strengths
                    val strengthsRaw = data["strengths"] as? List<*> ?: emptyList<Any>()
                    val strengths = strengthsRaw.mapNotNull { item ->
                        val map = item as? Map<*, *> ?: return@mapNotNull null
                        AnalysisItem(
                            category = (map["category"] as? String) ?: "Unknown",
                            score = ((map["score"] as? Number) ?: 0).toFloat()
                        )
                    }
                    
                    // Parse weaknesses
                    val weaknessesRaw = data["weaknesses"] as? List<*> ?: emptyList<Any>()
                    val weaknesses = weaknessesRaw.mapNotNull { item ->
                        val map = item as? Map<*, *> ?: return@mapNotNull null
                        AnalysisItem(
                            category = (map["category"] as? String) ?: "Unknown",
                            score = ((map["score"] as? Number) ?: 0).toFloat()
                        )
                    }
                    
                    uiState = uiState.copy(
                        isLoading = false,
                        avgScore = ((overall["avg_score"] as? Number) ?: 0).toFloat(),
                        totalTests = ((overall["total_tests"] as? Number) ?: 0).toInt(),
                        avgTimeMinutes = ((overall["avg_time_taken"] as? Number) ?: 0).toFloat() / 60f,
                        bestScore = ((overall["best_score"] as? Number) ?: 0).toFloat(),
                        passed = ((overall["passed"] as? Number) ?: 0).toInt(),
                        failed = ((overall["failed"] as? Number) ?: 0).toInt(),
                        trends = trends,
                        subjects = subjects,
                        strengths = strengths,
                        weaknesses = weaknesses
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = "Failed to load performance data"
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun changePeriod(period: String) {
        if (period != uiState.selectedPeriod) {
            loadPerformance(period)
        }
    }
}
