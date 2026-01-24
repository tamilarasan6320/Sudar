package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.model.TestDetailsAnswerDto
import com.sudar.tnpscapp.nativeapp.core.network.model.TestDetailsResultDto
import com.sudar.tnpscapp.nativeapp.core.repo.TestsRepository
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TestResultsUiState(
    val isLoading: Boolean = true,
    val result: TestDetailsResultDto? = null,
    val answers: List<TestDetailsAnswerDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TestResultsViewModel @Inject constructor(
    private val testsRepository: TestsRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    var uiState by mutableStateOf(TestResultsUiState())
        private set

    fun load(resultId: Int) {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val userId = sessionStore.userId.first()
                val res = testsRepository.testDetails(resultId = resultId, userId = userId)
                val body = res.body()
                if (res.isSuccessful && body?.success == true) {
                    uiState = uiState.copy(
                        isLoading = false,
                        result = body.result,
                        answers = body.answers.orEmpty(),
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = body?.message ?: "Failed to load results (HTTP ${res.code()})",
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, error = e.message ?: "Network error")
            }
        }
    }
}

