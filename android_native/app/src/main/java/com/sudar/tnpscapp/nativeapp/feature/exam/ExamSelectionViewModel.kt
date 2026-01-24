package com.sudar.tnpscapp.nativeapp.feature.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.ExamCategoryDto
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExamSelectionUiState(
    val isLoadingCategories: Boolean = true,
    val examCategories: List<ExamCategoryDto> = emptyList(),
    val selectedExam: ExamCategoryDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectionComplete: Boolean = false
)

@HiltViewModel
class ExamSelectionViewModel @Inject constructor(
    private val api: SudarApi,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamSelectionUiState())
    val uiState: StateFlow<ExamSelectionUiState> = _uiState.asStateFlow()

    init {
        loadExamCategories()
    }

    private fun loadExamCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategories = true) }

            try {
                val response = api.examCategories()

                if (response.isSuccessful && response.body()?.success == true) {
                    val categories = response.body()?.categories ?: emptyList()

                    // Auto-select Group 4 if available
                    val group4 = categories.firstOrNull { it.name.contains("Group 4") }
                        ?: categories.firstOrNull()

                    _uiState.update {
                        it.copy(
                            isLoadingCategories = false,
                            examCategories = categories,
                            selectedExam = group4
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingCategories = false,
                            error = response.body()?.message ?: "Failed to load exams"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingCategories = false,
                        error = "Failed to load exams. Please try again."
                    )
                }
            }
        }
    }

    fun selectExam(exam: ExamCategoryDto) {
        _uiState.update { it.copy(selectedExam = exam) }
    }

    fun confirmSelection() {
        val selectedExam = _uiState.value.selectedExam ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Save selected exam to session
                sessionStore.setSelectedExam(selectedExam.name, selectedExam.id)

                // Check premium status
                val userId = sessionStore.getUserId()
                if (userId != null) {
                    try {
                        val statusResponse = api.subscriptionStatus(userId)
                        if (statusResponse.isSuccessful && statusResponse.body()?.success == true) {
                            val isPremium = statusResponse.body()?.isPremium ?: false
                            sessionStore.setPremium(isPremium)
                        }
                    } catch (e: Exception) {
                        // Ignore - use cached value
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectionComplete = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to save selection. Please try again."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
