package com.sudar.tnpscapp.nativeapp.feature.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun AboutScreen(
    viewModel: AboutViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAboutData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "About",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        containerColor = AppColors.Background
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // App Logo - Styled box with gradient
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AppColors.Primary, AppColors.PrimaryDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // App Name
                Text(
                    text = "SUDAR",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = uiState.appName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Version Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AppColors.Primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Version ${uiState.appVersion}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.Primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Description
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AppColors.CardBackground
                ) {
                    Text(
                        text = uiState.description,
                        fontSize = 16.sp,
                        color = AppColors.TextSecondary,
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                // Contact Information
                if (uiState.contactEmail.isNotEmpty() || uiState.contactPhone.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(30.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AppColors.CardBackground
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Contact Us",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (uiState.contactEmail.isNotEmpty()) {
                                ContactItem(
                                    icon = Icons.Outlined.Email,
                                    label = "Email",
                                    value = uiState.contactEmail
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (uiState.contactPhone.isNotEmpty()) {
                                ContactItem(
                                    icon = Icons.Outlined.Phone,
                                    label = "Phone",
                                    value = uiState.contactPhone
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Copyright
                Text(
                    text = "© 2025 ${uiState.appName}",
                    fontSize = 12.sp,
                    color = AppColors.TextLight
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ContactItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AppColors.Primary.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )
        }
    }
}
