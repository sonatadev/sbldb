package com.github.sonatadev.sbldb.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class RemindersTest {
    private val zone = ZoneId.of("Europe/Rome")
    private fun at(day: Int, h: Int, m: Int) = ZonedDateTime.of(2026, 10, day, h, m, 0, 0, zone)

    @Test
    fun `rings later today if the time hasn't passed`() {
        assertEquals(at(9, 17, 0), Reminders.nextAt(17 * 60, at(9, 9, 30)))
    }

    @Test
    fun `rings tomorrow once the time has passed, or right at it`() {
        assertEquals(at(10, 17, 0), Reminders.nextAt(17 * 60, at(9, 17, 0)))
        assertEquals(at(10, 7, 30), Reminders.nextAt(7 * 60 + 30, at(9, 20, 0)))
    }

    @Test
    fun `keeps the wall-clock time across a DST change`() {
        // Clocks go back on 25 Oct 2026 in Rome
        assertEquals(ZonedDateTime.of(2026, 10, 25, 17, 0, 0, 0, zone), Reminders.nextAt(17 * 60, at(24, 18, 0)))
    }
}
