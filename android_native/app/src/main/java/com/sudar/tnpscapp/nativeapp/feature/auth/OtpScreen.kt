package com.sudar.tnpscapp.nativeapp.feature.auth

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors
import kotlinx.coroutines.delay

/**
 * OTP Verification Screen - Exact Flutter replica
 * Features:
 * - 6-digit OTP input boxes
 * - Auto-fill support
 * - Resend timer (60 seconds)
 * - Remaining attempts warning
 * - Support button
 * - Terms and Privacy links
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    phoneNumber: String,
    onBack: () -> Unit,
    onVerified: (isNewUser: Boolean, mobile: String, token: String?) -> Unit,
    onExistingUser: () -> Unit,
    viewModel: OtpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // SMS User Consent launcher - one-tap to allow reading SMS
    val smsConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            android.util.Log.d("OtpScreen", "SMS received: $message")
            message?.let {
                // Extract 6-digit OTP: "Your OTP is 247973..."
                val otpRegex = Regex("\\b(\\d{6})\\b")
                val matchResult = otpRegex.find(it)
                matchResult?.groupValues?.get(1)?.let { otp ->
                    android.util.Log.d("OtpScreen", "OTP extracted: $otp")
                    viewModel.updateOtp(otp)
                    viewModel.autoVerifyAfterSmsRead()
                }
            }
        }
    }

    // Start SMS User Consent API (one-tap dialog, no hash needed!)
    LaunchedEffect(Unit) {
        try {
            val client = SmsRetriever.getClient(context)
            client.startSmsUserConsent(null) // null = accept from any sender
                .addOnSuccessListener {
                    android.util.Log.d("OtpScreen", "SMS User Consent started - waiting for OTP SMS")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("OtpScreen", "SMS User Consent failed: ${e.message}")
                }
        } catch (e: Exception) {
            android.util.Log.e("OtpScreen", "Error starting SMS consent: ${e.message}")
        }
    }

    // Register BroadcastReceiver for SMS User Consent
    DisposableEffect(Unit) {
        val smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
                    val extras = intent.extras
                    val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

                    when (status?.statusCode) {
                        CommonStatusCodes.SUCCESS -> {
                            // Get consent intent and launch one-tap dialog
                            val consentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                            consentIntent?.let {
                                try {
                                    smsConsentLauncher.launch(it)
                                } catch (e: Exception) {
                                    android.util.Log.e("OtpScreen", "Error launching consent: ${e.message}")
                                }
                            }
                        }
                        CommonStatusCodes.TIMEOUT -> {
                            android.util.Log.d("OtpScreen", "SMS consent timeout (5 minutes)")
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        ContextCompat.registerReceiver(
            context,
            smsReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        onDispose {
            try {
                context.unregisterReceiver(smsReceiver)
            } catch (e: Exception) {
                // Receiver may not be registered
            }
        }
    }

    // Send OTP when screen opens
    LaunchedEffect(phoneNumber) {
        viewModel.setPhoneNumber(phoneNumber)
        viewModel.sendInitialOtp()
    }

    // Handle verification success
    LaunchedEffect(uiState.verificationResult) {
        uiState.verificationResult?.let { result ->
            if (result.isNewUser) {
                onVerified(true, result.mobile, result.token)
            } else {
                onExistingUser()
            }
            viewModel.clearVerificationResult()
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sign Up",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.openWhatsAppSupport() }) {
                        Text(
                            text = "Support",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        }
    ) { paddingValues ->
        // Use Column without scroll to avoid BringIntoViewRequester crash
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "OTP Verification",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle with phone number
            Text(
                buildAnnotatedString {
                    append("Enter the code from the sms we sent to\n")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)) {
                        append("+91 $phoneNumber")
                    }
                },
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // OTP Input Fields - Using simple approach without hidden BasicTextField
            OtpInputRow(
                otp = uiState.otp,
                onOtpChange = { viewModel.updateOtp(it) },
                onComplete = { 
                    keyboardController?.hide()
                    viewModel.verifyOtp() 
                },
                isError = uiState.error != null
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Resend Section
            Row(
                modifier = Modifier.clickable(
                    enabled = uiState.canResend && !uiState.isResending,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    viewModel.resendOtp()
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Didn't receive the OTP? ",
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = when {
                        uiState.isResending -> "Sending..."
                        uiState.canResend -> "Resend"
                        else -> "Resend in ${uiState.resendTimer}s"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiState.canResend) AppColors.Primary else AppColors.TextSecondary
                )
            }

            // Error message
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.error!!,
                    fontSize = 13.sp,
                    color = AppColors.Error,
                    textAlign = TextAlign.Center
                )
            }

            // Remaining attempts warning
            if (uiState.remainingAttempts in 1..2) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Only ${uiState.remainingAttempts} attempts left!",
                    fontSize = 13.sp,
                    color = AppColors.Warning,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Verify Button
            val isComplete = uiState.otp.length == 6

            Button(
                onClick = { 
                    keyboardController?.hide()
                    viewModel.verifyOtp() 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isComplete) AppColors.Primary else Color(0xFFBDBDBD)
                ),
                enabled = !uiState.isLoading && isComplete
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "SUBMIT",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Terms and Privacy
            Text(
                buildAnnotatedString {
                    append("By continuing, you agree with our\n")
                    withStyle(SpanStyle(color = AppColors.Primary, fontWeight = FontWeight.Medium)) {
                        append("Terms of Use")
                    }
                    append(" and ")
                    withStyle(SpanStyle(color = AppColors.Primary, fontWeight = FontWeight.Medium)) {
                        append("Privacy Policy")
                    }
                },
                fontSize = 11.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }

    // Show snackbar for success/error messages
    uiState.snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            delay(3000)
            viewModel.clearSnackbar()
        }
    }
}

@Composable
private fun OtpInputRow(
    otp: String,
    onOtpChange: (String) -> Unit,
    onComplete: () -> Unit,
    isError: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var hasFocus by remember { mutableStateOf(false) }

    // Invisible text field positioned absolutely to avoid scroll issues
    Box(modifier = Modifier.fillMaxWidth()) {
        // The actual input field - not inside any scroll
        BasicTextField(
            value = otp,
            onValueChange = { value ->
                val filtered = value.filter { it.isDigit() }.take(6)
                onOtpChange(filtered)
                if (filtered.length == 6) {
                    focusManager.clearFocus()
                    onComplete()
                }
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .size(1.dp) // Minimal size, effectively hidden
                .offset(x = (-100).dp), // Move off screen
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (otp.length == 6) onComplete()
                }
            ),
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent)
        )

        // Visible OTP boxes - responsive sizing to fit all screens
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    try {
                        focusRequester.requestFocus()
                    } catch (e: Exception) {
                        // Ignore focus errors
                    }
                }
        ) {
            repeat(6) { index ->
                val char = otp.getOrNull(index)?.toString() ?: ""
                val hasValue = char.isNotEmpty()
                val isCurrentPosition = index == otp.length

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(min = 40.dp, max = 52.dp)
                        .aspectRatio(0.85f) // Width to height ratio
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.CardBackground)
                        .border(
                            width = if (isCurrentPosition && hasFocus) 2.dp else 1.dp,
                            color = when {
                                isError -> AppColors.Error
                                isCurrentPosition && hasFocus -> AppColors.Primary
                                hasValue -> AppColors.Primary
                                else -> AppColors.Border
                            },
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
            }
        }
    }

    // Request focus after a short delay
    LaunchedEffect(Unit) {
        delay(800) // Wait for screen to fully render
        try {
            focusRequester.requestFocus()
            hasFocus = true
        } catch (e: Exception) {
            // Ignore
        }
    }
}
