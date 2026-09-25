package com.github.sonatadev.sbldb.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.github.sonatadev.sbldb.domain.AccentColor
import com.github.sonatadev.sbldb.domain.ThemeMode

@Composable
fun SbldbTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentColor = AccentColor.ORANGE,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = remember(dark, accent) { sbldbColors(dark, accent) }

    CompositionLocalProvider(LocalSbldbColors provides colors) {
        MaterialTheme(colorScheme = materialScheme(colors), typography = Typography, content = content)
    }
}

/** Maps the tokens onto Material roles so dialogs, menus and text fields follow the theme. */
private fun materialScheme(c: SbldbColors) = (if (c.isDark) darkColorScheme() else lightColorScheme()).copy(
    primary = c.accent,
    onPrimary = c.onAccent,
    primaryContainer = c.accentTint,
    onPrimaryContainer = c.ink,
    secondary = c.ink,
    onSecondary = c.ground,
    tertiary = c.accent,
    onTertiary = c.onAccent,
    background = c.ground,
    onBackground = c.ink,
    surface = c.ground,
    onSurface = c.ink,
    surfaceVariant = c.module,
    onSurfaceVariant = c.muted,
    surfaceContainerLowest = c.module,
    surfaceContainerLow = c.module,
    surfaceContainer = c.module,
    surfaceContainerHigh = c.module,
    surfaceContainerHighest = c.module,
    surfaceBright = c.module,
    surfaceDim = c.ground,
    outline = c.edge,
    outlineVariant = c.line,
    scrim = Color.Black.copy(alpha = 0.5f)
)

/** Access to the theme tokens: `SbldbTheme.colors.accent`. */
object SbldbTheme {
    val colors: SbldbColors
        @Composable @ReadOnlyComposable get() = LocalSbldbColors.current
}
