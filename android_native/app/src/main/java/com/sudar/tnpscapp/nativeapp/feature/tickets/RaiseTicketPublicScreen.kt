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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaiseTicketPublicScreen(
    prefillMobile: String = "",
    onBack: () -> Unit = {},
    viewModel: RaiseTicketPublicViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(prefillMobile) {
        viewModel.prefillMobileIfEmpty(prefillMobile)
    }

    if (uiState.createdTicketId != null) {
        TicketCreatedSuccess(
            ticketId = uiState.createdTicketId ?: 0,
            onDone = {
                viewModel.clearCreated()
                onBack()
            }
        )
        return
    }

    val categories = listOf(
        "general" to "General",
        "login" to "Login / OTP",
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
                        Icons.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            "Need help?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Raise a ticket and our team will reply in Admin.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = uiState.userName,
                onValueChange = viewModel::setUserName,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Your name (optional)") },
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

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.userMobile,
                onValueChange = viewModel::setUserMobile,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Mobile number") },
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

            Spacer(modifier = Modifier.height(16.dp))

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
                        label = { Text(label, fontSize = 12.sp) },
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.subject,
                onValueChange = viewModel::setSubject,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Subject") },
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

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.message,
                onValueChange = viewModel::setMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                placeholder = { Text("Describe your issue clearly (min 10 characters)") },
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

            Spacer(modifier = Modifier.height(16.dp))

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

@Composable
private fun TicketCreatedSuccess(ticketId: Int, onDone: () -> Unit) {
    Scaffold(
        containerColor = AppColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2ECC71),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Ticket submitted",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your Ticket ID is #$ticketId.\nWe will reply soon.",
                fontSize = 13.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

