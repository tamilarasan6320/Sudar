package com.sudar.tnpscapp.nativeapp.core.services

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.sudar.tnpscapp.nativeapp.core.network.SudarApi
import com.sudar.tnpscapp.nativeapp.core.session.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Captures Google Play Install Referrer on FIRST OPEN after a fresh install,
 * and sends it to backend: /api/referrals/track_install.php
 *
 * Link flow:
 * - Admin generates https://sudartnpscapp.in/r/CODE
 * - /r/index.php redirects to Play Store with ?referrer=ref=CODE
 * - Play Store provides installReferrer like: "ref=CODE" (sometimes encoded)
 */
@Singleton
class ReferralInstallService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
    private val api: SudarApi,
) {
    companion object {
        private const val TAG = "ReferralInstall"
        private val CODE_PATTERN = Regex("^[A-Za-z0-9_-]{2,64}$")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun captureIfNeeded() {
        scope.launch {
            try {
                // Only run on fresh installs (avoid counting updates as installs)
                if (!isFreshInstall()) {
                    sessionStore.setReferralInstallSent(true)
                    return@launch
                }

                // Retry pending first
                val pending = sessionStore.getPendingReferralCode()
                if (!pending.isNullOrBlank()) {
                    val ok = trySendToBackend(code = pending, installReferrer = null)
                    if (ok) {
                        sessionStore.clearPendingReferralCode()
                        sessionStore.setReferralInstallSent(true)
                    }
                    return@launch
                }

                // If already done, stop
                if (sessionStore.getReferralInstallSent()) return@launch

                val client = InstallReferrerClient.newBuilder(context).build()
                client.startConnection(object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        when (responseCode) {
                            InstallReferrerClient.InstallReferrerResponse.OK -> {
                                scope.launch {
                                    var shouldMarkDone = false
                                    try {
                                        val raw = client.installReferrer?.installReferrer.orEmpty()
                                        val decoded = decodePossiblyEncodedQuery(raw)
                                        val code = extractCode(decoded)

                                        if (!code.isNullOrBlank()) {
                                            // Save pending for retry safety
                                            sessionStore.setPendingReferralCode(code)

                                            val ok = trySendToBackend(code = code, installReferrer = decoded)
                                            if (ok) {
                                                Log.d(TAG, "✅ install tracked for code=$code")
                                                try { sessionStore.clearPendingReferralCode() } catch (_: Exception) {}
                                                shouldMarkDone = true
                                            } else {
                                                Log.w(TAG, "❌ install track failed; will retry next open (code=$code)")
                                                shouldMarkDone = false
                                            }
                                        } else {
                                            // No ref found, mark done to avoid repeated checks
                                            Log.d(TAG, "No ref code in install referrer: $decoded")
                                            shouldMarkDone = true
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to capture install referrer: ${e.message}", e)
                                    } finally {
                                        if (shouldMarkDone) {
                                            try { sessionStore.setReferralInstallSent(true) } catch (_: Exception) {}
                                        }
                                        try { client.endConnection() } catch (_: Exception) {}
                                    }
                                }
                            }

                            InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                                scope.launch { sessionStore.setReferralInstallSent(true) }
                                try { client.endConnection() } catch (_: Exception) {}
                            }

                            InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                                // Don't mark as sent; retry next open
                                try { client.endConnection() } catch (_: Exception) {}
                            }

                            else -> {
                                // Unknown code; avoid looping forever
                                scope.launch { sessionStore.setReferralInstallSent(true) }
                                try { client.endConnection() } catch (_: Exception) {}
                            }
                        }
                    }

                    override fun onInstallReferrerServiceDisconnected() {
                        // no-op
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "captureIfNeeded failed: ${e.message}", e)
            }
        }
    }

    private fun isFreshInstall(): Boolean {
        return try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            // Fresh install: firstInstallTime == lastUpdateTime (or extremely close)
            val diff = kotlin.math.abs(pi.firstInstallTime - pi.lastUpdateTime)
            diff < 5_000L
        } catch (_: PackageManager.NameNotFoundException) {
            true
        } catch (_: Exception) {
            true
        }
    }

    private fun decodePossiblyEncodedQuery(raw: String): String {
        if (raw.isBlank()) return raw
        val first = try {
            URLDecoder.decode(raw, "UTF-8")
        } catch (_: Exception) {
            raw
        }
        // Some sources deliver a double-encoded string
        return if (first.contains("%3D", ignoreCase = true) || first.contains("%26", ignoreCase = true)) {
            try {
                URLDecoder.decode(first, "UTF-8")
            } catch (_: Exception) {
                first
            }
        } else {
            first
        }
    }

    private fun extractParam(decodedQuery: String, key: String): String? {
        if (decodedQuery.isBlank()) return null
        val parts = decodedQuery.split("&")
        for (p in parts) {
            val kv = p.split("=", limit = 2)
            if (kv.size == 2 && kv[0].trim().equals(key, ignoreCase = true)) {
                return kv[1].trim().takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun extractCode(decodedQuery: String): String? {
        val c = extractParam(decodedQuery, "ref") ?: return null
        return if (CODE_PATTERN.matches(c)) c else null
    }

    private suspend fun trySendToBackend(code: String, installReferrer: String?): Boolean {
        return try {
            val deviceId = sessionStore.ensureDeviceId()
            val payload = mutableMapOf<String, Any?>(
                "device_id" to deviceId,
                "code" to code,
            )
            if (!installReferrer.isNullOrBlank()) payload["install_referrer"] = installReferrer

            val res = api.trackReferralInstall(payload)
            if (!res.isSuccessful) return false
            val body = res.body()
            (body?.get("success") as? Boolean) == true
        } catch (e: Exception) {
            Log.e(TAG, "track_install request failed: ${e.message}", e)
            false
        }
    }
}

