package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.model.TestHistoryItemDto
import com.sudar.tnpscapp.nativeapp.core.repo.TestsRepository
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TestHistoryUiState(
    val isLoading: Boolean = true,
    val history: List<TestHistoryItemDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TestHistoryViewModel @Inject constructor(
    private val testsRepository: TestsRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    var uiState by mutableStateOf(TestHistoryUiState())
        private set

    init {
        load()
    }

    fun load() {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val userId = sessionStore.userId.first()
                if (userId == null) {
                    uiState = uiState.copy(isLoading = false, error = "Please login again.")
                    return@launch
                }
                val res = testsRepository.history(userId = userId, limit = 50)
                val body = res.body()
                if (res.isSuccessful && body?.success == true) {
                    uiState = uiState.copy(isLoading = false, history = body.history.orEmpty())
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = body?.message ?: "Failed to load history (HTTP ${res.code()})",
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, error = e.message ?: "Network error")
            }
        }
    }
}

