package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ShadowNeonCyan,
    secondary = ShadowNeonPurple,
    tertiary = ShadowNeonPink,
    background = ShadowDarkBackground,
    surface = ShadowCard,
    onBackground = ShadowTextPrimary,
    onSurface = ShadowTextPrimary,
    primaryContainer = ShadowCard,
    onPrimaryContainer = ShadowNeonCyan,
    secondaryContainer = ShadowCardSelected,
    onSecondaryContainer = ShadowTextPrimary,
    error = ShadowNeonPink
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for the Shadow AI aesthetic
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our crafted Neon look
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
