package com.sudar.tnpscapp.nativeapp.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackUiState(
    val feedback: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val submitted: Boolean = false
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: SudarApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun updateFeedback(feedback: String) {
        _uiState.value = _uiState.value.copy(feedback = feedback, error = null)
    }

    fun submitFeedback() {
        val feedback = _uiState.value.feedback.trim()

        if (feedback.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your feedback")
            return
        }

        if (feedback.length < 10) {
            _uiState.value = _uiState.value.copy(error = "Please provide more details (at least 10 characters)")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

            try {
                val userId = sessionStore.userId.first()
                val userName = sessionStore.userName.first() ?: "Anonymous"
                val userMobile = sessionStore.userMobile.first()

                val body = mapOf(
                    "message" to feedback,
                    "subject" to "App Feedback",
                    "user_id" to userId,
                    "user_name" to userName,
                    "user_mobile" to userMobile,
                    "category" to "general"
                )

                val response = api.submitFeedback(body)
                if (response.isSuccessful && response.body()?.get("success") == true) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitted = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = "Failed to submit feedback. Please try again."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Error submitting feedback. Please try again."
                )
            }
        }
    }
}
