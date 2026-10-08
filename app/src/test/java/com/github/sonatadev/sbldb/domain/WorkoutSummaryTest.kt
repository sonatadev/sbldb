package com.github.sonatadev.sbldb.domain

import com.github.sonatadev.sbldb.data.entity.Role
import com.github.sonatadev.sbldb.data.entity.SetHistoryRow
import com.github.sonatadev.sbldb.data.entity.SetType
import com.github.sonatadev.sbldb.data.entity.VolumeRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSummaryTest {
    private fun set(workout: Long, kg: Double, reps: Int, type: SetType = SetType.NORMAL) =
        SetHistoryRow(workoutId = workout, startedAt = workout * 1000, weightKg = kg, reps = reps, rir = 1, setType = type)

    @Test
    fun `records beaten today, with the best set`() {
        val bench = 1 to "Barbell Bench Press"
        val history = mapOf(
            bench to listOf(set(3, 82.5, 5), set(3, 80.0, 8), set(2, 80.0, 6), set(1, 80.0, 5))
        )
        val hits = WorkoutSummary.records(history, workoutId = 3, startedAt = 3000)
        val hit = hits.single()
        assertTrue(PrKind.WEIGHT in hit.kinds)
        assertTrue(PrKind.REPS in hit.kinds)
        // 80 × 8 (e1RM 101) beats 82.5 × 5 (e1RM 96)
        assertEquals(80.0, hit.weightKg!!, 0.0)
        assertEquals(8, hit.reps)
    }

    @Test
    fun `no records on a first session or with drop sets only`() {
        val first = mapOf((1 to "Leg Press") to listOf(set(1, 200.0, 10)))
        assertTrue(WorkoutSummary.records(first, 1, 1000).isEmpty())
        val drops = mapOf((1 to "Leg Press") to listOf(set(2, 300.0, 10, SetType.DROP), set(1, 200.0, 10)))
        assertTrue(WorkoutSummary.records(drops, 2, 2000).isEmpty())
    }

    @Test
    fun `counterweight machines only count rep records`() {
        val pullUp = mapOf((1 to "Assisted Pull-Up Machine") to listOf(set(2, 40.0, 8), set(1, 30.0, 8)))
        assertTrue(WorkoutSummary.records(pullUp, 2, 2000).isEmpty())
    }

    @Test
    fun `muscles trained today with their week`() {
        fun row(set: Long, workout: Long, group: String, role: Role) = VolumeRow(set, workout, workout * 1000, 1, group, null, role, 2)
        val rows = listOf(
            row(1, 1, "Lats", Role.PRIMARY), row(2, 2, "Lats", Role.PRIMARY), row(3, 2, "Lats", Role.PRIMARY),
            row(3, 2, "Biceps", Role.SECONDARY), row(4, 1, "Chest", Role.PRIMARY)
        )
        val muscles = WorkoutSummary.muscles(rows, workoutId = 2, targets = mapOf("Lats" to VolumeTarget(6, 12)))
        assertEquals(listOf("Lats", "Biceps"), muscles.map { it.muscle.muscleGroup })
        assertEquals(2.0, muscles[0].today, 0.0)
        assertEquals(3.0, muscles[0].muscle.sets, 0.0)
        assertEquals(VolumeTarget(6, 12), muscles[0].muscle.target)
    }

    @Test
    fun `tonnage adds weight times reps`() {
        assertEquals(1200.0, WorkoutSummary.tonnage(listOf(100.0 to 10, 50.0 to 4, null to 12)), 0.0)
    }
}
