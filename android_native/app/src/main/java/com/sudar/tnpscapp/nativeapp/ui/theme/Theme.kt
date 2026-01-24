package com.sudar.tnpscapp.nativeapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light color scheme matching Flutter's AppColors
 */
private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = AppColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AppColors.PrimaryLight,
    onPrimaryContainer = AppColors.PrimaryDark,
    
    // Secondary colors
    secondary = AppColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = AppColors.SecondaryLight,
    onSecondaryContainer = AppColors.SecondaryDark,
    
    // Tertiary colors (using secondary variants)
    tertiary = AppColors.Info,
    onTertiary = Color.White,
    
    // Background colors
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    
    // Surface colors
    surface = AppColors.CardBackground,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceLight,
    onSurfaceVariant = AppColors.TextSecondary,
    
    // Container colors
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = AppColors.SurfaceLight,
    surfaceContainer = AppColors.Background,
    surfaceContainerHigh = AppColors.BorderLight,
    surfaceContainerHighest = AppColors.Border,
    
    // Error colors
    error = AppColors.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    // Outline colors
    outline = AppColors.Border,
    outlineVariant = AppColors.BorderLight,
    
    // Inverse colors
    inverseSurface = AppColors.TextPrimary,
    inverseOnSurface = Color.White,
    inversePrimary = AppColors.PrimaryLight,
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.5f)
)

// Dark mode removed - Flutter app uses light mode only

/**
 * SUDAR Theme matching Flutter app's design
 * 
 * Features:
 * - Adda247-style Pink/Red primary
 * - Blue secondary for actions
 * - Light mode only (matching Flutter)
 * - Poppins typography
 */
@Composable
fun SudarTheme(
    content: @Composable () -> Unit,
) {
    // Force light mode only - matching Flutter app
    val colorScheme = LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Status bar: Match background color with white icons
            window.statusBarColor = AppColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            
            // Navigation bar: Dark background with white icons
            window.navigationBarColor = AppColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
