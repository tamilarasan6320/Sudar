package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.model.QuestionSessionDto
import com.sudar.tnpscapp.nativeapp.core.repo.TestsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionsUiState(
    val isLoading: Boolean = true,
    val sessions: List<QuestionSessionDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val testsRepository: TestsRepository,
) : ViewModel() {
    var uiState by mutableStateOf(SessionsUiState())
        private set

    fun load(categoryId: Int) {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val res = testsRepository.getSessions(categoryId)
                val body = res.body()
                if (res.isSuccessful && body?.success == true) {
                    uiState = uiState.copy(
                        isLoading = false,
                        sessions = body.sessions.orEmpty(),
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = body?.message ?: "Failed to load sessions (HTTP ${res.code()})",
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, error = e.message ?: "Network error")
            }
        }
    }
}

