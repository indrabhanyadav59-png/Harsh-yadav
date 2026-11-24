package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val IndraDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBg,
    primaryContainer = NavyCardElevated,
    onPrimaryContainer = TextPrimary,
    secondary = CrimsonAccent,
    onSecondary = TextPrimary,
    secondaryContainer = CrimsonDark,
    onSecondaryContainer = TextPrimary,
    tertiary = GoldCoin,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = NavyCard,
    onSurface = TextPrimary,
    surfaceVariant = NavyCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderGlow,
    error = DangerRed,
    onError = TextPrimary
)

@Composable
fun IndraAnimeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IndraDarkColorScheme,
        typography = IndraTypography,
        content = content
    )
}
