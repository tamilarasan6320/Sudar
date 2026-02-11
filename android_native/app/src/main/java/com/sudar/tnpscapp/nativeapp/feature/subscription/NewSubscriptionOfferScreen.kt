package com.sudar.tnpscapp.nativeapp.feature.subscription

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * New Subscription Offer Screen (₹2 upfront, includes premium video from API)
 * Used when payment type is "₹2 non-refundable" (Option B).
 */
@Composable
fun NewSubscriptionOfferScreen(
    onSkip: () -> Unit,
    onSubscribed: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val uiState by viewModel.uiState.collectAsState()
    var isLaunchingPayment by remember { mutableStateOf(false) }

    // Set status/navigation bar colors to match dark background
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val originalStatusBarColor = window.statusBarColor
        val originalNavBarColor = window.navigationBarColor

        window.statusBarColor = Color(0xFF0A0A0A).toArgb()
        window.navigationBarColor = Color(0xFF0A0A0A).toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false

        onDispose {
            window.statusBarColor = originalStatusBarColor
            window.navigationBarColor = originalNavBarColor
        }
    }

    val razorpayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLaunchingPayment = false
        if (result.resultCode == Activity.RESULT_OK) {
            onSubscribed()
        }
    }

    val launchPayment: () -> Unit = {
        if (!isLaunchingPayment) {
            isLaunchingPayment = true
            val intent = Intent(context, RazorpaySubscriptionActivity::class.java).apply {
                putExtra(RazorpaySubscriptionActivity.EXTRA_FLOW, RazorpaySubscriptionActivity.FLOW_TRIAL_FEE_THEN_SUBSCRIPTION)
            }
            razorpayLauncher.launch(intent)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSubscriptionStatus()
    }

    // If user already premium, skip
    LaunchedEffect(uiState.isPremium) {
        if (uiState.isPremium && !uiState.isLoading) {
            onSkip()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "7 Day FREE Trial",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(28.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Video Banner (from Admin Settings → Premium Video)
            VideoPremiumBanner(videoPath = uiState.premiumVideoPath)

            Spacer(modifier = Modifier.height(20.dp))

            // Timeline card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1A1A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Step 1: ₹2 non-refundable
                    TimelineStep(
                        icon = Icons.Default.CurrencyRupee,
                        iconColor = Color(0xFF22C55E),
                        iconBackground = Color(0xFF22C55E).copy(alpha = 0.15f),
                        title = "Pay ₹2 (Non‑Refundable)",
                        subtitle = "One‑time trial verification fee.",
                        showLine = true,
                        lineColor = Color(0xFF22C55E)
                    )

                    // Step 2: 7 Day Premium Free
                    TimelineStep(
                        icon = Icons.Default.CheckCircle,
                        iconColor = Color(0xFF22C55E),
                        iconBackground = Color(0xFF22C55E).copy(alpha = 0.15f),
                        title = "7 Day Premium for FREE",
                        subtitle = "Enjoy full access for 7 days.",
                        showLine = true,
                        lineColor = Color.White.copy(alpha = 0.12f)
                    )

                    // Step 3: Reminder
                    TimelineStep(
                        icon = Icons.Default.Alarm,
                        iconColor = Color(0xFFEF4444),
                        iconBackground = Color(0xFFEF4444).copy(alpha = 0.15f),
                        title = "Day 6 — We Remind You",
                        subtitle = "We'll notify you before your trial ends.",
                        showLine = true,
                        lineColor = Color.White.copy(alpha = 0.12f)
                    )

                    // Step 4: Paid
                    TimelineStep(
                        icon = Icons.Default.Diamond,
                        iconColor = Color(0xFF3B82F6),
                        iconBackground = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        title = "Day 8 — You're a SUDAR Member",
                        subtitle = "Autopay ₹299 / month unless cancelled.",
                        showLine = false,
                        lineColor = Color.Transparent
                    )
                }
            }
        }

        // Bottom fixed section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0A0A))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = Color(0xFF2A2A2A))
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = launchPayment,
                enabled = !isLaunchingPayment && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22C55E),
                    disabledContainerColor = Color(0xFF22C55E).copy(alpha = 0.5f)
                )
            ) {
                if (isLaunchingPayment) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Pay ₹2 & Start Trial",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Skip and explore Home",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onSkip() }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

