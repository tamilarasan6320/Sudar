package com.sudar.tnpscapp.nativeapp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.network.model.CreateUserRequest
import com.sudar.tnpscapp.nativeapp.core.services.FirebaseService
import com.sudar.tnpscapp.nativeapp.core.services.MetaAppEventsService
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import com.sudar.tnpscapp.nativeapp.core.session.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSetupUiState(
    val mobileNumber: String = "",
    val isNewUser: Boolean = true,
    val token: String? = null,
    val verificationMethod: String = "otp",
    val isLoading: Boolean = false,
    val error: String? = null,
    val profileComplete: Boolean = false
)

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val api: SudarApi,
    private val sessionStore: SessionStore,
    private val metaAppEventsService: MetaAppEventsService,
    private val firebaseService: FirebaseService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    fun setInitialData(
        mobileNumber: String,
        isNewUser: Boolean,
        token: String?,
        verificationMethod: String
    ) {
        _uiState.update {
            it.copy(
                mobileNumber = mobileNumber,
                isNewUser = isNewUser,
                token = token,
                verificationMethod = verificationMethod
            )
        }
    }

    fun saveProfile(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val state = _uiState.value

                if (state.isNewUser) {
                    // Create new user
                    val deviceId = sessionStore.ensureDeviceId()
                    val response = api.createUser(
                        CreateUserRequest(
                            mobile = state.mobileNumber,
                            name = name,
                            deviceId = deviceId
                        )
                    )

                    if (response.isSuccessful && response.body()?.success == true) {
                        val body = response.body()!!
                        val user = body.user
                        val token = body.token

                        if (user != null) {
                            // Save session
                            sessionStore.setLoggedIn(
                                UserSession(
                                    id = user.id,
                                    name = user.name,
                                    mobile = user.mobile
                                ),
                                token ?: ""
                            )

                            // Log analytics events (Firebase + Meta) for attribution
                            try {
                                // Firebase Analytics
                                firebaseService.setUserId(user.id.toString())
                                firebaseService.logSignUp(state.verificationMethod)
                                
                                // Meta (Facebook) App Events
                                metaAppEventsService.setUserId(user.id.toString())
                                metaAppEventsService.logCompleteRegistration(state.verificationMethod)
                            } catch (e: Exception) {
                                // Ignore analytics errors
                            }

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    profileComplete = true
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Failed to create profile"
                                )
                            }
                        }
                    } else {
                        val errorMessage = response.body()?.message ?: "Failed to create profile"
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = errorMessage
                            )
                        }
                    }
                } else {
                    // Existing user - just save the session
                    // This case shouldn't normally happen from ProfileSetup
                    // but handle it gracefully
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profileComplete = true
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An error occurred"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
