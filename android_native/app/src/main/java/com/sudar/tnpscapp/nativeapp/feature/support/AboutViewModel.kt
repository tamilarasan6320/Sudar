package com.sudar.tnpscapp.nativeapp.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AboutUiState(
    val isLoading: Boolean = true,
    val appName: String = "TNPSC Mock Test",
    val appVersion: String = "4.1.0",
    val description: String = "Prepare for TNPSC exams with comprehensive mock tests and detailed analytics.",
    val contactEmail: String = "",
    val contactPhone: String = ""
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    fun loadAboutData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val result = authRepository.getPublicSettings("about")
                if (result.isSuccess) {
                    val about = result.getOrNull()
                    @Suppress("UNCHECKED_CAST")
                    val aboutMap = about as? Map<String, Any?>
                    if (aboutMap != null) {
                        _uiState.value = _uiState.value.copy(
                            appName = aboutMap["app_name"]?.toString() ?: _uiState.value.appName,
                            appVersion = aboutMap["app_version"]?.toString() ?: _uiState.value.appVersion,
                            description = aboutMap["description"]?.toString() ?: _uiState.value.description,
                            contactEmail = aboutMap["contact_email"]?.toString() ?: "",
                            contactPhone = aboutMap["contact_phone"]?.toString() ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                // Use defaults - silently fail
                e.printStackTrace()
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
