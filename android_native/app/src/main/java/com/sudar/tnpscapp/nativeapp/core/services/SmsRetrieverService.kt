package com.sudar.tnpscapp.nativeapp.core.services

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * SMS Retriever Service
 * Handles automatic OTP retrieval using Google's SMS Retriever API
 * Also provides Phone Number Hint API for auto-fill
 */
@Singleton
class SmsRetrieverService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SmsRetrieverService"
        // OTP pattern - 6 digits
        private val OTP_PATTERN = Pattern.compile("(\\d{6})")
    }

    private var activityRef: WeakReference<ComponentActivity>? = null
    private var smsReceiver: BroadcastReceiver? = null
    private var phoneHintLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    // Callbacks
    var onOtpReceived: ((String) -> Unit)? = null
    var onPhoneNumberReceived: ((String) -> Unit)? = null

    /**
     * Initialize with activity (for phone hint)
     */
    fun initialize(activity: ComponentActivity) {
        activityRef = WeakReference(activity)

        // Register phone hint launcher
        phoneHintLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val credential = Identity.getSignInClient(activity)
                        .getPhoneNumberFromIntent(result.data)
                    Log.d(TAG, "Phone number hint received: $credential")
                    onPhoneNumberReceived?.invoke(credential)
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting phone number from intent: ${e.message}")
                }
            }
        }
    }

    /**
     * Start SMS Retriever to listen for OTP
     * Must be called after sending OTP request to server
     */
    fun startSmsRetriever() {
        try {
            val client = SmsRetriever.getClient(context)
            val task = client.startSmsRetriever()

            task.addOnSuccessListener {
                Log.d(TAG, "SMS Retriever started")
                registerSmsReceiver()
            }

            task.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start SMS Retriever: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting SMS Retriever: ${e.message}")
        }
    }

    private fun registerSmsReceiver() {
        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
                    val extras = intent.extras
                    val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

                    when (status?.statusCode) {
                        CommonStatusCodes.SUCCESS -> {
                            val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                            Log.d(TAG, "SMS received: $message")
                            message?.let { extractOtp(it) }
                        }
                        CommonStatusCodes.TIMEOUT -> {
                            Log.d(TAG, "SMS Retriever timeout")
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        context.registerReceiver(smsReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null)
        Log.d(TAG, "SMS receiver registered")
    }

    private fun extractOtp(message: String) {
        val matcher = OTP_PATTERN.matcher(message)
        if (matcher.find()) {
            val otp = matcher.group(1)
            Log.d(TAG, "OTP extracted: $otp")
            otp?.let { onOtpReceived?.invoke(it) }
        }
    }

    /**
     * Request phone number hint (One Tap)
     * Shows system UI for user to select their phone number
     */
    fun requestPhoneNumberHint() {
        val activity = activityRef?.get() ?: return
        val launcher = phoneHintLauncher ?: return

        try {
            val request = GetPhoneNumberHintIntentRequest.builder().build()

            Identity.getSignInClient(activity)
                .getPhoneNumberHintIntent(request)
                .addOnSuccessListener { pendingIntent ->
                    try {
                        launcher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error launching phone hint: ${e.message}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get phone number hint: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting phone number hint: ${e.message}")
        }
    }

    /**
     * Stop listening for SMS
     */
    fun stopSmsRetriever() {
        try {
            smsReceiver?.let {
                context.unregisterReceiver(it)
                smsReceiver = null
                Log.d(TAG, "SMS receiver unregistered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering SMS receiver: ${e.message}")
        }
    }

    /**
     * Get app hash for SMS Retriever
     * This should be used in the OTP SMS message from server
     */
    fun getAppSignatureHash(): String {
        // Note: In production, this should be computed using AppSignatureHelper
        // For now, return empty string - the server should have this pre-configured
        return ""
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        stopSmsRetriever()
        activityRef?.clear()
        activityRef = null
        onOtpReceived = null
        onPhoneNumberReceived = null
    }
}
