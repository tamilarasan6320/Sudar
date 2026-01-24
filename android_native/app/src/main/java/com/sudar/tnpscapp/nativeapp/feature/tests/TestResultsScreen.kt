package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudar.tnpscapp.nativeapp.core.network.model.QuestionDto
import com.sudar.tnpscapp.nativeapp.ui.components.RichHtmlContent
import com.sudar.tnpscapp.nativeapp.ui.components.containsLatex
import com.sudar.tnpscapp.nativeapp.ui.components.convertLatexToPlainText
import com.sudar.tnpscapp.nativeapp.ui.components.cleanHtmlText
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

/**
 * Test Results Page - Exact Flutter replica
 * Shows results with question-by-question review
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestResultsScreen(
    resultData: TestResultData,
    onBack: () -> Unit,
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val questions = resultData.questions
    
    if (questions.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Test Results") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No questions to review")
            }
        }
        return
    }

    val currentQuestion = questions[currentQuestionIndex]
    val selectedAnswerIndex = resultData.selectedAnswers[currentQuestion.id]
    val selectedAnswerChar = selectedAnswerIndex?.let { ('A'.code + it).toChar().toString() }
    val isCorrect = selectedAnswerChar?.equals(currentQuestion.correctAnswer, ignoreCase = true) == true

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Test Results",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = resultData.testTitle,
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                },
                actions = {
                    // Score badge
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.Success.copy(alpha = 0.1f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.Success,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${resultData.correctAnswers}/${resultData.totalQuestions}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Success
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppColors.Background)
        ) {
            // Progress Bar
            LinearProgressIndicator(
                progress = { (currentQuestionIndex + 1f) / questions.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = AppColors.Primary,
                trackColor = AppColors.Border,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Question Number and Status
                Row {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.Primary)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Question ${currentQuestionIndex + 1}/${questions.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCorrect) AppColors.Success else AppColors.Error)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCorrect) "Correct" else "Incorrect",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Question Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Question Text (English) - Use RichHtmlContent for images
                        if (!currentQuestion.questionEn.isNullOrBlank()) {
                            RichHtmlContent(
                                htmlString = currentQuestion.questionEn,
                                fontSize = 16,
                                fontWeight = FontWeight.Medium,
                                textColor = AppColors.TextPrimary,
                                lineHeight = 24
                            )
                        }
                        // Question Text (Tamil) - Use RichHtmlContent for images
                        if (!currentQuestion.questionTa.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            RichHtmlContent(
                                htmlString = currentQuestion.questionTa,
                                fontSize = 15,
                                textColor = AppColors.TextSecondary,
                                lineHeight = 22
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Options
                val options = listOf(
                    "A" to (currentQuestion.optionAEn ?: ""),
                    "B" to (currentQuestion.optionBEn ?: ""),
                    "C" to (currentQuestion.optionCEn ?: ""),
                    "D" to (currentQuestion.optionDEn ?: "")
                )

                options.forEachIndexed { index, (label, text) ->
                    if (text.isNotBlank()) {
                        val isSelected = selectedAnswerIndex == index
                        val isCorrectOption = label.equals(currentQuestion.correctAnswer, ignoreCase = true)
                        
                        val bgColor = when {
                            isCorrectOption -> AppColors.Success.copy(alpha = 0.15f)
                            isSelected && !isCorrect -> AppColors.Error.copy(alpha = 0.15f)
                            else -> AppColors.CardBackground
                        }
                        val borderColor = when {
                            isCorrectOption -> AppColors.Success
                            isSelected && !isCorrect -> AppColors.Error
                            else -> AppColors.Border
                        }
                        val iconColor = when {
                            isCorrectOption -> AppColors.Success
                            isSelected && !isCorrect -> AppColors.Error
                            else -> AppColors.TextLight
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Option letter circle
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isCorrectOption -> AppColors.Success
                                                isSelected && !isCorrect -> AppColors.Error
                                                else -> AppColors.Background
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCorrectOption) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (isSelected && !isCorrect) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = label,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppColors.TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Option text - handle LaTeX and clean HTML
                                val displayText = if (containsLatex(text)) {
                                    convertLatexToPlainText(text)
                                } else {
                                    cleanHtmlText(text)
                                }
                                Text(
                                    text = displayText,
                                    fontSize = 14.sp,
                                    color = AppColors.TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )

                                // Show indicator for correct/selected
                                if (isCorrectOption || (isSelected && !isCorrect)) {
                                    Text(
                                        text = when {
                                            isCorrectOption && isSelected -> "Your answer ✓"
                                            isCorrectOption -> "Correct answer"
                                            else -> "Your answer ✗"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isCorrectOption) AppColors.Success else AppColors.Error
                                    )
                                }
                            }
                        }
                    }
                }

                // Explanation Section
                if (!currentQuestion.explanationEn.isNullOrBlank() || !currentQuestion.explanationTa.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Explanation",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.Primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if (!currentQuestion.explanationEn.isNullOrBlank()) {
                                Text(
                                    text = currentQuestion.explanationEn,
                                    fontSize = 14.sp,
                                    color = AppColors.TextPrimary,
                                    lineHeight = 20.sp
                                )
                            }
                            if (!currentQuestion.explanationTa.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentQuestion.explanationTa,
                                    fontSize = 13.sp,
                                    color = AppColors.TextSecondary,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.CardBackground)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentQuestionIndex > 0) {
                            currentQuestionIndex--
                        }
                    },
                    enabled = currentQuestionIndex > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.Primary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Previous", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        if (currentQuestionIndex < questions.size - 1) {
                            currentQuestionIndex++
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    Text(
                        if (currentQuestionIndex < questions.size - 1) "Next" else "Done",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
