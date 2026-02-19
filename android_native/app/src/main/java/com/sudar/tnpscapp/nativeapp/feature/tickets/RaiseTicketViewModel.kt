package com.sudar.tnpscapp.nativeapp.feature.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.CreateTicketRequest
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RaiseTicketUiState(
    val subject: String = "",
    val message: String = "",
    val category: String = "general",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdTicketId: Int? = null,
)

@HiltViewModel
class RaiseTicketViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: SudarApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RaiseTicketUiState())
    val uiState: StateFlow<RaiseTicketUiState> = _uiState.asStateFlow()

    fun setSubject(value: String) {
        _uiState.value = _uiState.value.copy(subject = value, error = null)
    }

    fun setMessage(value: String) {
        _uiState.value = _uiState.value.copy(message = value, error = null)
    }

    fun setCategory(value: String) {
        _uiState.value = _uiState.value.copy(category = value, error = null)
    }

    fun submit() {
        val subject = _uiState.value.subject.trim()
        val message = _uiState.value.message.trim()
        val category = _uiState.value.category.trim().ifEmpty { "general" }

        if (subject.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a subject")
            return
        }
        if (message.length < 10) {
            _uiState.value = _uiState.value.copy(error = "Please provide more details (at least 10 characters)")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null, createdTicketId = null)
            try {
                val userId = sessionStore.userId.first()
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, error = "Please login to raise a ticket.")
                    return@launch
                }

                val res = api.createTicket(
                    CreateTicketRequest(
                        userId = userId,
                        subject = subject,
                        message = message,
                        category = category,
                    )
                )

                if (res.isSuccessful && res.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        createdTicketId = res.body()?.id,
                        error = null,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, error = "Failed to submit ticket. Please try again.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = "Error submitting ticket. Please try again.")
            }
        }
    }

    fun clearCreated() {
        _uiState.value = _uiState.value.copy(createdTicketId = null)
    }
}

