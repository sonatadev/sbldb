package com.github.sonatadev.sbldb.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.sonatadev.sbldb.ui.theme.SbldbType

/**
 * Big dot-matrix text. Doto draws ":" as a cross-like glyph, so times ("2:00", "42:17") get a
 * colon made of two dots the same size as the font's own.
 */
@Composable
fun HeroText(text: String, size: Int, color: Color, modifier: Modifier = Modifier) {
    if (':' !in text) {
        Text(text, style = SbldbType.hero(size), color = color, modifier = modifier)
        return
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        text.split(':').forEachIndexed { i, part ->
            if (i > 0) DotColon(size, color)
            Text(part, style = SbldbType.hero(size), color = color)
        }
    }
}

@Composable
private fun DotColon(size: Int, color: Color) {
    Canvas(Modifier.width((size * 0.3f).dp).height((size * 0.95f).dp)) {
        val dot = size * 0.1f * density
        val x = (this.size.width - dot) / 2
        for (fraction in listOf(TOP_DOT, BOTTOM_DOT)) {
            drawRoundRect(
                color = color,
                topLeft = Offset(x, this.size.height * fraction - dot / 2),
                size = Size(dot, dot),
                cornerRadius = CornerRadius(dot * 0.2f)
            )
        }
    }
}

// Vertical centres of the two dots, as a fraction of the line height, lined up with the digits
private const val TOP_DOT = 0.42f
private const val BOTTOM_DOT = 0.74f
