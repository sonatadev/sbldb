package com.github.sonatadev.sbldb.domain

import com.github.sonatadev.sbldb.data.entity.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeeklyPlanTest {
    private val roles = mapOf(
        1 to listOf("Chest" to Role.PRIMARY, "Triceps" to Role.SECONDARY, "Shoulders" to Role.SECONDARY),
        // Two triceps heads: still one set per set for the group
        2 to listOf("Triceps" to Role.PRIMARY, "Triceps" to Role.PRIMARY),
        3 to listOf("Shoulders" to Role.PRIMARY, "Shoulders" to Role.SECONDARY)
    )

    @Test
    fun frequencyMultipliesAndSecondaryCountsHalf() {
        val upper = PlannedRoutine(timesPerWeek = 2, slots = listOf(PlannedSlot(1, 3), PlannedSlot(2, 2)))
        val plan = WeeklyPlan.setsPerGroup(listOf(upper), roles)
        assertEquals(6.0, plan["Chest"]!!, 0.0)
        assertEquals(3.0 + 4.0, plan["Triceps"]!!, 0.0) // 3×0.5×2 + 2×1×2
        assertEquals(3.0, plan["Shoulders"]!!, 0.0)
    }

    @Test
    fun routinesAddUpAndHighestRoleWins() {
        val a = PlannedRoutine(1, listOf(PlannedSlot(3, 4)))
        val b = PlannedRoutine(1, listOf(PlannedSlot(1, 2)))
        val plan = WeeklyPlan.setsPerGroup(listOf(a, b), roles)
        assertEquals(4.0 + 1.0, plan["Shoulders"]!!, 0.0)
    }

    @Test
    fun unknownExercisesAreIgnored() {
        val plan = WeeklyPlan.setsPerGroup(listOf(PlannedRoutine(3, listOf(PlannedSlot(99, 5)))), roles)
        assertNull(plan["Chest"])
    }
}
