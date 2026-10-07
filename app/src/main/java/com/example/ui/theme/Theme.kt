package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyanPrimary,
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004E5B),
    onPrimaryContainer = JarvisCyanLight,

    secondary = JarvisAmberAccent,
    onSecondary = Color(0xFF432C00),
    secondaryContainer = Color(0xFF5F4100),
    onSecondaryContainer = JarvisGoldGlow,

    tertiary = JarvisSuccessGreen,
    onTertiary = Color(0xFF003820),

    background = JarvisBackgroundDark,
    onBackground = JarvisTextPrimary,

    surface = JarvisSurfaceDark,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceVariantDark,
    onSurfaceVariant = JarvisTextSecondary,

    outline = JarvisCyanDark,
    outlineVariant = JarvisBorderCyan,

    error = JarvisErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve dedicated cybernetic Jarvis branding
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = Typography,
        content = content
    )
}
