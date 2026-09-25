package com.github.sonatadev.sbldb.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.github.sonatadev.sbldb.domain.ActionAnimation
import com.github.sonatadev.sbldb.domain.FigurePose
import com.github.sonatadev.sbldb.domain.Figure
import com.github.sonatadev.sbldb.domain.P
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import kotlin.math.hypot
import kotlin.math.max

/**
 * Dot-matrix stick figure performing a joint action: the body is drawn in dots, the moving
 * segments in the accent, swinging from the start to the end of the range and back.
 */
@Composable
fun JointFigure(animation: ActionAnimation, description: String, modifier: Modifier = Modifier) {
    val colors = SbldbTheme.colors
    val progress by rememberInfiniteTransition(label = "joint").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "angle"
    )
    Canvas(modifier.semantics { contentDescription = description }) {
        val value = animation.from + (animation.to - animation.from) * progress
        val figure = FigurePose.pose(animation, value)
        // Figure space -1.15..1.15 fitted into the canvas, centred; a fixed grid of dots lit by the body
        val scale = minOf(size.width, size.height) / 2.3f * figure.zoom
        val step = size.height / GRID_ROWS
        val dot = step * 0.34f
        val cols = (size.width / step).toInt()
        val x0 = (size.width - (cols - 1) * step) / 2
        val y0 = step / 2
        for (row in 0 until GRID_ROWS) {
            for (col in 0 until cols) {
                val px = x0 + col * step
                val py = y0 + row * step
                val fx = (px - size.width / 2) / scale + figure.focus.x
                val fy = (py - size.height / 2) / scale + figure.focus.y
                val lit = litBy(figure, fx, fy, step / scale)
                val color = when (lit) {
                    Lit.MOVING -> colors.accent
                    Lit.BODY -> colors.ink
                    Lit.GHOST -> colors.muted
                    Lit.NONE -> colors.empty
                }
                drawCircle(color, if (lit == Lit.NONE) dot * 0.55f else dot, Offset(px, py))
            }
        }
    }
}

private const val GRID_ROWS = 44

private enum class Lit { NONE, GHOST, BODY, MOVING }

/** What lights the grid dot at (x, y); moving parts win over the rest of the body. */
private fun litBy(figure: Figure, x: Float, y: Float, cell: Float): Lit {
    var result = Lit.NONE
    val slack = cell * 0.5f
    val headDist = hypot(x - figure.head.x, y - figure.head.y)
    if (figure.headOccludes && headDist < figure.headRadius - cell * 1.1f) return Lit.NONE
    if (headDist <= figure.headRadius + slack * 0.3f && headDist >= figure.headRadius - cell * 1.1f) {
        result = if (figure.headMoving) Lit.MOVING else Lit.BODY
    }
    figure.nose?.let { if (hypot(x - it.x, y - it.y) <= cell * 0.75f) result = maxOf(result, if (figure.headMoving) Lit.MOVING else Lit.BODY) }
    for (bone in figure.bones) {
        if (distance(x, y, bone.a, bone.b) <= max(bone.width, slack)) {
            val lit = when {
                bone.moving -> Lit.MOVING
                bone.ghost -> Lit.GHOST
                else -> Lit.BODY
            }
            if (lit > result) result = lit
            if (result == Lit.MOVING) return result
        }
    }
    return result
}

private fun distance(x: Float, y: Float, a: P, b: P): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len2 = dx * dx + dy * dy
    val t = if (len2 == 0f) 0f else (((x - a.x) * dx + (y - a.y) * dy) / len2).coerceIn(0f, 1f)
    return hypot(x - (a.x + t * dx), y - (a.y + t * dy))
}

