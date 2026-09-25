package com.github.sonatadev.sbldb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarGridTest {
    @Test
    fun `month starting on monday has no leading blanks`() {
        val weeks = CalendarGrid.weeks(YearMonth.of(2026, 6)) // 1 June 2026 is a Monday
        assertEquals(LocalDate.of(2026, 6, 1), weeks.first().first())
    }

    @Test
    fun `month starting on sunday has six leading blanks`() {
        val weeks = CalendarGrid.weeks(YearMonth.of(2026, 3)) // 1 March 2026 is a Sunday
        assertTrue(weeks.first().take(6).all { it == null })
        assertEquals(LocalDate.of(2026, 3, 1), weeks.first()[6])
        assertEquals(6, weeks.size)
    }

    @Test
    fun `every week has seven cells and all days appear once`() {
        val month = YearMonth.of(2028, 2) // leap year
        val weeks = CalendarGrid.weeks(month)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals((1..29).map { month.atDay(it) }, weeks.flatten().filterNotNull())
        assertNull(weeks.last().last())
    }
}
