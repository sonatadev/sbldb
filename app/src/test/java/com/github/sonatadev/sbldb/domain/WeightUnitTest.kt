package com.github.sonatadev.sbldb.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightUnitTest {
    @Test
    fun `kg values are formatted without useless decimals`() {
        assertEquals("80", WeightUnit.KG.format(80.0))
        assertEquals("82.5", WeightUnit.KG.format(82.5))
    }

    @Test
    fun `pounds round trip through kg storage`() {
        val stored = WeightUnit.LB.toKg(225.0)
        assertEquals(102.058, stored, 0.001)
        assertEquals("225", WeightUnit.LB.format(stored))
    }

    @Test
    fun `pounds show one decimal, kg two`() {
        assertEquals("176.4", WeightUnit.LB.format(80.0))
        assertEquals("81.25", WeightUnit.KG.format(81.25))
    }
}
