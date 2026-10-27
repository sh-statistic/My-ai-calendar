package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Calming, elegant Soft Light Palette
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

// Minimalist Crystal Night / Dark Palette (iOS & Telegram Inspired)
private val SoftDarkColorScheme = darkColorScheme(
    primary = SolarAmberCrystalPrimary,
    onPrimary = Color(0xFF000000),
    primaryContainer = SolarAmberCrystalContainer,
    onPrimaryContainer = SolarAmberCrystalText,

    secondary = PersianTealCrystalAccent,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF132F2C),
    onSecondaryContainer = Color(0xFFCCFBF1),

    tertiary = PersianBlueCrystalAccent,
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF10283C),
    onTertiaryContainer = Color(0xFFE0F2FE),

    background = CrystalDarkBg,
    onBackground = CrystalPureWhite,

    surface = CrystalDarkSurface,
    onSurface = CrystalPureWhite,
    surfaceVariant = CrystalDarkSurfaceVariant,
    onSurfaceVariant = CrystalMutedText,

    outline = CrystalDarkBorder,
    outlineVariant = Color(0xFF24242A),

    error = Color(0xFFF87171),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF381617),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SoftDarkColorScheme else SoftLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
