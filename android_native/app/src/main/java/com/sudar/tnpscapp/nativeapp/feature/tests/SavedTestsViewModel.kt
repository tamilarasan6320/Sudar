package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.QuestionSessionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedTestsUiState(
    val isLoading: Boolean = true,
    val sessions: List<QuestionSessionDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SavedTestsViewModel @Inject constructor(
    private val api: SudarApi
) : ViewModel() {

    var uiState by mutableStateOf(SavedTestsUiState())
        private set

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            try {
                val response = api.getQuestionSessions()
                if (response.isSuccessful && response.body()?.success == true) {
                    uiState = uiState.copy(
                        isLoading = false,
                        sessions = response.body()?.sessions ?: emptyList()
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = response.body()?.message ?: "Failed to load sessions"
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
}
