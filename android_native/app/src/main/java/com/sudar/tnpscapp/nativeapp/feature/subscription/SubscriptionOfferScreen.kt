package com.sudar.tnpscapp.nativeapp.feature.subscription

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sudar.tnpscapp.nativeapp.ui.components.LoopingAssetVideo
import com.sudar.tnpscapp.nativeapp.ui.components.LoopingVideo
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

// Colors for dark theme subscription offer
private val GreenAccent = Color(0xFF22C55E)
private val GreenBadge = Color(0xFF16A34A)
private val StarYellow = Color(0xFFFBBF24)
private val TimelineGreen = Color(0xFF22C55E)
private val AlarmOrange = Color(0xFFEF4444)
private val DiamondBlue = Color(0xFF3B82F6)
private val DarkBackground = Color(0xFF0A0A0A)
private val CardBackground = Color(0xFF1A1A1A)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF9CA3AF)
private val TextLightGray = Color(0xFF6B7280)

@Composable
fun SubscriptionOfferScreen(
    onSkip: () -> Unit,
    onSubscribed: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val uiState by viewModel.uiState.collectAsState()
    var isLaunchingPayment by remember { mutableStateOf(false) }

    // Set status bar color to match dark background
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val originalStatusBarColor = window.statusBarColor
        val originalNavBarColor = window.navigationBarColor
        
        // Set status bar to match dark background
        window.statusBarColor = DarkBackground.toArgb()
        // Set navigation bar to match dark background
        window.navigationBarColor = DarkBackground.toArgb()
        // Light icons on dark background
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        
        onDispose {
            // Restore original colors when leaving this screen
            window.statusBarColor = originalStatusBarColor
            window.navigationBarColor = originalNavBarColor
        }
    }

    // Activity result launcher for Razorpay checkout
    val razorpayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLaunchingPayment = false
        if (result.resultCode == Activity.RESULT_OK) {
            // Subscription successful - trigger callback
            onSubscribed()
        }
        // RESULT_CANCELED - user stays on offer screen
    }

    // Function to launch Razorpay payment
    val launchRazorpayPayment: () -> Unit = {
        if (!isLaunchingPayment) {
            isLaunchingPayment = true
            val intent = Intent(context, RazorpaySubscriptionActivity::class.java)
            // Explicit legacy flow (₹5 mandate refunded, old screen with video)
            intent.putExtra(RazorpaySubscriptionActivity.EXTRA_FLOW, RazorpaySubscriptionActivity.FLOW_LEGACY_SUBSCRIPTION)
            razorpayLauncher.launch(intent)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSubscriptionStatus()
    }

    // If user is already premium, skip to home
    LaunchedEffect(uiState.isPremium) {
        if (uiState.isPremium && !uiState.isLoading) {
            onSkip()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding() // Add padding for status bar
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // "7 Day FREE Trial" header with collapse icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = TextWhite,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "7 Day ",
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GreenBadge
                    ) {
                        Text(
                            "FREE",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        " Trial",
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(28.dp)) // Balance the row
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Star rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    Icon(
                        if (index < 4) Icons.Filled.Star else Icons.Filled.StarHalf,
                        contentDescription = null,
                        tint = StarYellow,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "4.6",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Video Banner
            VideoPremiumBanner(videoPath = uiState.premiumVideoPath)

            Spacer(modifier = Modifier.height(20.dp))

            // Timeline card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Step 1: 7 Day Premium for FREE
                    TimelineStep(
                        icon = Icons.Default.CheckCircle,
                        iconColor = TimelineGreen,
                        iconBackground = TimelineGreen.copy(alpha = 0.15f),
                        title = "7 Day Premium for FREE",
                        subtitle = "₹ 5 to verify payment, refunded immediately.",
                        showLine = true,
                        lineColor = TimelineGreen
                    )

                    // Step 2: Day 6 - We Remind You
                    TimelineStep(
                        icon = Icons.Default.Alarm,
                        iconColor = AlarmOrange,
                        iconBackground = AlarmOrange.copy(alpha = 0.15f),
                        title = "Day 6 - We Remind You",
                        subtitle = "We'll let you know before your trial ends.",
                        showLine = true,
                        lineColor = TextGray.copy(alpha = 0.3f)
                    )

                    // Step 3: Day 8 - You're a Member
                    TimelineStep(
                        icon = Icons.Default.Diamond,
                        iconColor = DiamondBlue,
                        iconBackground = DiamondBlue.copy(alpha = 0.15f),
                        title = "Day 8 - You're a SUDAR Member",
                        subtitle = "Autopay ₹ 299 / month unless cancelled.",
                        showLine = false,
                        lineColor = Color.Transparent
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom fixed section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground)
                // Keep CTA + "Skip" above system navigation bar (3-button / gesture)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = Color(0xFF2A2A2A))

            Spacer(modifier = Modifier.height(12.dp))

            // Try Now button - full width
            Button(
                onClick = launchRazorpayPayment,
                enabled = !isLaunchingPayment && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenAccent,
                    disabledContainerColor = GreenAccent.copy(alpha = 0.5f)
                )
            ) {
                if (isLaunchingPayment) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = TextWhite,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        "Try Now",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Skip link
            Text(
                "Skip and explore Home",
                color = TextGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onSkip() }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun VideoPremiumBanner(videoPath: String?) {
    // Red/coral gradient border container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFFB91C1C))),
                RoundedCornerShape(16.dp)
            )
            .padding(3.dp)
    ) {
        // Video container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFB91C1C))
        ) {
            // Looping video with audio enabled
            // Uses cached server video if available, falls back to bundled asset
            if (videoPath.isNullOrBlank()) {
                // Use bundled asset video
                LoopingAssetVideo(
                    assetFileName = "premium_video.mp4",
                    modifier = Modifier.fillMaxWidth(),
                    volume = 1f,
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
                    useVideoAspectRatio = true,
                )
            } else {
                // Use server-cached video with fallback
                LoopingVideo(
                    filePath = videoPath,
                    assetFallback = "premium_video.mp4",
                    modifier = Modifier.fillMaxWidth(),
                    volume = 1f,
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
                    useVideoAspectRatio = true,
                )
            }
        }
    }
}

@Composable
fun TimelineStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    showLine: Boolean,
    lineColor: Color
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Icon column with line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            // Connecting line
            if (showLine) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(40.dp)
                        .background(lineColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Text column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                color = TextGray,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            if (showLine) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
