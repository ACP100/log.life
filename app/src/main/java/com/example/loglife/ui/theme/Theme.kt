package com.example.loglife.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.loglife.data.settings.ThemeMode

private fun colorSchemeFor(p: LogPalette, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = p.ink, onPrimary = p.background,
        secondary = p.inkSecondary, tertiary = p.accent,
        background = p.background, onBackground = p.ink,
        surface = p.background, onSurface = p.ink,
        surfaceVariant = p.surface, onSurfaceVariant = p.inkSecondary,
        surfaceContainerLow = p.surface, surfaceContainer = p.surface, surfaceContainerHigh = p.surface,
        outline = p.sheetBorder, outlineVariant = p.sheetDivider, error = p.accent,
    )
} else {
    lightColorScheme(
        primary = p.ink, onPrimary = p.surface,
        secondary = p.inkSecondary, tertiary = p.accent,
        background = p.background, onBackground = p.ink,
        surface = p.background, onSurface = p.ink,
        surfaceVariant = p.surface, onSurfaceVariant = p.inkSecondary,
        surfaceContainerLow = p.surface, surfaceContainer = p.surface, surfaceContainerHigh = p.surface,
        outline = p.sheetBorder, outlineVariant = p.sheetDivider, error = p.accent,
    )
}

@Composable
fun LoglifeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (dark) DarkPalette else LightPalette
    // Written before any child reads it, so no backwards write.
    if (currentPalette != palette) currentPalette = palette

    MaterialTheme(
        colorScheme = colorSchemeFor(palette, dark),
        typography = Typography,
        content = content
    )
}
