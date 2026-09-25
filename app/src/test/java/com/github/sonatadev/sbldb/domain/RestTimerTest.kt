package com.github.sonatadev.sbldb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerTest {
    @Test
    fun `counts down and finishes`() {
        val t = RestTimer.start(90, now = 0)
        assertEquals(90, t.remainingSeconds(0))
        assertEquals(60, t.remainingSeconds(30_000))
        assertEquals(1, t.remainingSeconds(89_500))
        assertFalse(t.isFinished(89_999))
        assertTrue(t.isFinished(90_000))
        assertEquals(0, t.remainingSeconds(120_000))
    }

    @Test
    fun `adding and removing time moves the end and the total`() {
        val t = RestTimer.start(60, now = 0).adjust(15, now = 10_000)
        assertEquals(65, t.remainingSeconds(10_000))
        assertEquals(75, t.totalSeconds)
        val shorter = t.adjust(-15, now = 10_000)
        assertEquals(50, shorter.remainingSeconds(10_000))
    }

    @Test
    fun `removing more time than left ends the rest now`() {
        val t = RestTimer.start(30, now = 0).adjust(-60, now = 20_000)
        assertTrue(t.isFinished(20_000))
    }

    @Test
    fun `progress goes from one to zero and idle does nothing`() {
        val t = RestTimer.start(100, now = 0)
        assertEquals(1f, t.progress(0), 0.001f)
        assertEquals(0.5f, t.progress(50_000), 0.001f)
        assertEquals(RestTimer.Idle, RestTimer.Idle.adjust(15, 0))
        assertFalse(RestTimer.Idle.isRunning)
    }
}
