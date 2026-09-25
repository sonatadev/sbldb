package com.github.sonatadev.sbldb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyStatsTest {
    @Test
    fun steadyGainGivesItsWeeklyRate() {
        // +0.05 kg a day = +0.35 kg a week around 80 kg
        val points = (0L..21L).map { WeightPoint(it, 80.0 + it * 0.05) }
        val rate = BodyStats.rate(points)!!
        assertEquals(0.35, rate.kgPerWeek, 1e-9)
        assertEquals(0.35 / 80.525 * 100, rate.percentPerWeek, 1e-6)
    }

    @Test
    fun tooFewOrTooCloseReadingsGiveNothing() {
        assertNull(BodyStats.rate(listOf(WeightPoint(0, 80.0), WeightPoint(10, 81.0))))
        assertNull(BodyStats.rate((0L..3L).map { WeightPoint(it, 80.0) }))
    }

    @Test
    fun oldReadingsAreIgnored() {
        // A cut long ago, flat for the last four weeks
        val old = (0L..30L).map { WeightPoint(it, 90.0 - it * 0.3) }
        val recent = (60L..90L step 3).map { WeightPoint(it, 80.0) }
        assertEquals(0.0, BodyStats.rate(old + recent)!!.kgPerWeek, 1e-9)
    }

    @Test
    fun rollingAverageSmoothsTheLastWeek() {
        val points = listOf(WeightPoint(0, 80.0), WeightPoint(3, 82.0), WeightPoint(10, 81.0))
        val avg = BodyStats.rollingAverage(points)
        assertEquals(listOf(80.0, 81.0, 81.0), avg.map { it.kg })
    }
}
