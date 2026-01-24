package com.sudar.tnpscapp.nativeapp.feature.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

@Composable
fun StartupScreen(
    onNavigate: (String) -> Unit,
    viewModel: StartupViewModel = hiltViewModel(),
) {
    val destination = viewModel.destination.collectAsStateWithLifecycle().value

    LaunchedEffect(destination) {
        if (!destination.isNullOrBlank()) onNavigate(destination)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AppColors.Primary)
    }
}

