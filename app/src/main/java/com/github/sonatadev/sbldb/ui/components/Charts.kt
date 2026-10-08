package com.github.sonatadev.sbldb.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.github.sonatadev.sbldb.domain.VolumeBand
import com.github.sonatadev.sbldb.domain.VolumeTarget
import com.github.sonatadev.sbldb.domain.WeightUnit
import com.github.sonatadev.sbldb.ui.formatSets
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chartDate: DateTimeFormatter get() = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

/**
 * Line of a value over time (e.g. best e1RM per session), x spaced by real dates.
 * Values are in kg and labelled in [unit]; only the range ends and the latest value are labelled.
 */
@Composable
fun TrendChart(points: List<Pair<Long, Double>>, unit: WeightUnit, modifier: Modifier = Modifier) =
    TrendChart(points, modifier, format = unit::formatRounded, unitLabel = unit.label)

/** [format] turns a stored value into its label; [unitLabel] is only used for accessibility. */
@Composable
fun TrendChart(points: List<Pair<Long, Double>>, modifier: Modifier = Modifier, format: (Double) -> String, unitLabel: String) {
    if (points.size < 2) return
    val colors = SbldbTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = SbldbType.label.copy(color = colors.muted)
    val valueStyle = SbldbType.mono.copy(color = colors.accent)
    val description = remember(points, unitLabel) {
        "From ${format(points.first().second)} to ${format(points.last().second)} $unitLabel"
    }
    Canvas(modifier.semantics { contentDescription = description }) {
        val minV = points.minOf { it.second }
        val maxV = points.maxOf { it.second }
        val span = (maxV - minV).takeIf { it > 0 } ?: 1.0
        val t0 = points.first().first
        val tSpan = (points.last().first - t0).takeIf { it > 0 } ?: 1L

        val maxLabel = measurer.measure(format(maxV), labelStyle)
        val minLabel = measurer.measure(format(minV), labelStyle)
        val left = maxOf(maxLabel.size.width, minLabel.size.width) + 8.dp.toPx()
        val right = 14.dp.toPx()
        val top = 22.dp.toPx()
        val bottom = size.height - 18.dp.toPx()

        fun x(t: Long) = left + (size.width - left - right) * ((t - t0).toFloat() / tSpan)
        fun y(v: Double) = bottom - (bottom - top) * ((v - minV) / span).toFloat()

        // Top and bottom guides at the best and lowest values
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        listOf(maxV, minV).forEach { v ->
            drawLine(colors.line, Offset(left, y(v)), Offset(size.width - right, y(v)), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        }
        drawText(maxLabel, topLeft = Offset(0f, y(maxV) - maxLabel.size.height / 2f))
        drawText(minLabel, topLeft = Offset(0f, y(minV) - minLabel.size.height / 2f))

        val path = Path()
        points.forEachIndexed { i, (t, v) -> if (i == 0) path.moveTo(x(t), y(v)) else path.lineTo(x(t), y(v)) }
        drawPath(path, colors.accent, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.dropLast(1).forEach { (t, v) -> drawCircle(colors.accent, radius = 2.5.dp.toPx(), center = Offset(x(t), y(v))) }

        val (lastT, lastV) = points.last()
        drawCircle(colors.module, radius = 6.dp.toPx(), center = Offset(x(lastT), y(lastV)))
        drawCircle(colors.accent, radius = 4.5.dp.toPx(), center = Offset(x(lastT), y(lastV)))
        val lastLabel = measurer.measure(format(lastV), valueStyle)
        drawText(
            lastLabel,
            topLeft = Offset(
                (x(lastT) - lastLabel.size.width).coerceAtLeast(left),
                (y(lastV) - lastLabel.size.height - 6.dp.toPx()).coerceAtLeast(0f)
            )
        )

        val zone = ZoneId.systemDefault()
        val firstDate = measurer.measure(Instant.ofEpochMilli(t0).atZone(zone).format(chartDate).uppercase(), labelStyle)
        val lastDate = measurer.measure(Instant.ofEpochMilli(lastT).atZone(zone).format(chartDate).uppercase(), labelStyle)
        drawText(firstDate, topLeft = Offset(left, size.height - firstDate.size.height))
        drawText(lastDate, topLeft = Offset(size.width - right - lastDate.size.width + right, size.height - lastDate.size.height))
    }
}

/**
 * One rounded column per week, oldest first. The [highlight] week is drawn in the accent with its
 * value on top; with a [target] its zone sits behind the columns as a tinted band, as in [DotRow].
 * Empty weeks show a single empty dot. [onSelect] gets the index of a tapped column.
 */
@Composable
fun WeeklyBars(
    values: List<Double>,
    firstLabel: String,
    lastLabel: String,
    modifier: Modifier = Modifier,
    target: VolumeTarget? = null,
    highlight: Int = values.lastIndex,
    onSelect: ((Int) -> Unit)? = null
) {
    if (values.isEmpty()) return
    val colors = SbldbTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = SbldbType.label.copy(color = colors.muted)
    val valueStyle = SbldbType.mono.copy(color = colors.accent)
    val description = remember(values) { values.joinToString(", ") { formatSets(it) } + " sets per week" }
    val tap = if (onSelect == null) Modifier else Modifier.pointerInput(values.size) {
        detectTapGestures { offset -> onSelect((offset.x / (size.width.toFloat() / values.size)).toInt().coerceIn(0, values.lastIndex)) }
    }
    Canvas(modifier.then(tap).semantics { contentDescription = description }) {
        val first = measurer.measure(firstLabel.uppercase(), labelStyle)
        val last = measurer.measure(lastLabel.uppercase(), labelStyle)
        val top = 20.dp.toPx()
        val bottom = size.height - first.size.height - 6.dp.toPx()
        val maxV = maxOf(values.max(), target?.maxSets?.toDouble() ?: 0.0, 1.0)
        val slot = size.width / values.size
        val barW = (slot * 0.56f).coerceAtMost(14.dp.toPx())
        fun y(v: Double) = bottom - (bottom - top) * (v / maxV).toFloat()

        if (target != null && target.maxSets > 0) {
            val zoneTop = y(target.maxSets.toDouble())
            drawRoundRect(
                colors.accentTint,
                topLeft = Offset(0f, zoneTop),
                size = Size(size.width, y(target.minSets.toDouble()) - zoneTop),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
        }
        values.forEachIndexed { i, v ->
            val cx = slot * i + slot / 2
            val lit = i == highlight
            if (v <= 0) {
                drawCircle(colors.empty, barW / 2.6f, Offset(cx, bottom - barW / 2.6f))
            } else {
                val inZone = target?.band(v) == VolumeBand.OPTIMAL
                val color = when {
                    lit -> colors.accent
                    inZone -> colors.accent.copy(alpha = 0.45f)
                    else -> colors.ink.copy(alpha = if (target == null) 0.28f else 0.8f)
                }
                val h = (bottom - y(v)).coerceAtLeast(barW)
                drawRoundRect(color, Offset(cx - barW / 2, bottom - h), Size(barW, h), CornerRadius(barW / 2))
            }
            if (lit) {
                val label = measurer.measure(formatSets(v), valueStyle)
                drawText(
                    label,
                    topLeft = Offset(
                        (cx - label.size.width / 2f).coerceIn(0f, size.width - label.size.width),
                        (y(v) - label.size.height - 3.dp.toPx()).coerceAtLeast(0f)
                    )
                )
            }
        }
        drawText(first, topLeft = Offset(0f, size.height - first.size.height))
        drawText(last, topLeft = Offset(size.width - last.size.width, size.height - last.size.height))
    }
}

/** A small unlabelled trend line with the latest point marked, for lists. */
@Composable
fun Sparkline(points: List<Pair<Long, Double>>, modifier: Modifier = Modifier) {
    val colors = SbldbTheme.colors
    Canvas(modifier) {
        val pad = 4.dp.toPx()
        if (points.size < 2) {
            drawCircle(colors.accent, 3.dp.toPx(), Offset(size.width - pad, size.height / 2))
            return@Canvas
        }
        val minV = points.minOf { it.second }
        val span = points.maxOf { it.second } - minV
        val t0 = points.first().first
        val tSpan = (points.last().first - t0).takeIf { it > 0 } ?: 1L
        fun x(t: Long) = pad + (size.width - 2 * pad) * ((t - t0).toFloat() / tSpan)
        // A flat series sits in the middle instead of on the floor
        fun y(v: Double) = if (span <= 0) size.height / 2 else size.height - pad - (size.height - 2 * pad) * ((v - minV) / span).toFloat()
        val path = Path()
        points.forEachIndexed { i, (t, v) -> if (i == 0) path.moveTo(x(t), y(v)) else path.lineTo(x(t), y(v)) }
        drawPath(path, colors.ink.copy(alpha = 0.35f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val (lt, lv) = points.last()
        drawCircle(colors.accent, 3.dp.toPx(), Offset(x(lt), y(lv)))
    }
}
