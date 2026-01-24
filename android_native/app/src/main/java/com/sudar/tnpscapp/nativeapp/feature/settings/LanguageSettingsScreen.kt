package com.sudar.tnpscapp.nativeapp.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Language Settings Screen - Flutter replica
 * Allows users to select their preferred language (English/Tamil)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit = {}
) {
    var selectedLanguage by remember { mutableStateOf("en") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Language Settings",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Select your preferred language",
                fontSize = 14.sp,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Language Options Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.CardBackground,
                shadowElevation = 2.dp
            ) {
                Column {
                    LanguageOption(
                        flag = "🇺🇸",
                        name = "English",
                        nativeName = "English",
                        isSelected = selectedLanguage == "en",
                        onClick = {
                            selectedLanguage = "en"
                            scope.launch {
                                snackbarHostState.showSnackbar("Language set to English")
                                delay(500)
                                onBack()
                            }
                        }
                    )

                    HorizontalDivider()

                    LanguageOption(
                        flag = "🇮🇳",
                        name = "Tamil",
                        nativeName = "தமிழ்",
                        isSelected = selectedLanguage == "ta",
                        onClick = {
                            selectedLanguage = "ta"
                            scope.launch {
                                snackbarHostState.showSnackbar("மொழி தமிழ் என அமைக்கப்பட்டது")
                                delay(500)
                                onBack()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    flag: String,
    name: String,
    nativeName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = flag,
            fontSize = 32.sp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = nativeName,
                fontSize = 14.sp,
                color = AppColors.TextSecondary
            )
        }

        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (isSelected) "Selected" else "Not selected",
            tint = if (isSelected) AppColors.Primary else AppColors.TextLight,
            modifier = Modifier.size(24.dp)
        )
    }
}
