package com.sudar.tnpscapp.nativeapp

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.razorpay.Checkout
import com.sudar.tnpscapp.nativeapp.core.services.FirebaseService
import com.sudar.tnpscapp.nativeapp.core.services.MetaAppEventsService
import com.sudar.tnpscapp.nativeapp.core.services.OneSignalService
import com.sudar.tnpscapp.nativeapp.core.services.ReferralInstallService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SudarNativeApplication : Application() {

    @Inject
    lateinit var oneSignalService: OneSignalService

    @Inject
    lateinit var firebaseService: FirebaseService

    @Inject
    lateinit var metaAppEventsService: MetaAppEventsService

    @Inject
    lateinit var referralInstallService: ReferralInstallService

    override fun onCreate() {
        super.onCreate()
        
        // Force dark mode globally - ignore system light mode setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // Preload Razorpay checkout resources for faster loading
        try {
            Checkout.preload(applicationContext)
        } catch (e: Exception) {
            Log.e("SudarApp", "Razorpay preload failed: ${e.message}")
        }

        // Initialize services with error handling to prevent crashes
        try {
            // Initialize Firebase first (more stable)
            firebaseService.initialize()
        } catch (e: Exception) {
            Log.e("SudarApp", "Firebase init failed: ${e.message}")
        }

        try {
            // Initialize OneSignal
            oneSignalService.initialize()
        } catch (e: Exception) {
            Log.e("SudarApp", "OneSignal init failed: ${e.message}")
        }

        try {
            // Initialize Meta (Facebook) App Events for attribution
            metaAppEventsService.initialize()
        } catch (e: Exception) {
            Log.e("SudarApp", "Meta App Events init failed: ${e.message}")
        }

        try {
            // Capture install referrer (fresh installs only) for /r/CODE attribution
            referralInstallService.captureIfNeeded()
        } catch (e: Exception) {
            Log.e("SudarApp", "Referral install capture failed: ${e.message}")
        }

    }
}
