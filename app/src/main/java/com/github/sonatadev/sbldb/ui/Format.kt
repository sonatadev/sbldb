package com.github.sonatadev.sbldb.ui

import com.github.sonatadev.sbldb.domain.WeightUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)
private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateFormatter)

fun formatShortDate(date: java.time.LocalDate): String = date.format(shortDateFormatter)

/** "1h 05m", "42m" or "0:37" for short durations. */
fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "%dh %02dm".format(hours, minutes)
        minutes >= 10 -> "%dm".format(minutes)
        else -> "%d:%02d".format(minutes, seconds)
    }
}

/** Stopwatch style: "42:17", "1:05:09". */
fun formatClock(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFormatter)

/** Fractional set count without useless decimals: 3, 1.5. */
fun formatSets(sets: Double): String =
    BigDecimal(sets).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/** "80 kg × 8", "80 kg × 8 @2", "× 12" for bodyweight sets. */
fun formatSet(weightKg: Double?, reps: Int?, rir: Int?, unit: WeightUnit): String = buildString {
    if (weightKg != null) append("${unit.format(weightKg)} ${unit.label} ")
    append("× ${reps ?: "–"}")
    if (rir != null) append(" @$rir")
}
