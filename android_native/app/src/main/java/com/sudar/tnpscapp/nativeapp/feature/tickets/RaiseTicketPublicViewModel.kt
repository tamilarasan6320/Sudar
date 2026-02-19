package com.sudar.tnpscapp.nativeapp.feature.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.CreatePublicTicketRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RaiseTicketPublicUiState(
    val userName: String = "",
    val userMobile: String = "",
    val subject: String = "",
    val message: String = "",
    val category: String = "general",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdTicketId: Int? = null,
)

@HiltViewModel
class RaiseTicketPublicViewModel @Inject constructor(
    private val api: SudarApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RaiseTicketPublicUiState())
    val uiState: StateFlow<RaiseTicketPublicUiState> = _uiState.asStateFlow()

    fun setUserName(value: String) {
        _uiState.value = _uiState.value.copy(userName = value, error = null)
    }

    fun setUserMobile(value: String) {
        _uiState.value = _uiState.value.copy(userMobile = value, error = null)
    }

    fun setSubject(value: String) {
        _uiState.value = _uiState.value.copy(subject = value, error = null)
    }

    fun setMessage(value: String) {
        _uiState.value = _uiState.value.copy(message = value, error = null)
    }

    fun setCategory(value: String) {
        _uiState.value = _uiState.value.copy(category = value, error = null)
    }

    fun prefillMobileIfEmpty(mobile: String) {
        if (_uiState.value.userMobile.isBlank() && mobile.isNotBlank()) {
            _uiState.value = _uiState.value.copy(userMobile = mobile)
        }
    }

    fun submit() {
        val name = _uiState.value.userName.trim().ifEmpty { "Guest" }
        val mobile = _uiState.value.userMobile.trim()
        val subject = _uiState.value.subject.trim()
        val message = _uiState.value.message.trim()
        val category = _uiState.value.category.trim().ifEmpty { "general" }

        val digits = mobile.filter { it.isDigit() }
        if (digits.length < 10) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid mobile number")
            return
        }
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
                val res = api.createTicketPublic(
                    CreatePublicTicketRequest(
                        userName = name,
                        userMobile = digits,
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
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = "Error submitting ticket. Please try again.")
            }
        }
    }

    fun clearCreated() {
        _uiState.value = _uiState.value.copy(createdTicketId = null)
    }
}

