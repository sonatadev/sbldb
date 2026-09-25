package com.github.sonatadev.sbldb.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class WeekRangeTest {
    private val thursday = LocalDate.of(2026, 9, 24).atTime(18, 0).toInstant(ZoneOffset.UTC)

    @Test
    fun `current week starts on monday`() {
        val week = WeekRange.of(0, ZoneOffset.UTC, thursday)
        assertEquals(LocalDate.of(2026, 9, 21), week.start)
        assertEquals(7 * 24 * 3600 * 1000L, week.endMillis - week.startMillis)
    }

    @Test
    fun `weeks ago goes back whole weeks`() {
        assertEquals(LocalDate.of(2026, 9, 7), WeekRange.of(2, ZoneOffset.UTC, thursday).start)
    }
}
