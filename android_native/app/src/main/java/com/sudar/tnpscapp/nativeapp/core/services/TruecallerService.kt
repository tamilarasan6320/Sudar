package com.sudar.tnpscapp.nativeapp.core.services

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentActivity
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result from Truecaller profile request
 */
sealed class TruecallerResult {
    data class Success(val accessToken: String) : TruecallerResult()
    object Cancelled : TruecallerResult()
    object NotUsable : TruecallerResult()
    data class Error(val message: String) : TruecallerResult()
}

/**
 * Service to interact with Truecaller SDK
 * Handles OAuth flow for phone number verification
 * 
 * Note: Truecaller SDK requires API 24 (Android 7.0) or higher.
 * For devices below API 24, Truecaller will not be available.
 * Reference: https://docs.truecaller.com/truecaller-sdk/android/oauth-sdk-3.0.0/integration-steps/setup
 */
@Singleton
class TruecallerService @Inject constructor() {
    
    companion object {
        private const val TAG = "TruecallerService"
        private const val CLIENT_ID = "0l45fkucpsmpmovcm8muhs8z7uypq-ysfzrhws0slki"
        private const val TOKEN_URL = "https://oauth-account-noneu.truecaller.com/v1/token"
        // Minimum API level required by Truecaller SDK
        private const val MIN_API_LEVEL = Build.VERSION_CODES.N // API 24
    }

    private var activityRef: WeakReference<FragmentActivity>? = null
    private var codeVerifier: String? = null
    private var oAuthState: String? = null
    // Simple callback for result - no coroutines needed
    private var onResultCallback: ((TruecallerResult) -> Unit)? = null
    // OkHttp client with timeout settings
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private var authLauncher: ActivityResultLauncher<Intent>? = null
    private var isInitialized = false

    /**
     * Check if device meets minimum API level for Truecaller SDK
     */
    private fun isSdkSupported(): Boolean {
        return Build.VERSION.SDK_INT >= MIN_API_LEVEL
    }

    /**
     * Initialize Truecaller SDK
     * Must be called from FragmentActivity (ComponentActivity)
     * 
     * SDK will only initialize on API 24+ devices
     */
    fun initialize(activity: FragmentActivity, launcher: ActivityResultLauncher<Intent>) {
        Log.d(TAG, "=== TRUECALLER INIT START ===")
        Log.d(TAG, "Activity: ${activity.javaClass.simpleName}")
        Log.d(TAG, "API Level: ${Build.VERSION.SDK_INT}")
        
        // Check API level before accessing SDK
        if (!isSdkSupported()) {
            Log.w(TAG, "Truecaller SDK not supported on API ${Build.VERSION.SDK_INT} (requires API $MIN_API_LEVEL+)")
            return
        }
        
        if (isInitialized) {
            Log.d(TAG, "SDK already initialized, updating activity reference")
            // Update activity reference even if already initialized (in case activity was recreated)
            activityRef = WeakReference(activity)
            authLauncher = launcher
            return
        }
        
        activityRef = WeakReference(activity)
        authLauncher = launcher
        
        try {
            Log.d(TAG, "Creating TcSdkOptions...")
            // Import SDK classes only when we know API level is sufficient
            val tcOAuthCallback = createOAuthCallback()
            
            val options = com.truecaller.android.sdk.oAuth.TcSdkOptions.Builder(activity, tcOAuthCallback)
                .consentHeadingOption(com.truecaller.android.sdk.oAuth.TcSdkOptions.SDK_CONSENT_HEADING_LOG_IN_TO)
                .footerType(com.truecaller.android.sdk.oAuth.TcSdkOptions.FOOTER_TYPE_ANOTHER_MOBILE_NO)
                .ctaText(com.truecaller.android.sdk.oAuth.TcSdkOptions.CTA_TEXT_PROCEED)
                .buttonShapeOptions(com.truecaller.android.sdk.oAuth.TcSdkOptions.BUTTON_SHAPE_ROUNDED)
                .build()

            Log.d(TAG, "Calling TcSdk.init()...")
            com.truecaller.android.sdk.oAuth.TcSdk.init(options)
            isInitialized = true
            Log.d(TAG, "=== TRUECALLER SDK INITIALIZED SUCCESSFULLY ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SDK: ${e.message}", e)
        }
    }
    
