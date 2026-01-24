package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

/**
 * Test Intro Screen - Exact Flutter replica
 * Shows test info and instructions before starting the test.
 * 
 * Includes premium gate: if user is not premium, redirects to subscription offer
 * and blocks test start (same behavior as Flutter tests_page.dart::_startTest).
 */
@Composable
fun TestIntroScreen(
    testTitle: String,
    categoryName: String,
    examName: String,
    sessionId: Int,
    totalQuestions: Int,
    duration: Int,
    onBack: () -> Unit,
    onStartTest: () -> Unit,
    onSubscriptionNeeded: () -> Unit = {},
    viewModel: TestIntroViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // React to premium check result
    LaunchedEffect(uiState.premiumCheckResult) {
        when (val result = uiState.premiumCheckResult) {
            is PremiumCheckResult.Checked -> {
                viewModel.resetCheckResult() // Reset for next time
                if (result.isPremium) {
                    // User is premium, proceed to test
                    onStartTest()
                } else {
                    // User is not premium, show subscription offer
                    onSubscriptionNeeded()
                }
            }
            else -> { /* Waiting or not checked yet */ }
        }
    }

    // Check if currently checking premium
    val isChecking = uiState.premiumCheckResult is PremiumCheckResult.Checking

    // Handle Start Exam click - triggers premium check
    val handleStartExam: () -> Unit = {
        viewModel.onStartExamClicked()
    }
    Scaffold(
        containerColor = AppColors.Background,
        bottomBar = {
            // Start Exam Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.CardBackground,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = handleStartExam,
                        enabled = !isChecking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981) // Green
                        )
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Exam",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
            // Header Section with dark gradient
            TestIntroHeader(
                testTitle = testTitle,
                categoryName = categoryName,
                totalQuestions = totalQuestions,
                onBack = onBack
            )

            // Body Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Info Card
                InfoCard(
                    examName = examName,
                    categoryName = categoryName,
                    testTitle = testTitle,
                    totalQuestions = totalQuestions,
                    duration = duration
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Instructions Card
                InstructionsCard()

                Spacer(modifier = Modifier.height(100.dp)) // Space for bottom button
            }
        }
    }
}

@Composable
private fun TestIntroHeader(
    testTitle: String,
    categoryName: String,
    totalQuestions: Int,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C3E50), // Dark blue-gray
                        Color(0xFF34495E)  // Slightly lighter
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row: Back button and Questions badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Questions Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Quiz,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$totalQuestions Questions",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Document Icon
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Assignment,
                    contentDescription = null,
                    tint = Color(0xFF2C3E50),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Test Title
            Text(
                text = testTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category
            Text(
                text = categoryName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun InfoCard(
    examName: String,
    categoryName: String,
    testTitle: String,
    totalQuestions: Int,
    duration: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        Column {
            // Exam Name
            InfoRow(
                icon = Icons.Outlined.School,
                iconColor = Color(0xFF6366F1), // Indigo
                label = "Exam Name",
                value = examName.ifEmpty { "TNPSC" }
            )
            InfoDivider()

            // Exam Type (Category)
            InfoRow(
                icon = Icons.Outlined.Category,
                iconColor = Color(0xFF8B5CF6), // Violet
                label = "Exam Type",
                value = categoryName
            )
            InfoDivider()

            // Question Paper
            InfoRow(
                icon = Icons.Outlined.Description,
                iconColor = Color(0xFF06B6D4), // Cyan
                label = "Question Paper",
                value = testTitle
            )
            InfoDivider()

            // Total Questions
            InfoRow(
                icon = Icons.Outlined.Help,
                iconColor = Color(0xFF10B981), // Emerald
                label = "Total Questions",
                value = "$totalQuestions"
            )
            InfoDivider()

            // Duration
            InfoRow(
                icon = Icons.Outlined.Timer,
                iconColor = Color(0xFFF59E0B), // Amber
                label = "Duration",
                value = "$duration mins"
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Container
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Label
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )

        // Value
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = AppColors.Background
    )
}

@Composable
private fun InstructionsCard() {
    val instructions = listOf(
        "Answer all questions within the time limit",
        "Each correct answer carries 1 mark",
        "There is no negative marking",
        "You can review and change your answers"
    )

    // Dark mode instructions card
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF332B00) // Dark yellow/amber tint for dark mode
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B), // Amber
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Instructions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFBBF24) // Light amber
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Instructions List
            instructions.forEach { instruction ->
                Row(
                    modifier = Modifier.padding(bottom = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•  ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFCD34D) // Amber light
                    )
                    Text(
                        text = instruction,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFCD34D).copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
