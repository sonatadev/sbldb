package com.github.sonatadev.sbldb.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme

/**
 * Line drawings of the cable attachments on a 40 × 20 grid, as SVG path data with an optional
 * stroke width (default [ATTACHMENT_STROKE]). Every one hangs from the same ring at the top.
 * Drafted and checked as SVG before being copied here.
 */
internal const val ATTACHMENT_STROKE = 1.4f

internal val attachmentDrawings: Map<String, List<Pair<String, Float>>> = mapOf(
    "D-Handle" to listOf(
        "M20 1 a2 2 0 1 0 0.01 0" to ATTACHMENT_STROKE,
        "M20 5 C13 5 12.5 10 12.5 15" to ATTACHMENT_STROKE,
        "M20 5 C27 5 27.5 10 27.5 15" to ATTACHMENT_STROKE,
        "M13.5 15.5 H26.5" to 3f
    ),
    "Rope" to listOf(
        "M20 1 a2 2 0 1 0 0.01 0" to ATTACHMENT_STROKE,
        "M20 5 V7.5" to ATTACHMENT_STROKE,
        "M19.5 7.5 C17 10.5 14.5 12 13 14.5" to ATTACHMENT_STROKE,
        "M20.5 7.5 C23 10.5 25.5 12 27 14.5" to ATTACHMENT_STROKE,
        "M12.8 16.2 a1.5 1.5 0 1 0 0.01 0" to 2.2f,
        "M27.2 16.2 a1.5 1.5 0 1 0 0.01 0" to 2.2f
    ),
    "Straight Bar" to listOf(
        "M20 1 a2 2 0 1 0 0.01 0" to ATTACHMENT_STROKE,
        "M20 5 V10.5" to ATTACHMENT_STROKE,
        "M5 11.5 H35" to 2.2f,
        "M8.5 9.3 V13.7" to ATTACHMENT_STROKE,
        "M31.5 9.3 V13.7" to ATTACHMENT_STROKE
    ),
    "EZ Bar" to listOf(
        "M20 1 a2 2 0 1 0 0.01 0" to ATTACHMENT_STROKE,
        "M20 5 V10.5" to ATTACHMENT_STROKE,
        "M5 11.5 H10 L12.5 8.8 L15 11.5 H25 L27.5 8.8 L30 11.5 H35" to 2.2f
    ),
    "V-Bar" to listOf(
        "M20 1 a2 2 0 1 0 0.01 0" to ATTACHMENT_STROKE,
        "M20 5 V7" to ATTACHMENT_STROKE,
        "M20 7 L16.5 12" to ATTACHMENT_STROKE,
        "M20 7 L23.5 12" to ATTACHMENT_STROKE,
        "M16.5 12 L14.8 16.5" to 3f,
        "M23.5 12 L25.2 16.5" to 3f
    ),
    "Wide Bar" to listOf(
        "M20 1 a2 2 0 1 0 0.01 0" to ATTACHMENT_STROKE,
        "M20 5 V9.5" to ATTACHMENT_STROKE,
        "M3.5 15.5 L7.5 10 H32.5 L36.5 15.5" to 2.2f
    ),
    "Neutral-Grip Wide Bar" to listOf(
        "M20 1 a2 2 0 1 0 0.01 0" to ATTACHMENT_STROKE,
        "M20 5 V9.5" to ATTACHMENT_STROKE,
        "M5 10 H35" to 2.2f,
        "M10 10 V16.5" to 2.6f,
        "M30 10 V16.5" to 2.6f
    ),
    "Ankle Strap" to listOf(
        "M20 1 a2 2 0 1 0 0.01 0" to ATTACHMENT_STROKE,
        "M20 5 V7.5" to ATTACHMENT_STROKE,
        "M17.5 7.5 H22.5" to ATTACHMENT_STROKE,
        "M12 12.5 C12 9 28 9 28 12.5 C28 16 12 16 12 12.5 Z" to 2.2f
    ),
    "Forearm Strap" to listOf(
        "M20 1 a2 2 0 1 0 0.01 0" to ATTACHMENT_STROKE,
        "M20 5 V7.5" to ATTACHMENT_STROKE,
        "M10.5 9.5 C15 8 25 8 29.5 9.5 V15.5 C25 14 15 14 10.5 15.5 Z" to 2f
    )
)

/** For attachments added by the user: a plain carabiner. */
internal val customAttachmentDrawing: List<Pair<String, Float>> = listOf(
    "M20 1 a2 2 0 1 0 0.01 0" to ATTACHMENT_STROKE,
    "M20 5 V7" to ATTACHMENT_STROKE,
    "M16.8 10 C16.8 6.8 23.2 6.8 23.2 10 V13 C23.2 16.8 16.8 16.8 16.8 13 Z" to 2f
)

/** The attachment's drawing in [color], scaled to fit (it is twice as wide as tall). */
@Composable
fun AttachmentIcon(attachment: String, modifier: Modifier = Modifier, color: Color = SbldbTheme.colors.ink) {
    val paths = remember(attachment) {
        (attachmentDrawings[attachment] ?: customAttachmentDrawing).map { (d, width) -> PathParser().parsePathString(d).toPath() to width }
    }
    Canvas(modifier) {
        val k = minOf(size.width / 40f, size.height / 20f)
        withTransform({
            translate((size.width - 40f * k) / 2, (size.height - 20f * k) / 2)
            scale(k, k, pivot = Offset.Zero)
        }) {
            paths.forEach { (path, width) ->
                drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