    /**
     * Create OAuth callback for Truecaller SDK
     */
    private fun createOAuthCallback(): com.truecaller.android.sdk.oAuth.TcOAuthCallback {
        return object : com.truecaller.android.sdk.oAuth.TcOAuthCallback {
            override fun onSuccess(tcOAuthData: com.truecaller.android.sdk.oAuth.TcOAuthData) {
                Log.d(TAG, "=== TRUECALLER OAUTH SUCCESS ===")
                Log.d(TAG, "AuthCode length: ${tcOAuthData.authorizationCode.length}")
                Log.d(TAG, "Starting token exchange...")
                
                // Exchange authorization code for access token in background
                Thread {
                    try {
                        val startTime = System.currentTimeMillis()
                        val result = exchangeCodeForToken(tcOAuthData.authorizationCode)
                        val duration = System.currentTimeMillis() - startTime
                        Log.d(TAG, "Token exchange completed in ${duration}ms")
                        
                        // Send result through channel (thread-safe)
                        sendResult(result)
                    } catch (e: Exception) {
                        Log.e(TAG, "Token exchange thread error: ${e.message}", e)
                        sendResult(TruecallerResult.Error("Token exchange failed: ${e.message}"))
                    }
                }.start()
            }

            override fun onFailure(tcOAuthError: com.truecaller.android.sdk.oAuth.TcOAuthError) {
                Log.e(TAG, "=== TRUECALLER OAUTH FAILURE ===")
                Log.e(TAG, "Error code: ${tcOAuthError.errorCode}")
                Log.e(TAG, "Error message: ${tcOAuthError.errorMessage}")
                sendResult(TruecallerResult.Cancelled)
            }

            override fun onVerificationRequired(tcOAuthError: com.truecaller.android.sdk.oAuth.TcOAuthError?) {
                Log.d(TAG, "=== TRUECALLER VERIFICATION REQUIRED ===")
                Log.d(TAG, "User needs manual verification (non-Truecaller user)")
                sendResult(TruecallerResult.Cancelled)
            }
        }
    }
    
