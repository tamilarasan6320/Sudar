package com.sudar.tnpscapp.nativeapp.feature.auth

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.sudar.tnpscapp.nativeapp.R
import com.sudar.tnpscapp.nativeapp.ui.components.LoopingAssetVideo
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors
import kotlinx.coroutines.delay

/**
 * Login Screen - Exact Flutter replica
 * Features:
 * - Logo with glow effect
 * - SUDAR title with gradient
 * - Mobile number input
 * - Continue with OTP button
 * - Truecaller auto-trigger (if available)
 * - Loading overlay
 * - Support button
 */
@Composable
fun LoginScreen(
    onNavigateToOtp: (String) -> Unit,
    onTruecallerSuccess: (isNewUser: Boolean, mobile: String, token: String?) -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var isLayoutReady by remember { mutableStateOf(false) }

    // Activity result launcher for Truecaller
    val truecallerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Pass the result back to Truecaller SDK
        android.util.Log.d("LoginScreen", "Truecaller activity result: ${result.resultCode}")
        try {
            val activity = context as? FragmentActivity
            if (activity != null && result.data != null) {
                android.util.Log.d("LoginScreen", "Passing result to TcSdk.onActivityResultObtained")
                com.truecaller.android.sdk.oAuth.TcSdk.getInstance()?.onActivityResultObtained(
                    activity,
                    result.resultCode,
                    result.data
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("LoginScreen", "Error handling Truecaller result: ${e.message}")
        }
    }

    // Activity result launcher for Phone Number Picker (Identity API)
    val phoneHintLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.clearPhoneHint()
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val phoneNumberHint = Identity.getSignInClient(context)
                    .getPhoneNumberFromIntent(result.data)
                android.util.Log.d("LoginScreen", "Phone number selected: $phoneNumberHint")
                // Extract last 10 digits (remove country code like +91)
                val cleanNumber = phoneNumberHint.replace(Regex("[^0-9]"), "")
                val last10 = if (cleanNumber.length >= 10) cleanNumber.takeLast(10) else cleanNumber
                phoneNumber = last10
                phoneError = null
            } catch (e: Exception) {
                android.util.Log.e("LoginScreen", "Error getting phone number: ${e.message}")
            }
        }
    }

    // Show phone picker when Truecaller is cancelled
    LaunchedEffect(uiState.showPhoneHint) {
        if (uiState.showPhoneHint) {
            try {
                val request = GetPhoneNumberHintIntentRequest.builder().build()
                val activity = context as? Activity
                if (activity != null) {
                    Identity.getSignInClient(activity)
                        .getPhoneNumberHintIntent(request)
                        .addOnSuccessListener { pendingIntent ->
                            try {
                                phoneHintLauncher.launch(
                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                )
                            } catch (e: IntentSender.SendIntentException) {
                                android.util.Log.e("LoginScreen", "Error launching phone picker: ${e.message}")
                                viewModel.clearPhoneHint()
                            }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("LoginScreen", "Phone picker not available: ${e.message}")
                            viewModel.clearPhoneHint()
                        }
                } else {
                    viewModel.clearPhoneHint()
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginScreen", "Error requesting phone picker: ${e.message}")
                viewModel.clearPhoneHint()
            }
        }
    }

    // Mark layout as ready after composition
    LaunchedEffect(Unit) {
        delay(500) // Wait for layout to be fully placed
        isLayoutReady = true
    }

    // Initialize Truecaller when layout is ready
    LaunchedEffect(isLayoutReady) {
        if (isLayoutReady) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                android.util.Log.d("LoginScreen", "Initializing Truecaller with FragmentActivity: ${activity.javaClass.simpleName}")
                viewModel.initTruecaller(activity, truecallerLauncher)
            } else {
                android.util.Log.e("LoginScreen", "Context is NOT a FragmentActivity! Type: ${context.javaClass.simpleName}")
            }
        }
    }

    // Handle Truecaller result
    LaunchedEffect(uiState.truecallerResult) {
        uiState.truecallerResult?.let { result ->
            if (result.isNewUser) {
                onTruecallerSuccess(true, result.mobile, result.token)
            } else {
                onLoginSuccess()
            }
            viewModel.clearTruecallerResult()
        }
    }

    // Show success message
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            delay(2000)
            viewModel.clearSuccessMessage()
        }
    }

    // Show error message
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            delay(3000)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Content
        Scaffold(
            containerColor = AppColors.Background,
            snackbarHost = {
                // Success snackbar
                if (uiState.successMessage != null) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = AppColors.Success,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.successMessage!!,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                // Error snackbar
                if (uiState.error != null) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = AppColors.Error,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = uiState.error!!)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Video Header Section with overlaid branding
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Takes available space
                ) {
                    // Looping Video Background
                    LoopingAssetVideo(
                        assetFileName = "login_header.mp4",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Dark overlay for better text visibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )

                    // Bottom gradient scrim for smooth transition
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        AppColors.Background.copy(alpha = 0.8f),
                                        AppColors.Background
                                    )
                                )
                            )
                    )

                    // Support button overlaid at top-right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.openWhatsAppSupport() }) {
                            Text(
                                text = "Support",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }

                    // Centered Logo and Branding (overlaid on video)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Logo with glow effect
                        LogoWithGlow()

                        Spacer(modifier = Modifier.height(20.dp))

                        // SUDAR title
                        Text(
                            text = "SUDAR",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.Primary,
                            letterSpacing = 4.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(AppColors.Primary.copy(alpha = 0.15f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "TNPSC Mock Test App",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Login Form Section - Fixed at bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Background)
                        .imePadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 24.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Login/Signup text
                    Text(
                        text = "Log in/Sign up",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = AppColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mobile Number Label
                    Text(
                        text = "Mobile Number",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mobile Number Input
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { value ->
                            if (value.length <= 10 && value.all { it.isDigit() }) {
                                phoneNumber = value
                                phoneError = null
                            }
                        },
                        placeholder = {
                            Text(
                                text = "Enter Your 10 digit Mobile no.",
                                color = AppColors.TextLight,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = AppColors.SurfaceLight,
                            focusedContainerColor = AppColors.SurfaceLight,
                            unfocusedBorderColor = AppColors.Border,
                            focusedBorderColor = AppColors.Primary,
                            errorBorderColor = AppColors.Error,
                            unfocusedTextColor = AppColors.TextPrimary,
                            focusedTextColor = AppColors.TextPrimary,
                            cursorColor = AppColors.Primary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (phoneNumber.length == 10) {
                                    onNavigateToOtp(phoneNumber)
                                }
                            }
                        ),
                        singleLine = true,
                        isError = phoneError != null
                    )

                    if (phoneError != null) {
                        Text(
                            text = phoneError!!,
                            color = AppColors.Error,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Continue with OTP Button
                    val isValid = phoneNumber.length == 10

                    Button(
                        onClick = {
                            if (phoneNumber.isEmpty()) {
                                phoneError = "Please enter your mobile number"
                            } else if (phoneNumber.length != 10) {
                                phoneError = "Please enter a valid 10-digit mobile number"
                            } else {
                                focusManager.clearFocus()
                                onNavigateToOtp(phoneNumber)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isValid) AppColors.Primary else Color(0xFFBDBDBD)
                        ),
                        enabled = !uiState.isLoading && !uiState.truecallerLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Sms,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CONTINUE WITH OTP",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Guest Login Button (if enabled)
                    if (uiState.guestLoginEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { /* Navigate to guest login */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(listOf(AppColors.Primary, AppColors.Primary))
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Guest Login (U015)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // Loading Overlay
        if (uiState.truecallerLoading || uiState.isLoading) {
            LoadingOverlay(
                message = if (uiState.truecallerLoading) "Verifying..." else "Logging in...",
                showSlowConnection = uiState.showSlowConnection,
                onCancel = if (uiState.isLoading) viewModel::cancelLogin else null
            )
        }
    }
}

@Composable
private fun LogoWithGlow() {
    Box(
        modifier = Modifier
            .size(90.dp)
            .shadow(
                elevation = 30.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = AppColors.Primary.copy(alpha = 0.5f),
                spotColor = AppColors.Primary.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A1A)), // Dark background for contrast
        contentAlignment = Alignment.Center
    ) {
        // Logo placeholder - replace with actual logo
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = "SUDAR Logo",
            modifier = Modifier.size(50.dp),
            tint = AppColors.Primary
        )
    }
}

@Composable
private fun LoadingOverlay(
    message: String,
    showSlowConnection: Boolean,
    onCancel: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 300.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 40.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = AppColors.Primary,
                    strokeWidth = 3.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (showSlowConnection) "Slow connection, please wait..." else "Please wait",
                    fontSize = 13.sp,
                    color = if (showSlowConnection) AppColors.Warning else AppColors.TextSecondary
                )

                if (onCancel != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onCancel) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.Error
                        )
                    }
                }
            }
        }
    }
}
