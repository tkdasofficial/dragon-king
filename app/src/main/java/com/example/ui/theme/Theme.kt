package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DragonKingColorScheme = darkColorScheme(
    primary = DragonGold,
    onPrimary = DragonObsidian,
    secondary = DragonCrimson,
    onSecondary = DragonIvory,
    tertiary = DragonAmber,
    background = DragonObsidian,
    onBackground = DragonIvory,
    surface = DragonSurface,
    onSurface = DragonIvory,
    surfaceVariant = ColorTokens.PanelDark,
    onSurfaceVariant = DragonMuted
)

private object ColorTokens {
    val PanelDark = androidx.compose.ui.graphics.Color(0xCC1A1122)
}

@Composable
fun DragonKingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DragonKingColorScheme,
        typography = Typography,
        content = content
    )
}
