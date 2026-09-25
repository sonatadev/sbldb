package com.github.sonatadev.sbldb.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.sonatadev.sbldb.domain.VolumeCalculator
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme

enum class DotFill { EMPTY, HALF, FULL }

data class Dot(val fill: DotFill, val inZone: Boolean)

/**
 * One dot per set, up to [count]. Dots from the [zoneFrom]-th onward sit in the reference zone
 * and are drawn with the accent; half sets (secondary muscles) get a half dot.
 */
fun volumeDots(
    sets: Double,
    count: Int = VolumeCalculator.OPTIMAL_MAX_SETS.toInt(),
    zoneFrom: Int = VolumeCalculator.OPTIMAL_MIN_SETS.toInt()
): List<Dot> = List(count) { i ->
    val remaining = sets - i
    Dot(
        fill = when {
            remaining >= 1 -> DotFill.FULL
            remaining >= 0.5 -> DotFill.HALF
            else -> DotFill.EMPTY
        },
        inZone = i >= zoneFrom - 1
    )
}

/** Segments lit on the 5-segment effort meter, and whether the set counts toward volume. */
data class Effort(val filled: Int, val counts: Boolean)

fun effortFor(rir: Int?): Effort = when {
    rir == null -> Effort(0, true)
    rir > VolumeCalculator.MAX_HARD_SET_RIR -> Effort(1, false)
    else -> Effort((5 - rir).coerceIn(0, 5), true)
}

/** Volume as a row of dots with the 10–20 zone behind it. */
@Composable
fun DotRow(sets: Double, modifier: Modifier = Modifier, dotSize: Dp = 6.5.dp, gap: Dp = 3.5.dp) {
    val colors = SbldbTheme.colors
    val dots = volumeDots(sets)
    val zoneStart = dots.indexOfFirst { it.inZone }
    Canvas(
        modifier
            .width(dotSize * dots.size + gap * (dots.size - 1))
            .height(dotSize * 2.3f)
            .semantics { contentDescription = "${com.github.sonatadev.sbldb.ui.formatSets(sets)} sets" }
    ) {
        val d = dotSize.toPx()
        val g = gap.toPx()
        val cy = size.height / 2
        if (zoneStart >= 0) {
            val left = zoneStart * (d + g) - g / 2
            drawRoundRect(
                color = colors.accentTint,
                topLeft = Offset(left, 0f),
                size = Size(size.width - left + g / 2, size.height),
                cornerRadius = CornerRadius(size.height / 2)
            )
        }
        dots.forEachIndexed { i, dot ->
            val center = Offset(i * (d + g) + d / 2, cy)
            val ink = if (dot.inZone) colors.accent else colors.ink
            drawCircle(if (dot.fill == DotFill.FULL) ink else colors.empty, d / 2, center)
            if (dot.fill == DotFill.HALF) {
                drawArc(ink, 90f, 180f, true, Offset(center.x - d / 2, center.y - d / 2), Size(d, d))
            }
        }
    }
}

/** Session sets as a dot grid: done = accent, current = pulsing accent, pending = outline. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DotMatrix(total: Int, done: Int, modifier: Modifier = Modifier, columns: Int = 5, dotSize: Dp = 12.dp) {
    val colors = SbldbTheme.colors
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse"
    )
    FlowRow(
        modifier = modifier.width(dotSize * columns + 7.dp * (columns - 1)),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        maxItemsInEachRow = columns
    ) {
        repeat(total) { i ->
            when {
                i < done -> Box(Modifier.size(dotSize).background(colors.accent, CircleShape))
                i == done -> Box(Modifier.size(dotSize).alpha(pulse).background(colors.accent, CircleShape))
                else -> Box(Modifier.size(dotSize).border(1.dp, colors.edge, CircleShape))
            }
        }
    }
}

/** Five segments; more lit = closer to failure. Grey when the set is too easy to count. */
@Composable
fun EffortMeter(rir: Int?, modifier: Modifier = Modifier, outlineOnly: Boolean = false) {
    val colors = SbldbTheme.colors
    val effort = effortFor(rir)
    val lit = if (effort.counts) colors.accent else colors.dim
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { i ->
            val shape = RoundedCornerShape(2.dp)
            val segment = Modifier.width(7.dp).height(14.dp)
            when {
                i >= effort.filled -> Box(segment.background(colors.empty, shape))
                outlineOnly -> Box(segment.border(1.dp, lit, shape))
                else -> Box(segment.background(lit, shape))
            }
        }
    }
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = 6.dp) {
    Box(modifier.size(size).background(color, CircleShape))
}

/** 1–5 rating as five dots, lit in the accent. */
@Composable
fun RatingDots(rating: Int, modifier: Modifier = Modifier, dotSize: Dp = 7.dp) {
    val colors = SbldbTheme.colors
    Row(
        modifier.semantics { contentDescription = "$rating out of 5" },
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(5) { i ->
            Box(Modifier.size(dotSize).background(if (i < rating) colors.accent else colors.empty, CircleShape))
        }
    }
}

/** Five tappable rating dots; each dot has a full-size touch target. */
@Composable
fun RatingPicker(rating: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = SbldbTheme.colors
    Row(modifier.semantics { contentDescription = "$rating out of 5" }) {
        repeat(5) { i ->
            Box(
                Modifier
                    .size(30.dp)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button) { onSelect(i + 1) },
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Box(Modifier.size(11.dp).background(if (i < rating) colors.accent else colors.empty, CircleShape))
            }
        }
    }
}
