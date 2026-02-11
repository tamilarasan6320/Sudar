package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sudar.tnpscapp.nativeapp.ui.components.RichHtmlContent
import com.sudar.tnpscapp.nativeapp.ui.components.containsHtmlImage
import com.sudar.tnpscapp.nativeapp.ui.components.containsLatex
import com.sudar.tnpscapp.nativeapp.ui.components.convertLatexToPlainText
import com.sudar.tnpscapp.nativeapp.ui.components.cleanHtmlText
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeTestScreen(
    sessionId: Int,
    testTitle: String,
    categoryName: String,
    durationMinutes: Int,
    onExit: () -> Unit,
    onSubmitted: (TestResultData) -> Unit,
    viewModel: TakeTestViewModel = hiltViewModel(),
) {
    val showSubmitDialog = remember { mutableStateOf(false) }
    val showQuestionGrid = remember { mutableStateOf(false) }

    LaunchedEffect(sessionId, durationMinutes) {
        viewModel.start(sessionId, durationMinutes, testTitle, categoryName)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TakeTestEvent.Submitted -> onSubmitted(event.resultData)
            }
        }
    }

    val uiState = viewModel.uiState

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = testTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (categoryName.isNotBlank()) {
                            Text(
                                text = categoryName,
                                fontSize = 12.sp,
                                color = AppColors.TextSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showSubmitDialog.value = false; onExit() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                actions = {
                    // Question Grid Button
                    IconButton(onClick = { showQuestionGrid.value = true }) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "Question List",
                            tint = AppColors.Primary
                        )
                    }
                    
                    // Timer Badge
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (uiState.timeRemainingSec < 5 * 60)
                                    AppColors.Error.copy(alpha = 0.1f)
                                else
                                    AppColors.Primary.copy(alpha = 0.1f)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (uiState.timeRemainingSec < 5 * 60) AppColors.Error else AppColors.Primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatTime(uiState.timeRemainingSec),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.timeRemainingSec < 5 * 60) AppColors.Error else AppColors.Primary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            }

            !uiState.error.isNullOrBlank() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = uiState.error, color = AppColors.Error)
                }
            }

            else -> {
                val q = uiState.currentQuestion!!
                val progress = (uiState.currentIndex + 1).toFloat() / uiState.questions.size.toFloat()
                val isLast = uiState.currentIndex == uiState.questions.size - 1
                val selectedIndex = uiState.selectedAnswers[q.id]
                val optionsEn = q.optionsEn()
                val optionsTa = q.optionsTa()
                val isMarked = uiState.markedForReview.contains(q.id)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = AppColors.Primary,
                        trackColor = AppColors.Border
                    )

                    // Scrollable Content Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                    ) {
                        // Question Number Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppColors.Primary)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Question ${uiState.currentIndex + 1}/${uiState.questions.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Question Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // English Version
                                if (q.hasEnglish == true && !q.questionEn.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF2196F3).copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "English",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2196F3)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // Use RichHtmlContent for HTML with images
                                    RichHtmlContent(
                                        htmlString = q.questionEn,
                                        fontSize = 16,
                                        fontWeight = FontWeight.Medium,
                                        textColor = AppColors.TextPrimary,
                                        lineHeight = 24
                                    )
                                    if (q.hasTamil == true && !q.questionTa.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }

                                // Tamil Version
                                if (q.hasTamil == true && !q.questionTa.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFF9800).copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "தமிழ்",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFF9800)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // Use RichHtmlContent for HTML with images
                                    RichHtmlContent(
                                        htmlString = q.questionTa,
                                        fontSize = 16,
                                        fontWeight = FontWeight.Medium,
                                        textColor = AppColors.TextPrimary,
                                        lineHeight = 24
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Options
                        (0..3).forEach { idx ->
                            val isSelected = selectedIndex == idx
                            val optionLabel = ('A'.code + idx).toChar()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) AppColors.Primary.copy(alpha = 0.1f)
                                        else AppColors.CardBackground
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) AppColors.Primary else AppColors.Border,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.selectOption(idx) }
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Radio Circle
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = 2.dp,
                                                color = if (isSelected) AppColors.Primary else AppColors.Border,
                                                shape = CircleShape
                                            )
                                            .background(
                                                if (isSelected) AppColors.Primary else Color.Transparent
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Option Text
                                    Column(modifier = Modifier.weight(1f)) {
                                        // English Option
                                        if (q.hasEnglish == true && optionsEn[idx].isNotBlank()) {
                                            val optionText = optionsEn[idx]
                                            val textColor = if (isSelected) AppColors.Primary else AppColors.TextPrimary
                                            val fontWt = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            
                                            // Check if option contains an image
                                            if (containsHtmlImage(optionText)) {
                                                // Show label separately, then render image with RichHtmlContent
                                                Text(
                                                    text = "$optionLabel)",
                                                    fontSize = 15.sp,
                                                    fontWeight = fontWt,
                                                    color = textColor,
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                RichHtmlContent(
                                                    htmlString = optionText,
                                                    fontSize = 15,
                                                    fontWeight = fontWt,
                                                    textColor = textColor,
                                                    lineHeight = 20
                                                )
                                            } else {
                                                // Standard text rendering (LaTeX or plain)
                                                val displayText = if (containsLatex(optionText)) {
                                                    "$optionLabel) ${convertLatexToPlainText(optionText)}"
                                                } else {
                                                    "$optionLabel) ${cleanHtmlText(optionText)}"
                                                }
                                                Text(
                                                    text = displayText,
                                                    fontSize = 15.sp,
                                                    fontWeight = fontWt,
                                                    color = textColor,
                                                )
                                            }
                                        }
                                        // Tamil Option
                                        if (q.hasTamil == true && optionsTa[idx].isNotBlank()) {
                                            if (q.hasEnglish == true && optionsEn[idx].isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }
                                            val tamilLabels = listOf("அ", "ஆ", "இ", "ஈ")
                                            val optionText = optionsTa[idx]
                                            val textColor = if (isSelected) AppColors.Primary else AppColors.TextPrimary
                                            val fontWt = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            
                                            // Check if Tamil option contains an image
                                            if (containsHtmlImage(optionText)) {
                                                // Show label separately, then render image with RichHtmlContent
                                                Text(
                                                    text = "${tamilLabels[idx]})",
                                                    fontSize = 15.sp,
                                                    fontWeight = fontWt,
                                                    color = textColor,
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                RichHtmlContent(
                                                    htmlString = optionText,
                                                    fontSize = 15,
                                                    fontWeight = fontWt,
                                                    textColor = textColor,
                                                    lineHeight = 20
                                                )
                                            } else {
                                                // Standard text rendering (LaTeX or plain)
                                                val displayText = if (containsLatex(optionText)) {
                                                    "${tamilLabels[idx]}) ${convertLatexToPlainText(optionText)}"
                                                } else {
                                                    "${tamilLabels[idx]}) ${cleanHtmlText(optionText)}"
                                                }
                                                Text(
                                                    text = displayText,
                                                    fontSize = 15.sp,
                                                    fontWeight = fontWt,
                                                    color = textColor,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Extra space at the bottom for smooth scrolling
                        Spacer(modifier = Modifier.height(80.dp))
                    }

                    // Bottom Navigation Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppColors.CardBackground,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Previous Button
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = viewModel::previous,
                                enabled = uiState.currentIndex > 0 && !uiState.isSubmitting,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AppColors.Primary
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Previous",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Mark for Review Button
                            OutlinedButton(
                                onClick = viewModel::toggleMarkForReview,
                                enabled = !uiState.isSubmitting,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isMarked) AppColors.Warning.copy(alpha = 0.1f) else Color.Transparent,
                                    contentColor = if (isMarked) AppColors.Warning else AppColors.TextSecondary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (isMarked) "Unmark Review" else "Mark for Review",
                                    tint = if (isMarked) AppColors.Warning else AppColors.TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Next/Submit Button
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (isLast) showSubmitDialog.value = true else viewModel.next()
                                },
                                enabled = !uiState.isSubmitting,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Primary,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                if (uiState.isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (isLast) "Submit" else "Next",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isLast) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubmitDialog.value) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog.value = false },
            title = { Text("Submit test?", color = AppColors.TextPrimary) },
            text = { Text("Are you sure you want to submit your test now?", color = AppColors.TextSecondary) },
            containerColor = AppColors.CardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog.value = false
                        viewModel.submit(sessionId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog.value = false }) {
                    Text("Cancel", color = AppColors.TextSecondary)
                }
            },
        )
    }
    
    // Question Number Grid Dialog
    if (showQuestionGrid.value) {
        AlertDialog(
            onDismissRequest = { showQuestionGrid.value = false },
            containerColor = AppColors.CardBackground,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Questions",
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    IconButton(onClick = { showQuestionGrid.value = false }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AppColors.TextSecondary
                        )
                    }
                }
            },
            text = {
                Column {
                    // Legend
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem(color = AppColors.Success, label = "Answered")
                        LegendItem(color = AppColors.Warning, label = "Marked")
                        LegendItem(color = AppColors.TextLight, label = "Not Answered")
                    }
                    
                    // Question Grid - 5 columns - SCROLLABLE
                    val questions = uiState.questions
                    val chunkedQuestions = questions.chunked(5)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunkedQuestions.forEachIndexed { rowIndex, rowQuestions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowQuestions.forEachIndexed { colIndex, question ->
                                    val questionIndex = rowIndex * 5 + colIndex
                                    val questionId = question.id
                                    val isAnswered = uiState.selectedAnswers.containsKey(questionId)
                                    val isMarked = uiState.markedForReview.contains(questionId)
                                    val isCurrent = questionIndex == uiState.currentIndex
                                    
                                    val bgColor = when {
                                        isCurrent -> AppColors.Primary
                                        isMarked -> AppColors.Warning
                                        isAnswered -> AppColors.Success
                                        else -> AppColors.DarkSurface
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bgColor)
                                            .border(
                                                width = if (isCurrent) 2.dp else 0.dp,
                                                color = if (isCurrent) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                viewModel.goToQuestion(questionIndex)
                                                showQuestionGrid.value = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${questionIndex + 1}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = when {
                                                isCurrent || isAnswered || isMarked -> Color.White
                                                else -> AppColors.TextSecondary
                                            }
                                        )
                                    }
                                }
                                // Fill remaining space if row is incomplete
                                repeat(5 - rowQuestions.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQuestionGrid.value = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = AppColors.TextSecondary
        )
    }
}

private fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
