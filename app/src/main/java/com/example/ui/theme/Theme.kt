package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.model.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = DarkBackground,
    primaryContainer = SurfaceCard,
    onPrimaryContainer = TextPrimary,
    secondary = EmeraldConnected,
    onSecondary = DarkBackground,
    tertiary = PurpleAi,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    error = RedCritical
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.White,
    secondary = EmeraldConnected,
    tertiary = PurpleAi,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A)
)

private val NightDrivingColorScheme = darkColorScheme(
    primary = NightDrivingPrimary,
    onPrimary = Color.White,
    secondary = NightDrivingSecondary,
    onSecondary = Color.Black,
    tertiary = CyanPrimary,
    background = NightDrivingBackground,
    onBackground = NightDrivingOnBackground,
    surface = NightDrivingSurface,
    onSurface = NightDrivingOnBackground,
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = NightDrivingOnBackground,
    error = NightDrivingPrimary
)

@Composable
fun ThaiCarOBDTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.NIGHT_DRIVING -> NightDrivingColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = (appTheme == AppTheme.LIGHT)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
