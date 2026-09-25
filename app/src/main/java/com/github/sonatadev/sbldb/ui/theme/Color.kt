package com.github.sonatadev.sbldb.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.github.sonatadev.sbldb.domain.AccentColor

/** Design tokens of the "D+ Dot Matrix" theme. Everything highlighted uses [accent]. */
@Immutable
data class SbldbColors(
    val isDark: Boolean,
    /** Screen background. */
    val ground: Color,
    /** Module (panel) background. */
    val module: Color,
    /** Module border. */
    val edge: Color,
    /** Hairline between rows. */
    val line: Color,
    val ink: Color,
    val muted: Color,
    val dim: Color,
    /** Empty dots and meter segments. */
    val empty: Color,
    val accent: Color,
    val accentTint: Color,
    val onAccent: Color
)

fun AccentColor.color(dark: Boolean): Color = when (this) {
    AccentColor.ORANGE -> if (dark) Color(0xFFFF6A13) else Color(0xFFD94E00)
    AccentColor.RED -> if (dark) Color(0xFFF04438) else Color(0xFFD92D20)
    AccentColor.YELLOW -> if (dark) Color(0xFFFFC700) else Color(0xFF9E7400)
    AccentColor.GREEN -> if (dark) Color(0xFF4ADE80) else Color(0xFF15803D)
    AccentColor.CYAN -> if (dark) Color(0xFF22D3EE) else Color(0xFF0E7C99)
    AccentColor.BLUE -> if (dark) Color(0xFF4D7CFF) else Color(0xFF2D5BF0)
    AccentColor.VIOLET -> if (dark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
}

fun sbldbColors(dark: Boolean, accent: AccentColor): SbldbColors {
    val accentColor = accent.color(dark)
    return if (dark) {
        SbldbColors(
            isDark = true,
            ground = Color(0xFF000000),
            module = Color(0xFF0D0D0D),
            edge = Color(0xFF222222),
            line = Color(0xFF1A1A1A),
            ink = Color(0xFFFFFFFF),
            muted = Color(0xFF8A8A8A),
            dim = Color(0xFF5C5C5C),
            empty = Color(0xFF2A2A2A),
            accent = accentColor,
            accentTint = accentColor.copy(alpha = 0.12f),
            onAccent = readableOn(accentColor)
        )
    } else {
        SbldbColors(
            isDark = false,
            ground = Color(0xFFEFEFEC),
            module = Color(0xFFFFFFFF),
            edge = Color(0xFFE2E2DD),
            line = Color(0xFFEDEDE9),
            ink = Color(0xFF0A0A0A),
            muted = Color(0xFF6B6B6B),
            dim = Color(0xFF9A9A95),
            empty = Color(0xFFD6D6D1),
            accent = accentColor,
            accentTint = accentColor.copy(alpha = 0.12f),
            onAccent = readableOn(accentColor)
        )
    }
}

/** WCAG contrast ratio between two opaque colors. */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return maxOf(la, lb) / minOf(la, lb)
}

/** Black or white, whichever reads better on [background]. */
fun readableOn(background: Color): Color =
    if (contrastRatio(Color.Black, background) >= contrastRatio(Color.White, background)) Color.Black else Color.White

val LocalSbldbColors = staticCompositionLocalOf { sbldbColors(dark = false, accent = AccentColor.ORANGE) }
