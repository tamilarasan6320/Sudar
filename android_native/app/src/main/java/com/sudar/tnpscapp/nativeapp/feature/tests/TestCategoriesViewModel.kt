package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.QuestionSessionDto
import com.sudar.tnpscapp.nativeapp.core.network.model.TestCategoryDto
import com.sudar.tnpscapp.nativeapp.core.network.model.TestHistoryItemDto
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TestCategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<TestCategoryDto> = emptyList(),
    val sessions: List<QuestionSessionDto> = emptyList(),
    val completedTests: Map<Int, TestHistoryItemDto> = emptyMap(),
    val retakeDialogSession: QuestionSessionDto? = null,
    val error: String? = null
)

@HiltViewModel
class TestCategoriesViewModel @Inject constructor(
    private val api: SudarApi,
    private val sessionStore: SessionStore
) : ViewModel() {

    var uiState by mutableStateOf(TestCategoriesUiState())
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            try {
                val userId = sessionStore.getUserId()
                val examId = sessionStore.getSelectedExamId()

                // Load categories
                val categoriesResponse = api.testCategories(examId = examId, userId = userId)
                val categories = if (categoriesResponse.isSuccessful && categoriesResponse.body()?.success == true) {
                    categoriesResponse.body()?.categories ?: emptyList()
                } else {
                    emptyList()
                }

                // Load all sessions
                val sessionsResponse = api.allSessions()
                val sessions = if (sessionsResponse.isSuccessful && sessionsResponse.body()?.success == true) {
                    sessionsResponse.body()?.sessions ?: emptyList()
                } else {
                    emptyList()
                }

                // Load completed tests
                val completedTests = mutableMapOf<Int, TestHistoryItemDto>()
                if (userId != null) {
                    val historyResponse = api.testHistory(userId = userId, limit = 500)
                    if (historyResponse.isSuccessful && historyResponse.body()?.success == true) {
                        val history = historyResponse.body()?.history ?: emptyList()
                        for (test in history) {
                            val sessionId = test.sessionId
                            val existingScore = completedTests[sessionId]?.percentage ?: 0f
                            val newScore = test.percentage ?: 0f
                            if (!completedTests.containsKey(sessionId) || newScore > existingScore) {
                                completedTests[sessionId] = test
                            }
                        }
                    }
                }

                uiState = uiState.copy(
                    isLoading = false,
                    categories = categories,
                    sessions = sessions,
                    completedTests = completedTests
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun showRetakeDialog(session: QuestionSessionDto) {
        uiState = uiState.copy(retakeDialogSession = session)
    }

    fun dismissRetakeDialog() {
        uiState = uiState.copy(retakeDialogSession = null)
    }

    fun startTest(session: QuestionSessionDto) {
        // This would navigate to test page - handled by navigation
        // For now, just dismiss the dialog
        uiState = uiState.copy(retakeDialogSession = null)
    }
}
