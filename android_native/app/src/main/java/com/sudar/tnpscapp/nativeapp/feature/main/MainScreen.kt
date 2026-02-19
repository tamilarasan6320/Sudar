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
import com.sudar.tnpscapp.nativeapp.feature.home.HomeScreen
import com.sudar.tnpscapp.nativeapp.feature.home.HomeViewModel
import com.sudar.tnpscapp.nativeapp.feature.tests.TestCategoriesScreen
import com.sudar.tnpscapp.nativeapp.feature.tests.TestCategoriesViewModel
import com.sudar.tnpscapp.nativeapp.feature.profile.ProfileScreen
import com.sudar.tnpscapp.nativeapp.feature.profile.ProfileViewModel
import com.sudar.tnpscapp.nativeapp.feature.progress.ProgressScreen
import com.sudar.tnpscapp.nativeapp.feature.progress.ProgressViewModel
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
    onMyTickets: () -> Unit = {},
    onRaiseTicket: () -> Unit = {},
    onAccountDeletion: () -> Unit = {},
    onSubscription: () -> Unit = {},
    onLogout: () -> Unit = {},
    onStartTest: (sessionId: Int, title: String, categoryName: String, duration: Int, totalQuestions: Int) -> Unit = { _, _, _, _, _ -> }
) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val selectedTab = MainTab.values()[selectedTabIndex]
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableStateOf(0L) }
    
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

    Scaffold(
        containerColor = AppColors.Background,
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
                        onHistory = onNavigateToHistory
                    )
                }
                MainTab.TESTS -> {
                    val viewModel: TestCategoriesViewModel = hiltViewModel()
                    TestCategoriesScreen(
                        viewModel = viewModel,
                        onBack = { selectedTabIndex = MainTab.HOME.ordinal },
                        onOpenCategory = { categoryId ->
                            // Tests flow is handled inside TestCategoriesScreen
                            // It shows categories then sessions inline
                        },
                        onStartTest = onStartTest
                    )
                }
                MainTab.PROGRESS -> {
                    val viewModel: ProgressViewModel = hiltViewModel()
                    ProgressScreen(
                        viewModel = viewModel
                    )
                }
                MainTab.PROFILE -> {
                    val viewModel: ProfileViewModel = hiltViewModel()
                    ProfileScreen(
                        viewModel = viewModel,
                        onEditProfile = onEditProfile,
                        onTestHistory = onTestHistory,
                        onPerformance = { selectedTabIndex = MainTab.PROGRESS.ordinal },
                        onAbout = onAbout,
                        onPrivacyPolicy = onPrivacyPolicy,
                        onMyTickets = onMyTickets,
                        onRaiseTicket = onRaiseTicket,
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
        shadowElevation = 16.dp,
        color = AppColors.CardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
