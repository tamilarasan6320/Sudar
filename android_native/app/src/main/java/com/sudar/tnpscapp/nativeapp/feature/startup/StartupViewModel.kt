package com.sudar.tnpscapp.nativeapp.feature.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudar.tnpscapp.nativeapp.core.repo.StartupRepository
import com.sudar.tnpscapp.nativeapp.core.services.PremiumVideoService
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import com.sudar.tnpscapp.nativeapp.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val startupRepository: StartupRepository,
    private val premiumVideoService: PremiumVideoService,
) : ViewModel() {
    private val _destination = MutableStateFlow<String?>(null)
    val destination: StateFlow<String?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            // Start premium video sync in background (non-blocking)
            launch { premiumVideoService.sync() }
            
            _destination.value = decideNextRoute()
        }
    }

    private suspend fun decideNextRoute(): String {
        val loggedIn = sessionStore.isLoggedIn.first()
        if (!loggedIn) return Routes.Login

        val userId = sessionStore.userId.first()
        if (userId == null) {
            sessionStore.clearSession()
            return Routes.Login
        }

        // Best-effort session validation (same intent as Flutter AppStartupRouter)
        try {
            val res = startupRepository.checkSession(userId)
            if (!res.isSuccessful && res.code() == 401) {
                sessionStore.clearSession()
                return Routes.Login
            }
        } catch (_: Exception) {
            // Ignore network errors: continue with cached state (Flutter does best-effort too)
        }

        val selectedExam = sessionStore.selectedExam.first()
        val selectedExamId = sessionStore.selectedExamId.first()
        if (selectedExam.isNullOrBlank() || selectedExamId == null) {
            return Routes.ExamSelection
        }

        var isPremium = sessionStore.isPremium.first()

        // Best-effort refresh premium status from server
        if (!isPremium) {
            try {
                val res = startupRepository.subscriptionStatus(userId)
                if (res.isSuccessful) {
                    val serverPremium = res.body()?.isPremium == true
                    isPremium = serverPremium
                    sessionStore.setPremium(serverPremium)
                }
            } catch (_: Exception) {
                // ignore; keep cached
            }
        }

        return if (!isPremium) Routes.subscriptionOffer("startup") else Routes.Main
    }
}

