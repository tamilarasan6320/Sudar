package com.sudar.tnpscapp.nativeapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.model.ExamCategoryDto
import com.sudar.tnpscapp.nativeapp.core.network.model.TestCategoryDto
import com.sudar.tnpscapp.nativeapp.core.network.model.TestHistoryItemDto
import com.sudar.tnpscapp.nativeapp.core.repo.ExamRepository
import com.sudar.tnpscapp.nativeapp.core.repo.TestsRepository
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoadingStats: Boolean = true,
    val isLoadingCategories: Boolean = true,
    val isLoadingExams: Boolean = true,
    val currentExam: String = "Select Exam",
    val selectedExamId: Int? = null,
    val isPremium: Boolean = false,
    val testsTaken: Int = 0,
    val avgScore: Double = 0.0,
    val userRank: Int = 0,
    val testCategories: List<TestCategoryDto> = emptyList(),
    val examCategories: List<ExamCategoryDto> = emptyList(),
    val recentTests: List<TestHistoryItemDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val examRepository: ExamRepository,
    private val testsRepository: TestsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // For backward compatibility
    val selectedExam: StateFlow<String?> = _uiState.map { it.currentExam }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load saved exam from session
            val savedExam = sessionStore.getSelectedExamName()
            val savedExamId = sessionStore.getSelectedExamId()
            val isPremium = sessionStore.isPremium()

            _uiState.update {
                it.copy(
                    currentExam = savedExam ?: "Select Exam",
                    selectedExamId = savedExamId,
                    isPremium = isPremium
                )
            }

            // Load exam categories
            loadExamCategories()

            // Load user stats
            loadUserStats()
        }
    }

    fun loadExamCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingExams = true) }

            try {
                val response = examRepository.getExamCategories()
                if (response.isSuccessful && response.body()?.success == true) {
                    val categories = response.body()?.categories ?: emptyList()

                    _uiState.update { state ->
                        var newState = state.copy(
                            examCategories = categories,
                            isLoadingExams = false
                        )

                        // Auto-select first exam if none selected
                        if (state.selectedExamId == null && categories.isNotEmpty()) {
                            val firstExam = categories.first()
                            newState = newState.copy(
                                currentExam = firstExam.name,
                                selectedExamId = firstExam.id
                            )
                            // Save to session
                            viewModelScope.launch {
                                sessionStore.setSelectedExam(firstExam.name, firstExam.id)
                            }
                        }
                        newState
                    }

                    // Load test categories for selected exam
                    loadTestCategories()
                } else {
                    _uiState.update { it.copy(isLoadingExams = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingExams = false, error = e.message) }
            }
        }
    }

    fun loadTestCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategories = true) }

            try {
                val examId = _uiState.value.selectedExamId
                val userId = sessionStore.getUserId()

                val response = testsRepository.getCategories(examId, userId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val categories = response.body()?.categories ?: emptyList()
                    _uiState.update {
                        it.copy(
                            testCategories = categories,
                            isLoadingCategories = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingCategories = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingCategories = false, error = e.message) }
            }
        }
    }

    fun loadUserStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStats = true) }

            try {
                val userId = sessionStore.getUserId()
                if (userId != null) {
                    val historyResponse = testsRepository.history(userId, 50)
                    if (historyResponse.isSuccessful && historyResponse.body()?.success == true) {
                        val history = historyResponse.body()?.history ?: emptyList()

                        val testsTaken = history.size
                        val avgScore = if (history.isNotEmpty()) {
                            history.mapNotNull { it.percentage?.toDouble() }.average()
                        } else 0.0
                        val recentTests = history.take(3)

                        _uiState.update {
                            it.copy(
                                testsTaken = testsTaken,
                                avgScore = avgScore,
                                recentTests = recentTests,
                                isLoadingStats = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoadingStats = false) }
                    }
                } else {
                    _uiState.update { it.copy(isLoadingStats = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingStats = false, error = e.message) }
            }
        }
    }

    fun selectExam(examId: Int, examName: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentExam = examName,
                    selectedExamId = examId
                )
            }
            sessionStore.setSelectedExam(examName, examId)
            loadTestCategories()
        }
    }

    fun refresh() {
        loadTestCategories()
        loadUserStats()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
