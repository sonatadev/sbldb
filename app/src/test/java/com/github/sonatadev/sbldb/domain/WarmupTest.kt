package com.github.sonatadev.sbldb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarmupTest {
    @Test
    fun heavyBarbellGetsThreeRampSets() {
        val ramp = Warmup.ramp(100.0, "Barbell", WeightUnit.KG)
        assertEquals(listOf(40.0, 60.0, 80.0), ramp.map { it.weightKg })
        assertEquals(listOf(8, 5, 3), ramp.map { it.reps })
    }

    @Test
    fun neverBelowTheEmptyBarAndNoDuplicates() {
        val ramp = Warmup.ramp(40.0, "Barbell", WeightUnit.KG)
        assertEquals(listOf(20.0, 25.0, 32.5), ramp.map { it.weightKg })
    }

    @Test
    fun lightDumbbellsRoundToTheirIncrement() {
        val ramp = Warmup.ramp(10.0, "Dumbbell", WeightUnit.KG)
        assertEquals(listOf(4.0, 6.0, 8.0), ramp.map { it.weightKg })
    }

    @Test
    fun tinyLoadsGetNoRampSetsAtOrAboveWorkWeight() {
        Warmup.ramp(2.5, "Cable", WeightUnit.KG).forEach { assertTrue(it.weightKg < 2.5) }
    }

    @Test
    fun poundsRoundToFive() {
        val ramp = Warmup.ramp(WeightUnit.LB.toKg(225.0), "Barbell", WeightUnit.LB)
        assertEquals(listOf(90.0, 135.0, 180.0), ramp.map { Math.round(WeightUnit.LB.fromKg(it.weightKg)).toDouble() })
    }
}
