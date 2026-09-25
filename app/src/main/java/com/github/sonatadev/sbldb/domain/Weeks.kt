package com.github.sonatadev.sbldb.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** A Monday-to-Monday week as epoch millis, [startMillis] inclusive and [endMillis] exclusive. */
data class WeekRange(val start: LocalDate, val startMillis: Long, val endMillis: Long) {
    companion object {
        /** The week [weeksAgo] weeks before the one containing [now]. */
        fun of(weeksAgo: Int, zone: ZoneId = ZoneId.systemDefault(), now: Instant = Instant.now()): WeekRange {
            val monday = now.atZone(zone).toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(weeksAgo.toLong())
            return WeekRange(
                start = monday,
                startMillis = monday.atStartOfDay(zone).toInstant().toEpochMilli(),
                endMillis = monday.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
            )
        }
    }
}
