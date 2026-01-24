package com.sudar.tnpscapp.nativeapp.feature.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sudar.tnpscapp.nativeapp.core.network.model.ExamCategoryDto
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

/**
 * Exam Selection Screen - Exact Flutter replica
 * Features:
 * - Header with title and description
 * - List of exam categories with radio selection
 * - Auto-select Group 4 by default
 * - Fixed continue button at bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSelectionScreen(
    onExamSelected: () -> Unit,
    viewModel: ExamSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle exam selection complete
    LaunchedEffect(uiState.selectionComplete) {
        if (uiState.selectionComplete) {
            onExamSelected()
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Select Exam",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        bottomBar = {
            // Continue Button (Fixed at bottom)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.CardBackground,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = { viewModel.confirmSelection() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.selectedExam != null) AppColors.Primary else Color(0xFFBDBDBD)
                        ),
                        enabled = uiState.selectedExam != null && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "CONTINUE",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoadingCategories -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            }
            uiState.examCategories.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = AppColors.TextLight,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No exam categories available",
                            fontSize = 16.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    // Header
                    Text(
                        text = "Select Your TNPSC Exam",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Choose the exam you are preparing for. Group 4 is our primary focus with comprehensive content.",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary,
                        lineHeight = 21.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Exam List
                    uiState.examCategories.forEach { exam ->
                        ExamCard(
                            exam = exam,
                            isSelected = uiState.selectedExam?.id == exam.id,
                            onClick = { viewModel.selectExam(exam) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Extra space for bottom button
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun ExamCard(
    exam: ExamCategoryDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconText = getIconText(exam.name)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, AppColors.Primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) {
                            Brush.linearGradient(
                                colors = listOf(AppColors.Primary, AppColors.PrimaryLight)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    AppColors.Border.copy(alpha = 0.3f),
                                    AppColors.Border.copy(alpha = 0.5f)
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconText,
                    fontSize = if (iconText.length > 2) 14.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else AppColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exam.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )

                if (!exam.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exam.description,
                        fontSize = 13.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }

            // Radio Button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) AppColors.Primary else AppColors.Border,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(AppColors.Primary)
                    )
                }
            }
        }
    }
}

private fun getIconText(examName: String): String {
    return when {
        examName.contains("Group 1") -> "1"
        examName.contains("Group 2A") -> "2A"
        examName.contains("Group 2") -> "2"
        examName.contains("Group 4") -> "4"
        examName.contains("TNUSRB") -> "PO"
        examName.contains("VAO") -> "VAO"
        else -> examName.take(1).uppercase()
    }
}
