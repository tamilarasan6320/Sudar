package com.sudar.tnpscapp.nativeapp.feature.main

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudar.tnpscapp.nativeapp.feature.home.HomeScreen
import com.sudar.tnpscapp.nativeapp.feature.home.HomeViewModel
import com.sudar.tnpscapp.nativeapp.feature.tests.TestCategoriesScreen
import com.sudar.tnpscapp.nativeapp.feature.tests.TestCategoriesViewModel
import com.sudar.tnpscapp.nativeapp.feature.profile.ProfileScreen
import com.sudar.tnpscapp.nativeapp.feature.profile.ProfileViewModel
import com.sudar.tnpscapp.nativeapp.feature.progress.ProgressScreen
import com.sudar.tnpscapp.nativeapp.feature.progress.ProgressViewModel
import com.sudar.tnpscapp.nativeapp.feature.update.UpdateViewModel
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

/**
 * Navigation tabs
 */
enum class MainTab(
    val icon: ImageVector,
    val label: String
) {
    HOME(Icons.Default.Home, "Home"),
    TESTS(Icons.Default.LibraryBooks, "Tests"),
    PROGRESS(Icons.Default.BarChart, "Progress"),
    PROFILE(Icons.Default.Person, "Profile")
}

/**
 * Main screen with bottom navigation
 * Contains Home, Tests, Progress, and Profile tabs
 */
@Composable
fun MainScreen(
    onNavigateToTests: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onChangeExam: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onTestHistory: () -> Unit = {},
    onPerformance: () -> Unit = {},
    onAbout: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onHelpFaq: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onAccountDeletion: () -> Unit = {},
    onSubscription: () -> Unit = {},
    onLogout: () -> Unit = {},
    onStartTest: (sessionId: Int, title: String, categoryName: String, duration: Int, totalQuestions: Int) -> Unit = { _, _, _, _, _ -> },
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val selectedTab = MainTab.values()[selectedTabIndex]
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableStateOf(0L) }
    var pendingOpenTestCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingOpenTestCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Update check state
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Check for updates on first composition (like Flutter's UpdateService().checkForUpdate(context))
    LaunchedEffect(Unit) {
        (context as? Activity)?.let { activity ->
            updateViewModel.check(activity)
        }
    }
    
    // Show snackbar when flexible update is downloaded
    LaunchedEffect(updateState.downloaded) {
        if (updateState.downloaded) {
            val result = snackbarHostState.showSnackbar(
                message = "Update downloaded! Restart to install.",
                actionLabel = "RESTART",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                updateViewModel.completeFlexibleUpdate()
            } else {
                updateViewModel.dismissDownloadedPrompt()
            }
        }
    }
    
    // Handle back press - double tap to exit
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Update Available Dialog (fallback when immediate update not allowed)
    if (updateState.showFallbackDialog) {
        UpdateAvailableDialog(
            onDismiss = { updateViewModel.dismissDialog() },
            onUpdateNow = {
                (context as? Activity)?.let { activity ->
                    updateViewModel.startFlexible(activity)
                }
            },
            onOpenPlayStore = { updateViewModel.openPlayStore() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppColors.Background,
        // Bottom bar already applies navigation bar insets. Avoid double bottom inset padding
        // which creates a blank black strip above the bottom navigation.
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top
        ),
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTabIndex = it.ordinal }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                MainTab.HOME -> {
                    val viewModel: HomeViewModel = hiltViewModel()
                    HomeScreen(
                        viewModel = viewModel,
                        onChangeExam = onChangeExam,
                        onTests = { 
                            selectedTabIndex = MainTab.TESTS.ordinal 
                        },
                        onOpenTestCategory = { categoryId, categoryName ->
                            pendingOpenTestCategoryId = categoryId
                            pendingOpenTestCategoryName = categoryName
                            selectedTabIndex = MainTab.TESTS.ordinal
                        },
                        onHistory = onNavigateToHistory
                    )
                }
                MainTab.TESTS -> {
                    val viewModel: TestCategoriesViewModel = hiltViewModel()
                    TestCategoriesScreen(
                        viewModel = viewModel,
                        onBack = { selectedTabIndex = MainTab.HOME.ordinal },
                        onOpenCategory = { _ -> /* unused (inline selection in this tab) */ },
                        inBottomNav = true,
                        openCategoryId = pendingOpenTestCategoryId,
                        openCategoryName = pendingOpenTestCategoryName,
                        onOpenCategoryConsumed = {
                            pendingOpenTestCategoryId = null
                            pendingOpenTestCategoryName = null
                        },
                        onStartTest = onStartTest
                    )
                }
                MainTab.PROGRESS -> {
                    val viewModel: ProgressViewModel = hiltViewModel()
                    ProgressScreen(
                        inBottomNav = true,
                        viewModel = viewModel
                    )
                }
                MainTab.PROFILE -> {
                    val viewModel: ProfileViewModel = hiltViewModel()
                    ProfileScreen(
                        inBottomNav = true,
                        viewModel = viewModel,
                        onEditProfile = onEditProfile,
                        onTestHistory = onTestHistory,
                        onPerformance = { selectedTabIndex = MainTab.PROGRESS.ordinal },
                        onAbout = onAbout,
                        onPrivacyPolicy = onPrivacyPolicy,
                        onHelpFaq = onHelpFaq,
                        onFeedback = onFeedback,
                        onAccountDeletion = onAccountDeletion,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        // Remove heavy shadow (it creates a dark overlay above the nav bar)
        shadowElevation = 0.dp,
        color = AppColors.CardBackground
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = AppColors.BorderLight
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Prevent overlap with system navigation bar (3-button / gesture nav)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MainTab.values().forEach { tab ->
                    BottomNavItem(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        onClick = { onTabSelected(tab) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        AppColors.Primary.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }
    
    val contentColor = if (isSelected) {
        AppColors.Primary
    } else {
        AppColors.TextLight
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tab.label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor
        )
    }
}

/**
 * Update Available Dialog - matches Flutter's update dialog style
 */
@Composable
private fun UpdateAvailableDialog(
    onDismiss: () -> Unit,
    onUpdateNow: () -> Unit,
    onOpenPlayStore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = AppColors.CardBackground,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Update Available!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "A new version of Sudar TNPSC is available.",
                    fontSize = 15.sp,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Background)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "✨ Update now for new features and improvements!",
                        fontSize = 13.sp,
                        color = AppColors.TextLight
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "LATER",
                    color = AppColors.TextLight
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onOpenPlayStore) {
                    Text(
                        text = "PLAY STORE",
                        color = Color(0xFF4CAF50)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onUpdateNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "UPDATE NOW",
                        color = Color.White
                    )
                }
            }
        }
    )
}
