package com.sudar.tnpscapp.nativeapp.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

/**
 * Notifications Settings Screen - Flutter replica
 * Allows users to manage notification preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(
    onBack: () -> Unit = {}
) {
    var testReminders by remember { mutableStateOf(true) }
    var performanceUpdates by remember { mutableStateOf(false) }
    var newContent by remember { mutableStateOf(true) }
    var dailyReports by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
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
                text = "Manage your notification preferences",
                fontSize = 14.sp,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Notification Options Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.CardBackground,
                shadowElevation = 2.dp
            ) {
                Column {
                    NotificationTile(
                        icon = Icons.Outlined.Quiz,
                        title = "Test Reminders",
                        subtitle = "Get notified about upcoming tests",
                        checked = testReminders,
                        onCheckedChange = { testReminders = it }
                    )

                    HorizontalDivider()

                    NotificationTile(
                        icon = Icons.Outlined.TrendingUp,
                        title = "Performance Updates",
                        subtitle = "Receive performance insights",
                        checked = performanceUpdates,
                        onCheckedChange = { performanceUpdates = it }
                    )

                    HorizontalDivider()

                    NotificationTile(
                        icon = Icons.Outlined.NewReleases,
                        title = "New Content",
                        subtitle = "Notify when new tests are added",
                        checked = newContent,
                        onCheckedChange = { newContent = it }
                    )

                    HorizontalDivider()

                    NotificationTile(
                        icon = Icons.Outlined.Assessment,
                        title = "Daily Reports",
                        subtitle = "Get daily progress summaries",
                        checked = dailyReports,
                        onCheckedChange = { dailyReports = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = AppColors.TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = AppColors.Border
            )
        )
    }
}