    /**
     * Send result through callback safely on main thread
     */
    private fun sendResult(result: TruecallerResult) {
        Log.d(TAG, "Sending result via callback: ${result::class.simpleName}")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                Log.d(TAG, "Invoking callback on main thread")
                onResultCallback?.invoke(result)
                Log.d(TAG, "Callback invoked successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking callback: ${e.message}", e)
            }
        }
    }

    /**
     * Check if Truecaller OAuth flow is usable on this device
     * Returns false if API level < 24 or Truecaller app not installed
     */
    fun isUsable(): Boolean {
        Log.d(TAG, "=== CHECKING TRUECALLER USABILITY ===")
        
        // Check API level first
        if (!isSdkSupported()) {
            Log.d(TAG, "Truecaller not usable: API ${Build.VERSION.SDK_INT} < $MIN_API_LEVEL")
            return false
        }
        
        return try {
            val sdk = com.truecaller.android.sdk.oAuth.TcSdk.getInstance()
            Log.d(TAG, "SDK instance: ${if (sdk != null) "exists" else "NULL"}")
            
            val usable = sdk?.isOAuthFlowUsable ?: false
            Log.d(TAG, "isOAuthFlowUsable = $usable")
            usable
        } catch (e: Exception) {
            Log.e(TAG, "Error checking isUsable: ${e.message}", e)
            false
        }
    }

    /**
     * Request user profile from Truecaller (callback-based, no suspend)
     * @param onResult callback that will be called with the result
     */
    fun requestProfile(onResult: (TruecallerResult) -> Unit) {
        Log.d(TAG, "=== REQUEST PROFILE START ===")
        
        // Check API level first
        if (!isSdkSupported()) {
            Log.w(TAG, "SDK not supported on this API level")
            onResult(TruecallerResult.NotUsable)
            return
        }
        
        val activity = activityRef?.get()
        if (activity == null) {
            Log.e(TAG, "Activity reference is null")
            onResult(TruecallerResult.Error("Activity not available"))
            return
        }

        if (!isUsable()) {
            Log.w(TAG, "Truecaller not usable")
            onResult(TruecallerResult.NotUsable)
            return
        }

        val launcher = authLauncher
        if (launcher == null) {
            Log.e(TAG, "Auth launcher is null")
            onResult(TruecallerResult.Error("Auth launcher not initialized"))
            return
        }

        try {
            // Store callback for later
            onResultCallback = onResult
            
            // Generate OAuth state
            oAuthState = System.currentTimeMillis().toString()
            Log.d(TAG, "OAuth state: $oAuthState")
            
            // Generate code verifier and challenge for PKCE
            codeVerifier = com.truecaller.android.sdk.oAuth.CodeVerifierUtil.generateRandomCodeVerifier()
            val codeChallenge = codeVerifier?.let { 
                com.truecaller.android.sdk.oAuth.CodeVerifierUtil.getCodeChallenge(it) 
            }

            if (codeChallenge == null) {
                Log.e(TAG, "Failed to generate code challenge")
                onResult(TruecallerResult.Error("Failed to generate code challenge"))
                return
            }
            
            Log.d(TAG, "Code challenge generated, length: ${codeChallenge.length}")

            // Request authorization
            val sdk = com.truecaller.android.sdk.oAuth.TcSdk.getInstance()
            sdk?.setOAuthState(oAuthState!!)
            sdk?.setOAuthScopes(arrayOf("phone"))
            sdk?.setCodeChallenge(codeChallenge)
            
            Log.d(TAG, "Calling getAuthorizationCode...")
            sdk?.getAuthorizationCode(activity, launcher)
            Log.d(TAG, "getAuthorizationCode called, waiting for callback...")

        } catch (e: Exception) {
            Log.e(TAG, "Error requesting profile: ${e.message}", e)
            onResult(TruecallerResult.Error(e.message ?: "Unknown error"))
        }
    }

    /**
     * Exchange authorization code for access token using PKCE
     */
    private fun exchangeCodeForToken(authCode: String): TruecallerResult {
        Log.d(TAG, "=== TOKEN EXCHANGE START ===")
        
        val verifier = codeVerifier
        if (verifier == null) {
            Log.e(TAG, "Code verifier is NULL!")
            return TruecallerResult.Error("Code verifier is null")
        }
        
        Log.d(TAG, "Code verifier length: ${verifier.length}")
        Log.d(TAG, "Auth code length: ${authCode.length}")

        try {
            val formBody = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", CLIENT_ID)
                .add("code", authCode)
                .add("code_verifier", verifier)
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            Log.d(TAG, "Sending token request to: $TOKEN_URL")
            val startTime = System.currentTimeMillis()
            
            val response = httpClient.newCall(request).execute()
            val networkTime = System.currentTimeMillis() - startTime
            
            val responseBody = response.body?.string()

            Log.d(TAG, "Token response received in ${networkTime}ms")
            Log.d(TAG, "Token response status: ${response.code}")

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val accessToken = json.optString("access_token", null)
                
                if (accessToken != null) {
                    Log.d(TAG, "=== TOKEN EXCHANGE SUCCESS ===")
                    Log.d(TAG, "Access token length: ${accessToken.length}")
                    return TruecallerResult.Success(accessToken)
                } else {
                    Log.e(TAG, "No access_token in response: $responseBody")
                }
            } else {
                Log.e(TAG, "Token exchange HTTP error: ${response.code}")
                Log.e(TAG, "Response body: $responseBody")
            }

            return TruecallerResult.Error("Failed to exchange authorization code (HTTP ${response.code})")

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Token exchange TIMEOUT: ${e.message}")
            return TruecallerResult.Error("Connection timeout - please try again")
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Token exchange IO error: ${e.message}")
            return TruecallerResult.Error("Network error - please check internet connection")
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange error: ${e.message}", e)
            return TruecallerResult.Error(e.message ?: "Token exchange error")
        }
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        activityRef?.clear()
        activityRef = null
        onResultCallback = null
    }
}
