package com.github.sonatadev.sbldb.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JointFigureCycleTest {
    @Test
    fun holdsMovesOneWayHoldsThenRestarts() {
        assertEquals(0f, cycleProgress(0f))
        assertEquals(0f, cycleProgress(0.1f))
        val samples = (20..67).map { cycleProgress(it / 100f) }
        assertTrue(samples.zipWithNext().all { (a, b) -> b >= a }) // never reverses
        assertEquals(1f, cycleProgress(0.8f))
        assertEquals(1f, cycleProgress(0.99f))
    }
}
