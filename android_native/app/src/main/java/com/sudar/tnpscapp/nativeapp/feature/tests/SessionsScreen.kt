package com.sudar.tnpscapp.nativeapp.feature.tests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    categoryId: Int,
    onBack: () -> Unit,
    onStart: (sessionId: Int, title: String, categoryName: String, durationMinutes: Int, totalQuestions: Int) -> Unit,
    viewModel: SessionsViewModel = hiltViewModel(),
) {
    LaunchedEffect(categoryId) {
        viewModel.load(categoryId)
    }

    val uiState = viewModel.uiState

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tests",
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

            uiState.sessions.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No tests available", color = AppColors.TextSecondary)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    items(uiState.sessions) { s ->
                        val title = s.name ?: "Test"
                        val categoryName = s.categoryName ?: ""
                        val duration = s.duration ?: 60
                        val totalQuestions = s.actualQuestionCount ?: s.totalQuestions ?: 0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    if (totalQuestions > 0) {
                                        onStart(s.id, title, categoryName, duration, totalQuestions)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.TextPrimary
                                )
                                if (!s.description.isNullOrBlank()) {
                                    Text(
                                        modifier = Modifier.padding(top = 4.dp),
                                        text = s.description,
                                        fontSize = 14.sp,
                                        color = AppColors.TextSecondary,
                                    )
                                }
                                Row(
                                    modifier = Modifier.padding(top = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Questions: $totalQuestions",
                                        fontSize = 12.sp,
                                        color = AppColors.TextLight,
                                    )
                                    Text(
                                        modifier = Modifier.padding(start = 12.dp),
                                        text = "Duration: ${duration}m",
                                        fontSize = 12.sp,
                                        color = AppColors.TextLight,
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}
