package com.eagleeye.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberGreen,
    onPrimary = BackgroundDark,
    primaryContainer = CardDark,
    onPrimaryContainer = CyberGreen,
    secondary = CyberBlue,
    onSecondary = BackgroundDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = CyberBlue,
    tertiary = CyberOrange,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    error = CyberRed,
    onError = BackgroundDark,
    outline = CardBorderDark
)

@Composable
fun EagleEyeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
