package com.sudar.tnpscapp.nativeapp.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Quiz
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Performance Analytics Screen - Flutter replica
 * Shows detailed performance analytics with charts and analysis
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceScreen(
    onBack: () -> Unit = {},
    viewModel: PerformanceViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Performance Analytics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            }

            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.error, color = AppColors.Error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadPerformance() },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) {
                        Text("Retry")
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
                ) {
                    // Overall Performance Card
                    OverallPerformanceCard(uiState)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Time Period Selector
                    TimePeriodSelector(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = { viewModel.changePeriod(it) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Performance Chart
                    PerformanceChart(trends = uiState.trends)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Subject-wise Performance
                    Text(
                        text = "Subject-wise Performance",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.subjects.isEmpty()) {
                        EmptyCard("No subject data available")
                    } else {
                        uiState.subjects.forEach { subject ->
                            SubjectPerformanceCard(subject)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Analysis Section
                    Text(
                        text = "Analysis",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row {
                        StrengthsCard(
                            strengths = uiState.strengths,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        WeaknessesCard(
                            weaknesses = uiState.weaknesses,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Recent Activity
                    Text(
                        text = "Recent Activity",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ActivityTimeline(trends = uiState.trends.takeLast(5))
                }
            }
        }
    }
}

@Composable
private fun OverallPerformanceCard(uiState: PerformanceUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(AppColors.Primary, AppColors.Primary.copy(alpha = 0.8f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Overall Score",
                fontSize = 14.sp,
                color = AppColors.CardBackground.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${uiState.avgScore.toInt()}%",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.CardBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${uiState.passed} passed, ${uiState.failed} failed",
                fontSize = 13.sp,
                color = AppColors.CardBackground.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = AppColors.CardBackground.copy(alpha = 0.24f))
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    value = uiState.totalTests.toString(),
                    label = "Tests",
                    icon = Icons.Outlined.Quiz
                )
                StatItem(
                    value = String.format("%.1f", uiState.avgTimeMinutes),
                    label = "Hours",
                    icon = Icons.Default.AccessTime
                )
                StatItem(
                    value = "${uiState.bestScore.toInt()}%",
                    label = "Best",
                    icon = Icons.Default.EmojiEvents
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.CardBackground
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppColors.CardBackground.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun TimePeriodSelector(selectedPeriod: String, onPeriodSelected: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        Row {
            listOf("week" to "Week", "month" to "Month", "year" to "Year", "all" to "All").forEach { (code, label) ->
                val isSelected = selectedPeriod == code
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPeriodSelected(code) }
                        .background(
                            if (isSelected) AppColors.Primary else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else AppColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceChart(trends: List<TrendItem>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        if (trends.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trend data available",
                    color = AppColors.TextSecondary
                )
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Score Trend",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                val chartData = trends.takeLast(7)
                val maxScore = chartData.maxOfOrNull { it.score } ?: 100f
                val maxHeight = if (maxScore > 0) maxScore else 100f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartData.forEachIndexed { index, item ->
                        val heightFraction = (item.score / maxHeight).coerceIn(0f, 1f)
                        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        val label = dayLabels.getOrElse(index) { "Day ${index + 1}" }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = item.score.toInt().toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(heightFraction.coerceAtLeast(0.05f))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(AppColors.PrimaryLight, AppColors.Primary)
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
                }
            }
        }
    }
}

@Composable
private fun SubjectPerformanceCard(subject: SubjectPerformance) {
    val color = getCategoryColor(subject.categoryName)
    val progress = (subject.avgScore / 100f).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subject.categoryName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "${subject.avgScore.toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${subject.passed}/${subject.testCount}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun StrengthsCard(strengths: List<AnalysisItem>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = Brush.linearGradient(listOf(AppColors.Success.copy(alpha = 0.3f), AppColors.Success.copy(alpha = 0.3f)))
        ),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = AppColors.Success,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Strengths",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (strengths.isEmpty()) {
                Text(
                    text = "No strengths yet",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            } else {
                strengths.forEach { item ->
                    AnalysisTag(
                        text = "${item.category} (${item.score.toInt()}%)",
                        color = AppColors.Success
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun WeaknessesCard(weaknesses: List<AnalysisItem>, modifier: Modifier = Modifier) {
    val warningColor = Color(0xFFFF9800)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = Brush.linearGradient(listOf(warningColor.copy(alpha = 0.3f), warningColor.copy(alpha = 0.3f)))
        ),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TrendingDown,
                contentDescription = null,
                tint = warningColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Improve",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (weaknesses.isEmpty()) {
                Text(
                    text = "No areas to improve",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            } else {
                weaknesses.forEach { item ->
                    AnalysisTag(
                        text = "${item.category} (${item.score.toInt()}%)",
                        color = warningColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun AnalysisTag(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ActivityTimeline(trends: List<TrendItem>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        if (trends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent activity",
                    color = AppColors.TextSecondary
                )
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {
                trends.forEachIndexed { index, item ->
                    val color = if (item.score >= 50) AppColors.Success else Color(0xFFFF9800)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.testName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.score.toInt()}% Score",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = color
                            )
                        }
                        Text(
                            text = formatDate(item.date),
                            fontSize = 11.sp,
                            color = AppColors.TextLight
                        )
                    }

                    if (index < trends.size - 1) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = AppColors.TextSecondary
            )
        }
    }
}

private fun getCategoryColor(categoryName: String): Color {
    return when {
        categoryName.contains("Tamil", ignoreCase = true) -> AppColors.Primary
        categoryName.contains("Science", ignoreCase = true) -> Color(0xFF00BCD4)
        categoryName.contains("Social", ignoreCase = true) -> AppColors.Success
        categoryName.contains("Aptitude", ignoreCase = true) -> AppColors.Secondary
        categoryName.contains("Current", ignoreCase = true) -> Color(0xFFFF5722)
        else -> AppColors.Primary
    }
}

private fun formatDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = inputFormat.parse(dateStr) ?: return dateStr
        val now = Calendar.getInstance()
        val dateCalendar = Calendar.getInstance().apply { time = date }

        val diffDays = ((now.timeInMillis - dateCalendar.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

        when {
            diffDays == 0 -> "Today"
            diffDays == 1 -> "Yesterday"
            diffDays < 7 -> "$diffDays days ago"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        dateStr
    }
}
