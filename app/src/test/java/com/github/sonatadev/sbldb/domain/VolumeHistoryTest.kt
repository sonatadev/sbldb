package com.github.sonatadev.sbldb.domain

import com.github.sonatadev.sbldb.data.entity.Role
import com.github.sonatadev.sbldb.data.entity.VolumeRow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class VolumeHistoryTest {
    private val zone = ZoneOffset.UTC
    // Wednesday 30 Sep 2026: this week starts Monday 28 Sep
    private val now = LocalDate.of(2026, 9, 30).atStartOfDay(zone).toInstant()
    private val day = 24 * 3600 * 1000L

    private fun weeks(ago: Int = 0, count: Int = 3) = VolumeHistory.weeksEndingAt(ago, count, zone, now)

    private fun benchSet(setId: Long, startedAt: Long, rir: Int? = 2) = listOf(
        VolumeRow(setId, startedAt, startedAt, 1, "Chest", null, Role.PRIMARY, rir),
        VolumeRow(setId, startedAt, startedAt, 1, "Triceps", "Long Head", Role.SECONDARY, rir),
        VolumeRow(setId, startedAt, startedAt, 1, "Triceps", "Lateral Head", Role.SECONDARY, rir)
    )

    @Test
    fun `weeks run oldest first and end with the selected one`() {
        val w = weeks(ago = 1)
        assertEquals(listOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 21)), w.map { it.start })
    }

    @Test
    fun `sets land in their own week`() {
        val w = weeks()
        val rows = benchSet(1, w[0].startMillis + day) + benchSet(2, w[2].startMillis) + benchSet(3, w[2].endMillis - 1)
        val history = VolumeHistory.of(rows, w, zone)
        assertEquals(listOf(1.0, 0.0, 2.0), history.of("Chest"))
        assertEquals(listOf(0.5, 0.0, 1.0), history.of("Triceps"))
    }

    @Test
    fun `totals count each hard set once, whatever it hits`() {
        val w = weeks()
        val rows = benchSet(1, w[1].startMillis) + benchSet(2, w[1].startMillis) + benchSet(3, w[1].startMillis, rir = 6)
        assertEquals(listOf(0.0, 2.0, 0.0), VolumeHistory.of(rows, w, zone).totals)
    }

    @Test
    fun `untrained groups read as zeros`() {
        assertEquals(listOf(0.0, 0.0, 0.0), VolumeHistory.of(emptyList(), weeks(), zone).of("Calves"))
    }
}
