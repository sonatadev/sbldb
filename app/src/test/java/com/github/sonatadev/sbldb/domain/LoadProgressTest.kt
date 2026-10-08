package com.github.sonatadev.sbldb.domain

import com.github.sonatadev.sbldb.data.entity.SetHistoryRow
import com.github.sonatadev.sbldb.data.entity.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoadProgressTest {
    private fun set(workout: Long, kg: Double?, reps: Int?, type: SetType = SetType.NORMAL) =
        SetHistoryRow(workoutId = workout, startedAt = workout * 1000, weightKg = kg, reps = reps, rir = 2, setType = type)

    private val history = listOf(
        set(2, 85.0, 5), set(2, 100.0, 3, SetType.DROP), set(2, 80.0, 8),
        set(1, 80.0, 6), set(1, 80.0, 5)
    )

    @Test
    fun `series is one point per session, oldest first`() {
        assertEquals(listOf(1000L, 2000L), LoadProgress.series(history, LoadMetric.E1RM).map { it.first })
    }

    @Test
    fun `heaviest ignores drop sets and partials`() {
        assertEquals(listOf(80.0, 85.0), LoadProgress.series(history, LoadMetric.HEAVIEST).map { it.second })
    }

    @Test
    fun `volume adds weight times reps over the session`() {
        assertEquals(listOf(880.0, 425.0 + 300.0 + 640.0), LoadProgress.series(history, LoadMetric.VOLUME).map { it.second })
    }

    @Test
    fun `sessions without a usable value are skipped`() {
        val bodyweight = listOf(set(1, null, 12), set(2, 20.0, 20))
        assertEquals(emptyList<Pair<Long, Double>>(), LoadProgress.series(bodyweight, LoadMetric.E1RM))
    }

    @Test
    fun `change and trend`() {
        assertEquals(0.1, LoadProgress.change(listOf(1L to 100.0, 2L to 90.0, 3L to 110.0))!!, 1e-9)
        assertNull(LoadProgress.change(listOf(1L to 100.0)))
        assertEquals(Trend.UP, LoadProgress.trend(0.05))
        assertEquals(Trend.FLAT, LoadProgress.trend(-0.02))
        assertEquals(Trend.DOWN, LoadProgress.trend(-0.03))
        assertEquals(Trend.NEW, LoadProgress.trend(null))
    }

    @Test
    fun `overview leaves out counterweight machines and puts recent first`() {
        val bench = ExerciseRef(1, "Bench Press")
        val pullUp = ExerciseRef(2, "Assisted Pull-Up Machine")
        val row = ExerciseRef(3, "Seal Row")
        val rows = listOf(
            bench to set(1, 80.0, 5), row to set(3, 60.0, 8), pullUp to set(2, 30.0, 8), bench to set(2, 82.5, 5)
        )
        assertEquals(listOf("Seal Row", "Bench Press"), LoadProgress.of(rows, LoadMetric.E1RM).map { it.name })
    }
}
