package com.sudar.tnpscapp.nativeapp.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProgressData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Your Progress",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = AppColors.TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadProgressData() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        containerColor = AppColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Error Message
            if (uiState.errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.Error.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Error,
                            contentDescription = null,
                            tint = AppColors.Error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            fontSize = 13.sp,
                            color = AppColors.Error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Overall Stats Card
            OverallStatsCard(
                testsTaken = uiState.testsTaken,
                avgScore = uiState.avgScore,
                rank = uiState.rank,
                streak = uiState.streak,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Performance Overview Section
            Text(
                "Performance Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            PerformanceChart(
                performanceTrend = uiState.performanceTrend,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Tests Section
            Text(
                "Recent Tests",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            } else if (uiState.testHistory.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.CardBackground
                ) {
                    Column(
                        modifier = Modifier.padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = AppColors.TextLight
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No test history yet",
                            fontSize = 16.sp,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Start taking tests to see your progress",
                            fontSize = 14.sp,
                            color = AppColors.TextLight
                        )
                    }
                }
            } else {
                uiState.testHistory.forEach { test ->
                    RecentTestCard(
                        title = test.sessionName,
                        date = viewModel.formatDate(test.completedAt),
                        score = test.percentage.toInt(),
                        questions = test.totalQuestions
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Strengths & Weaknesses Section
            Text(
                "Strengths & Weaknesses",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Strengths
                StrengthWeaknessCard(
                    modifier = Modifier.weight(1f),
                    title = "Strengths",
                    icon = Icons.Default.ThumbUp,
                    color = AppColors.Success,
                    items = uiState.strengths.map { "${it.name} (${it.avgScore.toInt()}%)" },
                    emptyText = "No strengths yet"
                )

                // Weaknesses
                StrengthWeaknessCard(
                    modifier = Modifier.weight(1f),
                    title = "Needs Work",
                    icon = Icons.Default.ThumbDown,
                    color = AppColors.Error,
                    items = uiState.weaknesses.map { "${it.name} (${it.avgScore.toInt()}%)" },
                    emptyText = "No areas to improve"
                )
            }
        }
    }
}

@Composable
private fun OverallStatsCard(
    testsTaken: Int,
    avgScore: Double,
    rank: Int,
    streak: Int,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(AppColors.Primary, AppColors.Primary.copy(alpha = 0.8f))
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AssignmentTurnedIn,
                        label = "Tests Taken",
                        value = if (isLoading) "..." else "$testsTaken"
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    StatItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TrendingUp,
                        label = "Avg Score",
                        value = if (isLoading) "..." else "${avgScore.toInt()}%"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.24f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    StatItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.EmojiEvents,
                        label = "Rank",
                        value = if (isLoading) "..." else if (rank > 0) "#$rank" else "--"
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    StatItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Streak",
                        value = if (isLoading) "..." else "$streak ${if (streak == 1) "day" else "days"}"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun PerformanceChart(
    performanceTrend: List<PerformanceTrendItem>,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else if (performanceTrend.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No performance data yet",
                    color = AppColors.TextSecondary
                )
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Performance Trend",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Simple bar chart
                val displayTrends = performanceTrend.takeLast(7)
                val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    displayTrends.forEachIndexed { index, item ->
                        ChartBar(
                            value = (item.score / 100.0).toFloat(),
                            score = item.score.toInt(),
                            label = if (index < labels.size) labels[index] else "T${index + 1}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartBar(
    value: Float,
    score: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(40.dp)
    ) {
        Text(
            text = "$score",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height((80 * value.coerceIn(0f, 1f)).dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(AppColors.PrimaryLight, AppColors.Primary)
                    )
                )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = AppColors.TextLight
        )
    }
}

@Composable
private fun RecentTestCard(
    title: String,
    date: String,
    score: Int,
    questions: Int
) {
    val iconColor = when {
        score >= 75 -> AppColors.Success
        score >= 50 -> AppColors.Warning
        else -> AppColors.Error
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (score >= 50) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = date,
                    fontSize = 12.sp,
                    color = AppColors.TextLight
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$score%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Text(
                    text = "$questions Qs",
                    fontSize = 11.sp,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun StrengthWeaknessCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    items: List<String>,
    emptyText: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.4f)))
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                Text(
                    text = emptyText,
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            } else {
                items.forEach { item ->
                    Surface(
                        modifier = Modifier.padding(vertical = 3.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = color,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
