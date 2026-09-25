package com.github.sonatadev.sbldb.domain

import com.github.sonatadev.sbldb.data.entity.Role
import com.github.sonatadev.sbldb.data.entity.VolumeRow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class VolumeCalculatorTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 9, 21).atStartOfDay(zone).toInstant().toEpochMilli()
    private val day = 24 * 3600 * 1000L

    /** Bench press as in exercises.yaml: chest sternal primary, chest clavicular / delts / triceps secondary. */
    private fun benchSet(setId: Long, startedAt: Long = monday, rir: Int? = 2) = listOf(
        row(setId, startedAt, "Chest", "Sternocostal Head", Role.PRIMARY, rir),
        row(setId, startedAt, "Chest", "Clavicular Head", Role.SECONDARY, rir),
        row(setId, startedAt, "Shoulders", "Anterior Head", Role.SECONDARY, rir),
        row(setId, startedAt, "Triceps", null, Role.SECONDARY, rir)
    )

    /** Pushdown: three triceps heads, one primary. */
    private fun pushdownSet(setId: Long, startedAt: Long = monday) = listOf(
        row(setId, startedAt, "Triceps", "Lateral Head", Role.PRIMARY, 1),
        row(setId, startedAt, "Triceps", "Long Head", Role.SECONDARY, 1),
        row(setId, startedAt, "Triceps", "Medial Head", Role.SECONDARY, 1)
    )

    private fun row(setId: Long, startedAt: Long, group: String, region: String?, role: Role, rir: Int?) =
        VolumeRow(setId, workoutId = startedAt, startedAt = startedAt, exerciseId = 1, muscleGroup = group, muscleRegion = region, role = role, rir = rir)

    private fun List<MuscleGroupVolume>.of(group: String) = first { it.muscleGroup == group }

    @Test
    fun `primary counts one and secondary counts half`() {
        val result = VolumeCalculator.calculate((1L..3L).flatMap { benchSet(it) }, zone = zone)
        assertEquals(3.0, result.of("Chest").sets, 0.0)
        assertEquals(1.5, result.of("Triceps").sets, 0.0)
        assertEquals(1.5, result.of("Shoulders").sets, 0.0)
    }

    @Test
    fun `a set counts once per group using the highest role`() {
        val result = VolumeCalculator.calculate(pushdownSet(1) + pushdownSet(2), zone = zone)
        assertEquals(2.0, result.of("Triceps").sets, 0.0)
    }

    @Test
    fun `regions are counted separately and null regions only count for the group`() {
        val result = VolumeCalculator.calculate(pushdownSet(1) + benchSet(2), zone = zone)
        val regions = result.of("Triceps").regions.associate { it.region to it.sets }
        assertEquals(mapOf("Lateral Head" to 1.0, "Long Head" to 0.5, "Medial Head" to 0.5), regions)
        assertEquals(1.5, result.of("Triceps").sets, 0.0)
    }

    @Test
    fun `sets far from failure are excluded, unknown RIR counts`() {
        val rows = benchSet(1, rir = 5) + benchSet(2, rir = 4) + benchSet(3, rir = null)
        assertEquals(2.0, VolumeCalculator.calculate(rows, zone = zone).of("Chest").sets, 0.0)
    }

    @Test
    fun `frequency counts distinct training days`() {
        val rows = benchSet(1) + benchSet(2, startedAt = monday + 3600_000) + benchSet(3, startedAt = monday + 3 * day)
        assertEquals(2, VolumeCalculator.calculate(rows, zone = zone).of("Chest").frequency)
    }

    @Test
    fun `untrained groups are included with zero sets and a low band`() {
        val result = VolumeCalculator.calculate(benchSet(1), allGroups = listOf("Chest", "Calves"), zone = zone)
        assertEquals(0.0, result.of("Calves").sets, 0.0)
        assertEquals(VolumeBand.LOW, result.of("Calves").band)
        assertEquals("Chest", result.first().muscleGroup)
    }

    @Test
    fun `bands follow the 10 to 20 reference range`() {
        fun band(sets: Double) = MuscleGroupVolume("X", sets, 0, emptyList()).band
        assertEquals(VolumeBand.LOW, band(9.5))
        assertEquals(VolumeBand.OPTIMAL, band(10.0))
        assertEquals(VolumeBand.OPTIMAL, band(20.0))
        assertEquals(VolumeBand.HIGH, band(20.5))
    }
}
