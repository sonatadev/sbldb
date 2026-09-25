package com.github.sonatadev.sbldb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutinePlanTest {
    private fun sets(vararg reps: Int, rir: Int? = 2) = reps.map { PastSet(60.0, it, rir) }

    @Test
    fun sameRepsEverySetWidensToAFourRepRange() {
        assertEquals(SlotPlan(3, 8, 12, 2), RoutinePlan.fromSets(sets(10, 10, 10)))
    }

    @Test
    fun wideEnoughRangeIsKept() {
        assertEquals(SlotPlan(4, 6, 12, 2), RoutinePlan.fromSets(sets(12, 10, 8, 6)))
    }

    @Test
    fun lowRepsNeverGoBelowOne() {
        assertEquals(1, RoutinePlan.fromSets(sets(1, 2))!!.repMin)
    }

    @Test
    fun targetRirIsTheTypicalEffortAndOptional() {
        val mixed = listOf(PastSet(60.0, 8, 0), PastSet(60.0, 8, 2), PastSet(60.0, 8, 3))
        assertEquals(2, RoutinePlan.fromSets(mixed)!!.targetRir)
        assertNull(RoutinePlan.fromSets(sets(8, 8, rir = null))!!.targetRir)
    }

    @Test
    fun nothingDoneGivesNothing() {
        assertNull(RoutinePlan.fromSets(listOf(PastSet(60.0, null, 2))))
    }
}
