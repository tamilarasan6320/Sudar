package com.sudar.tnpscapp.nativeapp.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onEditProfile: () -> Unit = {},
    onTestHistory: () -> Unit = {},
    onPerformance: () -> Unit = {},
    onAbout: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onHelpFaq: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onAccountDeletion: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                ),
                actions = {
                    TextButton(onClick = { viewModel.openWhatsAppSupport() }) {
                        Text(
                            "Support",
                            color = AppColors.Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
        },
        containerColor = AppColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Header
            ProfileHeader(
                userName = uiState.userName,
                userMobile = uiState.userMobile,
                isPremium = uiState.isPremium,
                testsTaken = uiState.testsTaken,
                rank = uiState.rank,
                avgScore = uiState.avgScore,
                isLoading = uiState.isLoadingStats
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Exam Preparation Section
            ProfileSection(
                title = "Exam Preparation",
                items = listOf(
                    ProfileMenuItem(
                        icon = Icons.Outlined.History,
                        title = "Test History",
                        subtitle = "View all attempted tests",
                        onClick = onTestHistory
                    ),
                    ProfileMenuItem(
                        icon = Icons.Outlined.TrendingUp,
                        title = "Performance Analytics",
                        subtitle = "Detailed performance insights",
                        onClick = onPerformance
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account Settings Section
            ProfileSection(
                title = "Account Settings",
                items = listOf(
                    ProfileMenuItem(
                        icon = Icons.Outlined.Person,
                        title = "Edit Profile",
                        subtitle = "Update your information",
                        onClick = onEditProfile
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Support Section
            ProfileSection(
                title = "Support",
                items = listOf(
                    ProfileMenuItem(
                        icon = Icons.Outlined.Help,
                        title = "Help & FAQ",
                        subtitle = "Get help with the app",
                        onClick = onHelpFaq
                    ),
                    ProfileMenuItem(
                        icon = Icons.Outlined.Feedback,
                        title = "Send Feedback",
                        subtitle = "Share your thoughts",
                        onClick = onFeedback
                    ),
                    ProfileMenuItem(
                        icon = Icons.Outlined.Info,
                        title = "About",
                        subtitle = "App information",
                        onClick = onAbout
                    ),
                    ProfileMenuItem(
                        icon = Icons.Outlined.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "View our privacy policy",
                        onClick = onPrivacyPolicy
                    ),
                    ProfileMenuItem(
                        icon = Icons.Outlined.DeleteForever,
                        title = "Delete Account",
                        subtitle = "Request account deletion",
                        onClick = onAccountDeletion
                    )
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.Error
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(listOf(AppColors.Error, AppColors.Error))
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    tint = AppColors.Error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Logout",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = AppColors.CardBackground,
            title = { Text("Logout", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary) },
            text = { Text("Are you sure you want to logout?", color = AppColors.TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error)
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = AppColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
    userMobile: String,
    isPremium: Boolean,
    testsTaken: Int,
    rank: Int,
    avgScore: Double,
    isLoading: Boolean
) {
    Surface(
        color = AppColors.CardBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AppColors.Primary, AppColors.PrimaryDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (userName.isNotEmpty()) userName.first().uppercase() else "U",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name with Premium Badge
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = userName.ifEmpty { "User" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                if (isPremium) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        modifier = Modifier.background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                            ),
                            RoundedCornerShape(12.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "PRO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Mobile
            Text(
                text = userMobile.ifEmpty { "No mobile number" },
                fontSize = 14.sp,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileStat(
                    value = if (isLoading) "..." else "$testsTaken",
                    label = "Tests"
                )
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp),
                    color = AppColors.Border
                )
                ProfileStat(
                    value = if (isLoading) "..." else if (rank > 0) "#$rank" else "--",
                    label = "Rank"
                )
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp),
                    color = AppColors.Border
                )
                ProfileStat(
                    value = if (isLoading) "..." else "${avgScore.toInt()}%",
                    label = "Avg Score"
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    items: List<ProfileMenuItem>
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.CardBackground,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            items.forEachIndexed { index, item ->
                ProfileMenuItemRow(item)
                if (index < items.lastIndex) {
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = AppColors.Border.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuItemRow(item: ProfileMenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AppColors.Primary.copy(alpha = 0.15f),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                fontSize = 12.sp,
                color = AppColors.TextLight
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextLight,
            modifier = Modifier.size(16.dp)
        )
    }
}

data class ProfileMenuItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)
