package com.sudar.tnpscapp.nativeapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val name: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            val userName = sessionStore.userName.first() ?: ""
            _uiState.value = _uiState.value.copy(
                name = userName,
                isLoading = false
            )
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, error = null)
    }

    fun saveProfile() {
        val name = _uiState.value.name.trim()

        if (name.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Name cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            try {
                sessionStore.updateUserName(name)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saved = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save profile"
                )
            }
        }
    }
}
