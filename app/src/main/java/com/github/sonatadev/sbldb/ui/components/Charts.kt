package com.github.sonatadev.sbldb.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.github.sonatadev.sbldb.domain.WeightUnit
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
