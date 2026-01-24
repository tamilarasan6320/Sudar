package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sudar.tnpscapp.nativeapp.core.network.model.QuestionSessionDto
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

/**
 * Saved Tests Screen - Flutter replica
 * Shows list of bookmarked/saved tests
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedTestsScreen(
    onBack: () -> Unit = {},
    onStartTest: (sessionId: Int, title: String, categoryName: String, duration: Int, totalQuestions: Int) -> Unit = { _, _, _, _, _ -> },
    viewModel: SavedTestsViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    var selectedFilter by remember { mutableStateOf("All") }

    val totalSessions = uiState.sessions.size
    val notStarted = totalSessions // All sessions considered "not started" for now

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Saved Tests",
                        fontWeight = FontWeight.Bold,
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
                    Text(uiState.error, color = AppColors.Error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadSessions() },
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
                        .padding(16.dp)
                ) {
                    // Summary Card
                    SummaryCard(
                        total = totalSessions,
                        notStarted = notStarted,
                        inProgress = 0
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Filter Tabs
                    FilterTabs(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section Title
                    Text(
                        text = "Your Saved Tests",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tests List
                    if (uiState.sessions.isEmpty()) {
                        EmptyState()
                    } else {
                        Column {
                            uiState.sessions.forEachIndexed { index, session ->
                                SavedTestCard(
                                    session = session,
                                    isNew = index < 2,
                                    onRemove = { /* TODO: Implement remove */ },
                                    onStartTest = {
                                        onStartTest(
                                            session.id,
                                            session.name ?: "Test",
                                            session.categoryName ?: "",
                                            session.duration ?: 180,
                                            session.actualQuestionCount ?: session.totalQuestions ?: 0
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(total: Int, notStarted: Int, inProgress: Int) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(value = total.toString(), label = "Saved Tests", icon = Icons.Outlined.Bookmark)
            VerticalDivider(modifier = Modifier.height(40.dp), color = AppColors.CardBackground.copy(alpha = 0.24f))
            StatItem(value = notStarted.toString(), label = "Not Started", icon = Icons.Outlined.Pending)
            VerticalDivider(modifier = Modifier.height(40.dp), color = AppColors.CardBackground.copy(alpha = 0.24f))
            StatItem(value = inProgress.toString(), label = "In Progress", icon = Icons.Outlined.PlayCircleOutline)
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
            fontSize = 11.sp,
            color = AppColors.CardBackground.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun FilterTabs(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        Row {
            listOf("All", "Not Started", "In Progress").forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) AppColors.Primary else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else AppColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
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
                imageVector = Icons.Outlined.Bookmark,
                contentDescription = null,
                tint = AppColors.TextLight,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No tests available yet",
                fontSize = 16.sp,
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Check back later for new tests",
                fontSize = 14.sp,
                color = AppColors.TextLight
            )
        }
    }
}

@Composable
private fun SavedTestCard(
    session: QuestionSessionDto,
    isNew: Boolean,
    onRemove: () -> Unit,
    onStartTest: () -> Unit
) {
    val questionCount = session.actualQuestionCount ?: session.totalQuestions ?: 0
    val sessionDuration = session.duration ?: 180
    val difficulty = when {
        questionCount <= 20 -> "Easy"
        questionCount <= 50 -> "Medium"
        else -> "Hard"
    }
    val difficultyColor = when (difficulty) {
        "Easy" -> AppColors.Success
        "Hard" -> AppColors.Error
        else -> Color(0xFFFF9800)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.name ?: "Test",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isNew) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AppColors.Success.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "NEW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Success,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = session.description ?: "Test Session",
                fontSize = 13.sp,
                color = AppColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Info Chips Row
            Row {
                InfoChip(
                    icon = Icons.Outlined.Quiz,
                    text = "$questionCount Questions",
                    color = AppColors.Primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                InfoChip(
                    icon = Icons.Outlined.AccessTime,
                    text = "$sessionDuration Minutes",
                    color = AppColors.Secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                InfoChip(
                    icon = Icons.Outlined.SignalCellularAlt,
                    text = difficulty,
                    color = difficultyColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row {
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Error),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = Brush.linearGradient(listOf(AppColors.Error, AppColors.Error))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkRemove,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onStartTest,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Test", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}
