package com.github.sonatadev.sbldb.ui

import com.github.sonatadev.sbldb.ui.components.DotFill
import com.github.sonatadev.sbldb.ui.components.effortFor
import com.github.sonatadev.sbldb.ui.components.volumeDots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DotsTest {
    @Test
    fun `one full dot per set and a half dot for half sets`() {
        val dots = volumeDots(3.5)
        assertEquals(20, dots.size)
        assertEquals(List(3) { DotFill.FULL }, dots.take(3).map { it.fill })
        assertEquals(DotFill.HALF, dots[3].fill)
        assertTrue(dots.drop(4).all { it.fill == DotFill.EMPTY })
    }

    @Test
    fun `the zone starts at the tenth dot`() {
        val dots = volumeDots(12.0)
        assertFalse(dots[8].inZone)
        assertTrue(dots[9].inZone)
        assertTrue(dots[19].inZone)
    }

    @Test
    fun `volume above twenty fills every dot`() {
        assertTrue(volumeDots(24.0).all { it.fill == DotFill.FULL })
    }

    @Test
    fun `effort meter lights more segments closer to failure`() {
        assertEquals(5, effortFor(0).filled)
        assertEquals(3, effortFor(2).filled)
        assertEquals(1, effortFor(4).filled)
        assertTrue(effortFor(4).counts)
    }

    @Test
    fun `sets too far from failure do not count`() {
        assertFalse(effortFor(5).counts)
        assertEquals(0, effortFor(null).filled)
        assertTrue(effortFor(null).counts)
    }
}
