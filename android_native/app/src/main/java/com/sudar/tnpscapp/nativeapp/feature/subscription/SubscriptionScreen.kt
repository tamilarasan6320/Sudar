package com.sudar.tnpscapp.nativeapp.feature.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sudar.tnpscapp.nativeapp.R
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

// Colors matching Flutter subscription_offer_page
private val PurpleAccent = Color(0xFF5E60CE)
private val CoralAccent = Color(0xFFFF6B6B)
private val OrangeAccent = Color(0xFFFFAB40)
private val GreenCheck = Color(0xFF00C853)
private val GoldPremium = Color(0xFFFFD700)
private val DarkBg = Color(0xFF1A1A2E)
private val DarkBg2 = Color(0xFF16213E)
private val TextDark = Color(0xFF1F2937)
private val TextGray = Color(0xFF6B7280)
private val TextLightGray = Color(0xFF9CA3AF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSubscriptionStatus()
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PurpleAccent)
            }
        }
        uiState.error != null -> {
            ErrorView(
                error = uiState.error!!,
                onRetry = { viewModel.loadSubscriptionStatus() },
                onBack = onBack
            )
        }
        uiState.isPremium -> {
            PremiumView(
                trialDaysLeft = uiState.trialDaysLeft,
                daysLeft = uiState.daysLeft,
                isTrial = uiState.isTrial,
                premiumSource = uiState.premiumSource,
                onBack = onBack
            )
        }
        else -> {
            SubscribeView(
                onStartTrial = { viewModel.startSubscription() },
                isProcessing = uiState.isProcessing,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF667eea), Color(0xFF764ba2))
                )
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF667eea)
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PremiumView(
    trialDaysLeft: Int?,
    daysLeft: Int?,
    isTrial: Boolean,
    premiumSource: String?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF667eea), Color(0xFF764ba2))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF22C55E)
            ) {
                Text(
                    "PREMIUM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Premium Badge
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.15f)
        ) {
            Column(
                modifier = Modifier.padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = GoldPremium,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "👑 Premium Member",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (trialDaysLeft != null && isTrial) {
                    Surface(
                        shape = RoundedCornerShape(25.dp),
                        color = Color(0xFFF97316)
                    ) {
                        Text(
                            "🆓 Free Trial: $trialDaysLeft days left",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                } else if (daysLeft != null) {
                    Text(
                        "$daysLeft days remaining",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Features
        PremiumFeatureCard("📚", "All Premium Questions", "Access 10,000+ questions")
        PremiumFeatureCard("📊", "Detailed Analytics", "Track your progress")
        PremiumFeatureCard("🎯", "Topic-wise Tests", "Practice by topic")
        PremiumFeatureCard("📱", "Ad-free Experience", "Focus on learning")

        Spacer(modifier = Modifier.height(30.dp))

        // Subscription Details
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.1f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Subscription Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                DetailRow("Plan", "₹299/month")
                DetailRow("Status", "ACTIVE")
                DetailRow("Source", premiumSource?.replaceFirstChar { it.uppercase() } ?: "Subscription")
                if (daysLeft != null) {
                    DetailRow("Days Remaining", "$daysLeft days")
                }
            }
        }
    }
}

@Composable
private fun SubscribeView(
    onStartTrial: () -> Unit,
    isProcessing: Boolean,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Professional Premium Banner
            PremiumBanner()

            Spacer(modifier = Modifier.height(20.dp))

            // "UNLOCK PREMIUM" title
            Text(
                "UNLOCK PREMIUM",
                color = CoralAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Gradient divider line
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(2.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(
                        Brush.horizontalGradient(
                            listOf(PurpleAccent, CoralAccent)
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Headline
            Text(
                "Try 7 days for",
                color = TextDark,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Big price
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "₹",
                    color = TextGray,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "5",
                    color = TextDark,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 80.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Then ₹299/month
            Text(
                "Then ₹299/month",
                color = TextLightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Cancel Anytime",
                color = CoralAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Feature list
            FeatureCheckRow("10,000+ questions")
            FeatureCheckRow("New questions daily")
            FeatureCheckRow("No Ads")

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom fixed section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                // Keep CTA + "Skip" above system navigation bar (3-button / gesture)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Try for 7 days and cancel anytime",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // CTA button with gradient
            Button(
                onClick = onStartTrial,
                enabled = !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(CoralAccent, OrangeAccent)
                            ),
                            RoundedCornerShape(30.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "₹5",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                "START TRIAL",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Skip link
            Text(
                "Skip and explore Home",
                color = TextGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onBack() }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PremiumBanner() {
    // Gradient border container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(PurpleAccent, CoralAccent)
                ),
                RoundedCornerShape(18.dp)
            )
            .padding(3.dp)
    ) {
        // Inner content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(DarkBg, DarkBg2)
                    )
                )
                .height(180.dp)
        ) {
            // Background pattern circles
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = 280.dp, y = (-30).dp)
                    .background(
                        PurpleAccent.copy(alpha = 0.1f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = (-20).dp, y = 130.dp)
                    .background(
                        CoralAccent.copy(alpha = 0.1f),
                        CircleShape
                    )
            )

            // Main content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo box
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // App logo - using gradient background with S text
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(AppColors.Primary, AppColors.PrimaryDark)
                                ),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "S",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "SUDAR TNPSC",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = Color.White // Will apply gradient effect via shader in production
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Premium Features",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Feature highlights
                    Row {
                        MiniFeature(Icons.Default.CheckCircle, "All Tests")
                        Spacer(modifier = Modifier.width(12.dp))
                        MiniFeature(Icons.Default.Star, "Ad-Free")
                    }
                }
            }

            // Premium badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(GoldPremium, Color(0xFFFFA500))
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "PRO",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniFeature(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = PurpleAccent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun FeatureCheckRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = GreenCheck,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text,
            color = Color(0xFF374151),
            fontSize = 15.sp
        )
    }
}

@Composable
private fun PremiumFeatureCard(emoji: String, title: String, subtitle: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f))
        Text(value, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
