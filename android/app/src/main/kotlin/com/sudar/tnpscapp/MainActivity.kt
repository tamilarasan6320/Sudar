package com.sudar.tnpscapp

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant

class MainActivity : FlutterFragmentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val CHANNEL = "com.sudar.tnpscapp/phone_hint"
        private const val SMS_USER_CONSENT_CHANNEL = "com.sudar.tnpscapp/sms_user_consent"
    }
    
    private var pendingResult: MethodChannel.Result? = null
    private var smsUserConsentChannel: MethodChannel? = null
    private var smsConsentReceiver: BroadcastReceiver? = null
    private var isSmsConsentReceiverRegistered: Boolean = false
    
    // Must be registered before STARTED state - using lazy init with registerForActivityResult
    private val phoneNumberHintLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            handlePhoneNumberHintResult(activityResult.resultCode, activityResult.data)
        }

    // SMS User Consent launcher (shows system consent dialog)
    private val smsConsentLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            handleSmsUserConsentResult(activityResult.resultCode, activityResult.data)
        }
    
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        
        // Set up MethodChannel for phone number hint
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "requestPhoneNumberHint" -> {
                    requestPhoneNumberHint(result)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        // Set up MethodChannel for SMS User Consent (OTP autofill without changing SMS format)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, SMS_USER_CONSENT_CHANNEL).also { channel ->
            smsUserConsentChannel = channel
            channel.setMethodCallHandler { call, result ->
                when (call.method) {
                    "start" -> startSmsUserConsent(result)
                    "stop" -> stopSmsUserConsent(result)
                    else -> result.notImplemented()
                }
            }
        }
    }
    
    private fun requestPhoneNumberHint(result: MethodChannel.Result) {
        pendingResult = result
        
        try {
            val request = GetPhoneNumberHintIntentRequest.builder().build()
            
            Identity.getSignInClient(this)
                .getPhoneNumberHintIntent(request)
                .addOnSuccessListener { pendingIntent ->
                    try {
                        val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        phoneNumberHintLauncher.launch(intentSenderRequest)
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(TAG, "Failed to launch phone hint intent", e)
                        pendingResult?.error("LAUNCH_FAILED", "Failed to launch phone hint: ${e.message}", null)
                        pendingResult = null
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Phone number hint failed", e)
                    pendingResult?.error("HINT_FAILED", "Phone number hint failed: ${e.message}", null)
                    pendingResult = null
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting phone hint", e)
            result.error("ERROR", "Error requesting phone hint: ${e.message}", null)
            pendingResult = null
        }
    }
    
    private fun handlePhoneNumberHintResult(resultCode: Int, data: android.content.Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            try {
                val phoneNumber = Identity.getSignInClient(this).getPhoneNumberFromIntent(data)
                Log.d(TAG, "Phone number selected: $phoneNumber")
                pendingResult?.success(phoneNumber)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get phone number from intent", e)
                pendingResult?.error("PARSE_FAILED", "Failed to get phone number: ${e.message}", null)
            }
        } else {
            Log.d(TAG, "Phone hint cancelled or no result")
            pendingResult?.success(null) // User cancelled - return null, not an error
        }
        pendingResult = null
    }

    private fun startSmsUserConsent(result: MethodChannel.Result) {
        try {
            registerSmsConsentReceiverIfNeeded()
            SmsRetriever.getClient(this)
                // null = accept SMS from any sender (useful when you can't control sender ID/template)
                .startSmsUserConsent(null)
                .addOnSuccessListener {
                    Log.d(TAG, "SMS User Consent started")
                    result.success(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "SMS User Consent start failed", e)
                    result.error("START_FAILED", "Failed to start SMS User Consent: ${e.message}", null)
                }
        } catch (e: Exception) {
            Log.e(TAG, "SMS User Consent error", e)
            result.error("ERROR", "SMS User Consent error: ${e.message}", null)
        }
    }

    private fun stopSmsUserConsent(result: MethodChannel.Result) {
        try {
            unregisterSmsConsentReceiverIfNeeded()
            result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Stop SMS User Consent error", e)
            result.error("ERROR", "Failed to stop SMS User Consent: ${e.message}", null)
        }
    }

    private fun registerSmsConsentReceiverIfNeeded() {
        if (isSmsConsentReceiverRegistered) return

        smsConsentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION != intent.action) return

                val extras = intent.extras ?: return
                val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return

                when (status.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        val consentIntent = getParcelableCompat(extras, SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
                        if (consentIntent != null) {
                            smsConsentLauncher.launch(consentIntent)
                        } else {
                            emitToFlutter("onSmsConsentError", "Consent intent is null")
                        }
                    }
                    CommonStatusCodes.TIMEOUT -> {
                        emitToFlutter("onSmsConsentTimeout", null)
                    }
                    else -> {
                        emitToFlutter("onSmsConsentError", "Status: ${status.statusCode}")
                    }
                }
            }
        }

        val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        ContextCompat.registerReceiver(this, smsConsentReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        isSmsConsentReceiverRegistered = true
        Log.d(TAG, "SMS User Consent receiver registered")
    }

    private fun unregisterSmsConsentReceiverIfNeeded() {
        if (!isSmsConsentReceiverRegistered) return
        try {
            unregisterReceiver(smsConsentReceiver)
        } catch (e: Exception) {
            // ignore unregister issues
        } finally {
            smsConsentReceiver = null
            isSmsConsentReceiverRegistered = false
            Log.d(TAG, "SMS User Consent receiver unregistered")
        }
    }

    private fun handleSmsUserConsentResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            if (!message.isNullOrEmpty()) {
                emitToFlutter("onSmsReceived", message)
            } else {
                emitToFlutter("onSmsConsentError", "SMS message is empty")
            }
        } else {
            // User cancelled the dialog or no data
            emitToFlutter("onSmsConsentCancelled", null)
        }
    }

    private fun emitToFlutter(method: String, args: Any?) {
        runOnUiThread {
            smsUserConsentChannel?.invokeMethod(method, args)
        }
    }

    @Suppress("DEPRECATION")
    private fun <T> getParcelableCompat(bundle: Bundle, key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, clazz)
        } else {
            bundle.getParcelable(key) as? T
        }
    }
}
