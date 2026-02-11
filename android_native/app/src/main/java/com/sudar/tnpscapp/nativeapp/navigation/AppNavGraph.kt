package com.sudar.tnpscapp.nativeapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sudar.tnpscapp.nativeapp.feature.auth.LoginScreen
import com.sudar.tnpscapp.nativeapp.feature.auth.OtpScreen
import com.sudar.tnpscapp.nativeapp.feature.auth.ProfileSetupScreen
import com.sudar.tnpscapp.nativeapp.feature.exam.ExamSelectionScreen
import com.sudar.tnpscapp.nativeapp.feature.main.MainScreen
import com.sudar.tnpscapp.nativeapp.feature.profile.EditProfileScreen
import com.sudar.tnpscapp.nativeapp.feature.splash.SplashDestination
import com.sudar.tnpscapp.nativeapp.feature.splash.SplashScreen
import com.sudar.tnpscapp.nativeapp.feature.startup.StartupScreen
import com.sudar.tnpscapp.nativeapp.feature.subscription.NewSubscriptionOfferScreen
import com.sudar.tnpscapp.nativeapp.feature.subscription.SubscriptionOfferScreen
import com.sudar.tnpscapp.nativeapp.feature.subscription.SubscriptionScreen
import com.sudar.tnpscapp.nativeapp.feature.subscription.SubscriptionPaymentType
import com.sudar.tnpscapp.nativeapp.feature.subscription.SubscriptionPaymentTypeViewModel
import com.sudar.tnpscapp.nativeapp.feature.support.AboutScreen
import com.sudar.tnpscapp.nativeapp.feature.support.AccountDeletionScreen
import com.sudar.tnpscapp.nativeapp.feature.support.FeedbackScreen
import com.sudar.tnpscapp.nativeapp.feature.support.HelpFaqScreen
import com.sudar.tnpscapp.nativeapp.feature.support.PrivacyPolicyScreen
import com.sudar.tnpscapp.nativeapp.feature.progress.PerformanceScreen
import com.sudar.tnpscapp.nativeapp.feature.settings.LanguageSettingsScreen
import com.sudar.tnpscapp.nativeapp.feature.settings.NotificationsSettingsScreen
import com.sudar.tnpscapp.nativeapp.feature.tests.SavedTestsScreen
import com.sudar.tnpscapp.nativeapp.feature.tests.SessionsScreen
import com.sudar.tnpscapp.nativeapp.feature.tests.TakeTestScreen
import com.sudar.tnpscapp.nativeapp.feature.tests.TestCategoriesScreen
import com.sudar.tnpscapp.nativeapp.feature.tests.TestHistoryScreen
import com.sudar.tnpscapp.nativeapp.feature.tests.TestIntroScreen
import com.sudar.tnpscapp.nativeapp.feature.tests.TestResultHolder
import com.sudar.tnpscapp.nativeapp.feature.tests.TestResultsScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Startup,
    ) {
        // ==================== SPLASH ====================
        composable(Routes.Splash) {
            SplashScreen(
                onNavigate = { destination ->
                    when (destination) {
                        SplashDestination.Login -> {
                            navController.navigate(Routes.Login) {
                                popUpTo(Routes.Splash) { inclusive = true }
                            }
                        }
                        SplashDestination.ExamSelection -> {
                            navController.navigate(Routes.ExamSelection) {
                                popUpTo(Routes.Splash) { inclusive = true }
                            }
                        }
                        SplashDestination.SubscriptionOffer -> {
                            navController.navigate(Routes.subscriptionOffer("startup")) {
                                popUpTo(Routes.Splash) { inclusive = true }
                            }
                        }
                        is SplashDestination.Home -> {
                            navController.navigate(Routes.Main) {
                                popUpTo(Routes.Splash) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        // ==================== STARTUP ====================
        composable(Routes.Startup) {
            StartupScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.Startup) { inclusive = true }
                    }
                },
            )
        }

        // ==================== LOGIN FLOW ====================
        composable(Routes.Login) {
            LoginScreen(
                onNavigateToOtp = { mobile ->
                    navController.navigate(Routes.otp(mobile))
                },
                onTruecallerSuccess = { isNewUser, mobile, token ->
                    if (isNewUser) {
                        navController.navigate(Routes.profileSetup(mobile, token ?: "", "truecaller")) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.Main) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    }
                },
                onLoginSuccess = {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.Otp,
            arguments = listOf(navArgument("mobile") { type = NavType.StringType }),
        ) { backStackEntry ->
            val mobile = backStackEntry.arguments?.getString("mobile").orEmpty()
            OtpScreen(
                phoneNumber = mobile,
                onBack = { navController.popBackStack() },
                onVerified = { isNewUser, verifiedMobile, token ->
                    if (isNewUser) {
                        navController.navigate(Routes.profileSetup(verifiedMobile, token ?: "", "otp")) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    }
                },
                onExistingUser = {
                    // Go back to startup to check exam selection
                    navController.navigate(Routes.Startup) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.ProfileSetup,
            arguments = listOf(
                navArgument("mobile") { type = NavType.StringType },
                navArgument("token") { type = NavType.StringType; defaultValue = "" },
                navArgument("method") { type = NavType.StringType; defaultValue = "otp" }
            ),
        ) { backStackEntry ->
            val mobile = backStackEntry.arguments?.getString("mobile").orEmpty()
            val token = backStackEntry.arguments?.getString("token").orEmpty()
            val method = backStackEntry.arguments?.getString("method").orEmpty()
            ProfileSetupScreen(
                mobileNumber = mobile,
                isNewUser = true,
                token = token.ifEmpty { null },
                verificationMethod = method.ifEmpty { "otp" },
                onProfileComplete = {
                    navController.navigate(Routes.ExamSelection) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ExamSelection) {
            ExamSelectionScreen(
                onExamSelected = {
                    // Go back to startup to check premium status
                    navController.navigate(Routes.Startup) {
                        popUpTo(Routes.ExamSelection) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.SubscriptionOffer,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = "startup" }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "startup"
            val paymentTypeViewModel: SubscriptionPaymentTypeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val paymentTypeState by paymentTypeViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                paymentTypeViewModel.load()
            }

            val onSkip: () -> Unit = {
                if (mode == "gate") {
                    navController.popBackStack()
                    Unit
                } else {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.SubscriptionOffer) { inclusive = true }
                    }
                }
            }

            val onSubscribed: () -> Unit = {
                if (mode == "gate") {
                    navController.previousBackStackEntry?.savedStateHandle?.set("subscribed", true)
                    navController.popBackStack()
                    Unit
                } else {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.SubscriptionOffer) { inclusive = true }
                    }
                }
            }

            if (paymentTypeState.isLoading) {
                // Lightweight loader (avoid flashing wrong screen)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (paymentTypeState.paymentType) {
                    SubscriptionPaymentType.TrialFee2NonRefundable -> {
                        NewSubscriptionOfferScreen(
                            onSkip = onSkip,
                            onSubscribed = onSubscribed
                        )
                    }
                    SubscriptionPaymentType.LegacyMandate5Refunded -> {
                        SubscriptionOfferScreen(
                            onSkip = onSkip,
                            onSubscribed = onSubscribed
                        )
                    }
                }
            }
        }

        composable(Routes.Subscription) {
            SubscriptionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ==================== MAIN APP (with bottom nav) ====================
        composable(Routes.Main) {
            MainScreen(
                onNavigateToTests = { navController.navigate(Routes.TestCategories) },
                onNavigateToHistory = { navController.navigate(Routes.TestHistory) },
                onChangeExam = { navController.navigate(Routes.ExamSelection) },
                onEditProfile = { navController.navigate(Routes.EditProfile) },
                onTestHistory = { navController.navigate(Routes.TestHistory) },
                onPerformance = { /* Performance is shown in Progress tab */ },
                onAbout = { navController.navigate(Routes.About) },
                onPrivacyPolicy = { navController.navigate(Routes.PrivacyPolicy) },
                onHelpFaq = { navController.navigate(Routes.HelpFaq) },
                onFeedback = { navController.navigate(Routes.Feedback) },
                onAccountDeletion = { navController.navigate(Routes.AccountDeletion) },
                onSubscription = { navController.navigate(Routes.Subscription) },
                onLogout = {
                    navController.navigate(Routes.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onStartTest = { sessionId, title, categoryName, duration, totalQuestions ->
                    navController.navigate(Routes.testIntro(sessionId, title, categoryName, duration, totalQuestions))
                }
            )
        }

        // ==================== PROFILE SCREENS ====================
        composable(Routes.EditProfile) {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // ==================== SUPPORT SCREENS ====================
        composable(Routes.About) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Feedback) {
            FeedbackScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HelpFaq) {
            HelpFaqScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PrivacyPolicy) {
            PrivacyPolicyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AccountDeletion) {
            AccountDeletionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ==================== TESTS FLOW ====================
        composable(Routes.TestCategories) {
            TestCategoriesScreen(
                onBack = { navController.popBackStack() },
                onOpenCategory = { categoryId ->
                    navController.navigate(Routes.sessions(categoryId))
                },
            )
        }

        composable(
            route = Routes.Sessions,
            arguments = listOf(navArgument("categoryId") { type = NavType.IntType }),
        ) { entry ->
            val categoryId = entry.arguments?.getInt("categoryId") ?: 0
            SessionsScreen(
                categoryId = categoryId,
                onBack = { navController.popBackStack() },
                onStart = { sessionId, title, categoryName, duration, totalQuestions ->
                    navController.navigate(Routes.testIntro(sessionId, title, categoryName, duration, totalQuestions))
                },
            )
        }

        // Test Intro Screen - shown before starting the test
        composable(
            route = Routes.TestIntro,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.IntType },
                navArgument("title") { type = NavType.StringType; defaultValue = "Test" },
                navArgument("category") { type = NavType.StringType; defaultValue = "" },
                navArgument("duration") { type = NavType.IntType; defaultValue = 60 },
                navArgument("questions") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { entry ->
            val sessionId = entry.arguments?.getInt("sessionId") ?: 0
            val title = entry.arguments?.getString("title").orEmpty()
            val category = entry.arguments?.getString("category").orEmpty()
            val duration = entry.arguments?.getInt("duration") ?: 60
            val questions = entry.arguments?.getInt("questions") ?: 0
            
            // Check if user just subscribed (coming back from subscription offer)
            val subscribedFlag = entry.savedStateHandle.get<Boolean>("subscribed") == true
            
            // Auto-start test if user just subscribed
            LaunchedEffect(subscribedFlag) {
                if (subscribedFlag) {
                    // Clear the flag
                    entry.savedStateHandle.remove<Boolean>("subscribed")
                    // Auto-navigate to test
                    navController.navigate(Routes.takeTest(sessionId, title, category, duration)) {
                        popUpTo(Routes.TestIntro) { inclusive = true }
                    }
                }
            }
            
            TestIntroScreen(
                testTitle = if (title.isBlank()) "Test" else title,
                categoryName = category,
                examName = "TNPSC",
                sessionId = sessionId,
                totalQuestions = questions,
                duration = duration,
                onBack = { navController.popBackStack() },
                onStartTest = {
                    navController.navigate(Routes.takeTest(sessionId, title, category, duration)) {
                        popUpTo(Routes.TestIntro) { inclusive = true }
                    }
                },
                onSubscriptionNeeded = {
                    // Navigate to subscription offer in gate mode
                    // When user skips or completes, they return here and premium status is re-checked
                    navController.navigate(Routes.subscriptionOffer("gate"))
                }
            )
        }

        composable(
            route = Routes.TakeTest,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.IntType },
                navArgument("title") { type = NavType.StringType; defaultValue = "Test" },
                navArgument("category") { type = NavType.StringType; defaultValue = "" },
                navArgument("duration") { type = NavType.IntType; defaultValue = 60 },
            ),
        ) { entry ->
            val sessionId = entry.arguments?.getInt("sessionId") ?: 0
            val title = entry.arguments?.getString("title").orEmpty()
            val category = entry.arguments?.getString("category").orEmpty()
            val duration = entry.arguments?.getInt("duration") ?: 60
            TakeTestScreen(
                sessionId = sessionId,
                testTitle = if (title.isBlank()) "Test" else title,
                categoryName = category,
                durationMinutes = duration,
                onExit = { navController.popBackStack() },
                onSubmitted = { resultData ->
                    // Store result data in holder for the results screen
                    TestResultHolder.set(resultData)
                    navController.navigate(Routes.TestResults) {
                        popUpTo(Routes.TakeTest) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.TestHistory) {
            TestHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TestResults) {
            val resultData = TestResultHolder.get()
            if (resultData != null) {
                TestResultsScreen(
                    resultData = resultData,
                    onBack = {
                        TestResultHolder.clear()
                        navController.popBackStack()
                    },
                )
            } else {
                // Fallback if no data - go back
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        // ==================== SETTINGS SCREENS ====================
        composable(Routes.NotificationsSettings) {
            NotificationsSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.LanguageSettings) {
            LanguageSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ==================== PERFORMANCE SCREEN ====================
        composable(Routes.Performance) {
            PerformanceScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ==================== SAVED TESTS SCREEN ====================
        composable(Routes.SavedTests) {
            SavedTestsScreen(
                onBack = { navController.popBackStack() },
                onStartTest = { sessionId, title, categoryName, duration, totalQuestions ->
                    navController.navigate(Routes.testIntro(sessionId, title, categoryName, duration, totalQuestions))
                }
            )
        }
    }
}
