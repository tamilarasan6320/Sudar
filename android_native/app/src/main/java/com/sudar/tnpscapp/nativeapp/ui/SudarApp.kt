package com.sudar.tnpscapp.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.sudar.tnpscapp.nativeapp.navigation.AppNavGraph
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors
import com.sudar.tnpscapp.nativeapp.ui.theme.SudarTheme

@Composable
fun SudarApp() {
    SudarTheme {
        // Global dark background wrapper - ensures consistent dark theme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
        ) {
            val navController = rememberNavController()
            AppNavGraph(navController = navController)
        }
    }
}

