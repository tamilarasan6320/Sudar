package com.sudar.tnpscapp.nativeapp.feature.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.CloseTicketRequest
import com.sudar.tnpscapp.nativeapp.core.network.model.TicketDto
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketDetailsUiState(
    val isLoading: Boolean = false,
    val ticket: TicketDto? = null,
    val error: String? = null,
    val isClosing: Boolean = false,
)

@HiltViewModel
class TicketDetailsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: SudarApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketDetailsUiState(isLoading = true))
    val uiState: StateFlow<TicketDetailsUiState> = _uiState.asStateFlow()

    fun load(ticketId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val userId = sessionStore.userId.first()
                if (userId == null) {
                    _uiState.value = TicketDetailsUiState(isLoading = false, error = "Please login to view this ticket.")
                    return@launch
                }

                val res = api.ticketDetails(userId = userId, ticketId = ticketId)
                if (res.isSuccessful && res.body()?.success == true) {
                    _uiState.value = TicketDetailsUiState(isLoading = false, ticket = res.body()?.ticket)
                } else {
                    _uiState.value = TicketDetailsUiState(isLoading = false, error = "Failed to load ticket details.")
                }
            } catch (_: Exception) {
                _uiState.value = TicketDetailsUiState(isLoading = false, error = "Error loading ticket details.")
            }
        }
    }

    fun close(ticketId: Int, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val userId = sessionStore.userId.first()
                if (userId == null) return@launch

                _uiState.value = _uiState.value.copy(isClosing = true)
                val res = api.closeTicket(CloseTicketRequest(userId = userId, id = ticketId))
                _uiState.value = _uiState.value.copy(isClosing = false)

                if (res.isSuccessful && res.body()?.success == true) {
                    load(ticketId)
                    onDone?.invoke()
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isClosing = false)
            }
        }
    }
}

