package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ImmersiveColorScheme = darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    background = ImmersiveBackground,
    onBackground = ImmersiveTextPrimary,
    surface = ImmersiveSurface,
    onSurface = ImmersiveTextPrimary,
    surfaceVariant = ImmersiveSurfaceVariant,
    onSurfaceVariant = ImmersiveTextSecondary,
    error = ImmersiveError,
    outline = ImmersiveOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for Immersive UI
    dynamicColor: Boolean = false, // Disable dynamic colors
    content: @Composable () -> Unit,
) {
    val colorScheme = ImmersiveColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
