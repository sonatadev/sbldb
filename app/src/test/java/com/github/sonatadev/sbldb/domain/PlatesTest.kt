package com.github.sonatadev.sbldb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlatesTest {
    @Test
    fun hundredKilos() {
        assertEquals(listOf(25.0, 15.0), Plates.perSide(100.0, WeightUnit.KG))
    }

    @Test
    fun smallChangePlates() {
        assertEquals(listOf(25.0, 5.0, 2.5, 1.25), Plates.perSide(87.5, WeightUnit.KG))
    }

    @Test
    fun emptyBarOrLessHasNoPlates() {
        assertNull(Plates.perSide(20.0, WeightUnit.KG))
        assertNull(Plates.perSide(15.0, WeightUnit.KG))
    }

    @Test
    fun impossibleLoadsGiveNothing() {
        assertNull(Plates.perSide(21.0, WeightUnit.KG))
    }

    @Test
    fun poundsUseTheFortyFiveBar() {
        assertEquals(listOf(45.0, 45.0), Plates.perSide(WeightUnit.LB.toKg(225.0), WeightUnit.LB))
    }
}
