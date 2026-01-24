package com.sudar.tnpscapp.nativeapp.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.repo.StartupRepository
import com.sudar.tnpscapp.nativeapp.core.services.FirebaseService
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val startupRepository: StartupRepository,
    private val firebaseService: FirebaseService
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    fun checkLoginStatus() {
        viewModelScope.launch {
            // Log app open for analytics
            try {
                firebaseService.logEvent("app_open", null)
            } catch (e: Exception) {
                // Ignore analytics errors
            }

            val isLoggedIn = sessionStore.isLoggedIn.first()

            if (!isLoggedIn) {
                _destination.value = SplashDestination.Login
                return@launch
            }

            // Ensure device id exists
            sessionStore.ensureDeviceId()

            val userId = sessionStore.userId.first()
            if (userId == null) {
                sessionStore.setLoggedIn(false)
                _destination.value = SplashDestination.Login
                return@launch
            }

            // Validate session with server
            try {
                val sessionResponse = startupRepository.checkSession(userId)
                if (!sessionResponse.isSuccessful) {
                    val errorBody = sessionResponse.errorBody()?.string() ?: ""
                    val isAuthFailure = errorBody.contains("SESSION_REVOKED", ignoreCase = true) ||
                            errorBody.contains("UNAUTHORIZED", ignoreCase = true) ||
                            errorBody.contains("SESSION_NOT_INITIALIZED", ignoreCase = true)

                    if (isAuthFailure) {
                        sessionStore.clearSession()
                        _destination.value = SplashDestination.Login
                        return@launch
                    }
                    // else: proceed (offline / server down)
                } else {
                    val body = sessionResponse.body()
                    if (body?.success != true) {
                        val errorCode = body?.errorCode ?: ""
                        val isAuthFailure = errorCode.contains("SESSION_REVOKED", ignoreCase = true) ||
                                errorCode.contains("UNAUTHORIZED", ignoreCase = true) ||
                                errorCode.contains("SESSION_NOT_INITIALIZED", ignoreCase = true)

                        if (isAuthFailure) {
                            sessionStore.clearSession()
                            _destination.value = SplashDestination.Login
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                // Network error - proceed with cached data
            }

            // Check if exam is selected
            val selectedExam = sessionStore.selectedExam.first()
            val selectedExamId = sessionStore.selectedExamId.first()

            if (selectedExam == null || selectedExamId == null) {
                _destination.value = SplashDestination.ExamSelection
                return@launch
            }

            // Check premium status
            var isPremium = sessionStore.isPremium.first()

            // Try to refresh premium status from server
            try {
                val statusResponse = startupRepository.subscriptionStatus(userId)
                if (statusResponse.isSuccessful) {
                    val status = statusResponse.body()
                    isPremium = status?.isPremium ?: false
                    sessionStore.setPremium(isPremium)

                    // Set user ID for analytics
                    firebaseService.setUserId(userId.toString())
                }
            } catch (e: Exception) {
                // Use cached value
            }

            if (!isPremium) {
                _destination.value = SplashDestination.SubscriptionOffer
            } else {
                _destination.value = SplashDestination.Home(selectedExam)
            }
        }
    }
}
