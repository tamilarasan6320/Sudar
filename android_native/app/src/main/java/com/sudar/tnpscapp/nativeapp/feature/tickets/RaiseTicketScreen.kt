package com.sudar.tnpscapp.nativeapp.feature.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaiseTicketScreen(
    onBack: () -> Unit = {},
    onCreated: (Int) -> Unit = {},
    viewModel: RaiseTicketViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.createdTicketId) {
        val id = uiState.createdTicketId
        if (id != null && id > 0) {
            viewModel.clearCreated()
            onCreated(id)
        }
    }

    val categories = listOf(
        "general" to "General",
        "bug" to "Bug / Error",
        "test" to "Test Issue",
        "subscription" to "Subscription",
        "payment" to "Payment",
        "account" to "Account",
        "feature" to "Feature Request",
        "complaint" to "Complaint",
        "suggestion" to "Suggestion",
    )

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Raise Ticket",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(AppColors.Primary, AppColors.PrimaryLight)
                            )
                        )
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Feedback,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            "Tell us what you need",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "We will review your ticket and respond soon.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Category",
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories) { (key, label) ->
                    FilterChip(
                        selected = uiState.category == key,
                        onClick = { viewModel.setCategory(key) },
                        label = {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (uiState.category == key) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Primary.copy(alpha = 0.16f),
                            selectedLabelColor = AppColors.Primary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = uiState.category == key,
                            borderColor = AppColors.Border,
                            selectedBorderColor = AppColors.Primary.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Subject",
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = uiState.subject,
                onValueChange = viewModel::setSubject,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Example: Payment success but premium not active") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary,
                    unfocusedBorderColor = AppColors.Border,
                    focusedContainerColor = AppColors.DarkSurface,
                    unfocusedContainerColor = AppColors.DarkSurface,
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    cursorColor = AppColors.Primary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Message",
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = uiState.message,
                onValueChange = viewModel::setMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                placeholder = {
                    Text(
                        "Describe your issue clearly.\n\nInclude:\n- What happened\n- Steps to reproduce\n- Your mobile number (if needed)",
                        fontSize = 13.sp,
                        color = AppColors.TextSecondary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary,
                    unfocusedBorderColor = AppColors.Border,
                    focusedContainerColor = AppColors.DarkSurface,
                    unfocusedContainerColor = AppColors.DarkSurface,
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    cursorColor = AppColors.Primary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = uiState.error ?: "",
                    color = AppColors.Error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary,
                    disabledContainerColor = AppColors.Primary.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Submit Ticket",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

