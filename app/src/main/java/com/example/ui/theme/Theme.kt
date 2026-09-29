package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Calming, elegant Soft Light Palette (No dark mode, no harsh pure white #FFFFFF)
private val SoftLightColorScheme = lightColorScheme(
    primary = SolarAmberPrimary,
    onPrimary = WarmCreamBg,
    primaryContainer = SolarAmberPastel,
    onPrimaryContainer = SolarAmberDeep,

    secondary = PersianTealAccent,
    onSecondary = WarmCreamBg,
    secondaryContainer = PersianTealPastel,
    onSecondaryContainer = Color(0xFF115E59),

    tertiary = PersianBlueAccent,
    onTertiary = WarmCreamBg,
    tertiaryContainer = PersianBluePastel,
    onTertiaryContainer = Color(0xFF0369A1),

    background = WarmCreamBg,
    onBackground = GentleDarkGrayText,

    surface = WarmCreamSurface,
    onSurface = GentleDarkGrayText,
    surfaceVariant = WarmCreamSurfaceVariant,
    onSurfaceVariant = GentleMutedText,

    outline = WarmCreamBorder,
    outlineVariant = Color(0xFFE7E2D6),

    error = SoftError,
    onError = WarmCreamBg,
    errorContainer = SoftErrorContainer,
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Strictly soft light mode as required
    dynamicColor: Boolean = false, // Consistent eye-friendly warm cream branding
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SoftLightColorScheme,
        typography = Typography,
        content = content
    )
}
