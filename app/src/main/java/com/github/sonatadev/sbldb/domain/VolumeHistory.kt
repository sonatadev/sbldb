package com.github.sonatadev.sbldb.domain

import com.github.sonatadev.sbldb.data.entity.VolumeRow
import java.time.Instant
import java.time.ZoneId

/**
 * Hard sets week by week, oldest week first.
 * [totals] counts each hard set once; [byGroup] uses the same fractional rules as [VolumeCalculator].
 */
data class WeeklyVolume(
    val weeks: List<WeekRange> = emptyList(),
    val totals: List<Double> = emptyList(),
    val byGroup: Map<String, List<Double>> = emptyMap()
) {
    /** The group's sets for every week, zeros where it wasn't trained. */
    fun of(group: String): List<Double> = byGroup[group] ?: List(weeks.size) { 0.0 }
}

object VolumeHistory {
    /** How many weeks the volume screen looks back, the selected week included. */
    const val WEEKS = 12

    /** The [count] weeks ending with the one [weeksAgo] weeks back, oldest first. */
    fun weeksEndingAt(weeksAgo: Int, count: Int = WEEKS, zone: ZoneId = ZoneId.systemDefault(), now: Instant = Instant.now()): List<WeekRange> =
        (weeksAgo + count - 1 downTo weeksAgo).map { WeekRange.of(it, zone, now) }

    fun of(rows: List<VolumeRow>, weeks: List<WeekRange>, zone: ZoneId = ZoneId.systemDefault()): WeeklyVolume {
        val byWeek = weeks.map { week -> rows.filter { it.startedAt >= week.startMillis && it.startedAt < week.endMillis } }
        val perWeek = byWeek.map { VolumeCalculator.calculate(it, zone = zone).associate { g -> g.muscleGroup to g.sets } }
        val groups = perWeek.flatMap { it.keys }.distinct()
        return WeeklyVolume(
            weeks = weeks,
            totals = byWeek.map { week -> week.filter { VolumeCalculator.isHardSet(it.rir) }.map { it.setId }.distinct().size.toDouble() },
            byGroup = groups.associateWith { g -> perWeek.map { it[g] ?: 0.0 } }
        )
    }
}
