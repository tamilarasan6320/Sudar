package com.sudar.tnpscapp.nativeapp.feature.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.TicketDto
import com.sudar.tnpscapp.nativeapp.core.network.model.TicketStatsDto
import com.sudar.tnpscapp.nativeapp.core.network.model.CloseTicketRequest
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketsUiState(
    val isLoading: Boolean = false,
    val tickets: List<TicketDto> = emptyList(),
    val stats: TicketStatsDto? = null,
    val error: String? = null,
)

@HiltViewModel
class TicketsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: SudarApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketsUiState(isLoading = true))
    val uiState: StateFlow<TicketsUiState> = _uiState.asStateFlow()

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val userId = sessionStore.userId.first()
                if (userId == null) {
                    _uiState.value = TicketsUiState(isLoading = false, error = "Please login to view tickets.")
                    return@launch
                }

                val res = api.myTickets(userId = userId, limit = 100, offset = 0)
                if (res.isSuccessful && res.body()?.success == true) {
                    val body = res.body()
                    _uiState.value = TicketsUiState(
                        isLoading = false,
                        tickets = body?.tickets.orEmpty(),
                        stats = body?.stats,
                        error = null
                    )
                } else {
                    _uiState.value = TicketsUiState(isLoading = false, error = "Failed to load tickets. Please try again.")
                }
            } catch (e: Exception) {
                _uiState.value = TicketsUiState(isLoading = false, error = "Error loading tickets. Please try again.")
            }
        }
    }

    fun closeTicket(ticketId: Int, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val userId = sessionStore.userId.first()
                if (userId == null) return@launch

                val res = api.closeTicket(CloseTicketRequest(userId = userId, id = ticketId))
                if (res.isSuccessful && res.body()?.success == true) {
                    load(refresh = true)
                    onDone?.invoke()
                }
            } catch (_: Exception) {
                // Ignore; UI can still show existing status
            }
        }
    }
}

