package com.github.sonatadev.sbldb.data.importer

import com.github.sonatadev.sbldb.data.entity.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class ForeignCsvTest {
    private val zone = ZoneOffset.UTC
    private fun millis(text: String) = LocalDateTime.parse(text).toInstant(zone).toEpochMilli()

    private val strong = """
        Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes,Workout Notes,RPE
        2026-09-01 18:30:00,"Push, heavy",1h 5m,Bench Press (Barbell),W,40,10,0,0,,,
        2026-09-01 18:30:00,"Push, heavy",1h 5m,Bench Press (Barbell),1,80,8,0,0,"seat 4",felt good,8
        2026-09-01 18:30:00,"Push, heavy",1h 5m,Bench Press (Barbell),D,60,12,0,0,,,
        2026-09-01 18:30:00,"Push, heavy",1h 5m,Plank,1,0,0,0,45,,,
        2026-09-03 07:00:00,Legs,45m,Squat (Barbell),1,"100,5",5,0,0,,,9.5
    """.trimIndent()

    private val hevy = """
        "title","start_time","end_time","description","exercise_title","superset_id","exercise_notes","set_index","set_type","weight_kg","reps","distance_km","duration_seconds","rpe"
        "Upper A","2 Sep 2026, 18:00","2 Sep 2026, 19:10","","Lat Pulldown (Cable)",,"",0,"warmup",30,12,,,
        "Upper A","2 Sep 2026, 18:00","2 Sep 2026, 19:10","","Lat Pulldown (Cable)",,"",1,"normal",55,10,,,8
        "Upper A","2 Sep 2026, 18:00","2 Sep 2026, 19:10","","Bicep Curl (Dumbbell)",,"",0,"failure",14,9,,,10
    """.trimIndent()

    @Test
    fun `strong export`() {
        val log = ForeignCsv.parse(strong, zone)
        assertEquals(ForeignApp.STRONG, log.app)
        assertNull(log.weightIsKg)
        assertEquals(2, log.workouts.size)
        val push = log.workouts.first()
        assertEquals("Push, heavy", push.name)
        assertEquals(millis("2026-09-01T18:30:00"), push.startedAt)
        assertEquals(65 * 60_000L, push.endedAt - push.startedAt)
        assertEquals("felt good", push.notes)
        val bench = push.exercises.first()
        assertEquals("Bench Press (Barbell)", bench.name)
        assertEquals("seat 4", bench.note)
        assertEquals(listOf(SetType.WARMUP, SetType.NORMAL, SetType.DROP), bench.sets.map { it.type })
        assertEquals(2, bench.sets[1].rir)
        assertEquals(45, push.exercises[1].sets.single().seconds)
        assertNull(push.exercises[1].sets.single().weight)
        // Decimal comma, RPE 9.5 rounds to RIR 1 (half up)
        val squat = log.workouts[1].exercises.single().sets.single()
        assertEquals(100.5, squat.weight!!, 0.0)
        assertEquals(1, squat.rir)
    }

    @Test
    fun `hevy export`() {
        val log = ForeignCsv.parse(hevy, zone)
        assertEquals(ForeignApp.HEVY, log.app)
        assertEquals(true, log.weightIsKg)
        val w = log.workouts.single()
        assertEquals(70 * 60_000L, w.endedAt - w.startedAt)
        assertEquals(listOf("Lat Pulldown (Cable)", "Bicep Curl (Dumbbell)"), w.exercises.map { it.name })
        assertEquals(SetType.WARMUP, w.exercises[0].sets[0].type)
        assertEquals(SetType.FAILURE, w.exercises[1].sets[0].type)
        assertEquals(0, w.exercises[1].sets[0].rir)
    }

    @Test
    fun `semicolons and line breaks in quotes`() {
        val rows = ForeignCsv.rows("a;\"b;c\";\"x\ny\"\n1;2;3", ';')
        assertEquals(listOf(listOf("a", "b;c", "x\ny"), listOf("1", "2", "3")), rows)
    }

    @Test
    fun `other files are refused`() {
        val e = runCatching { ForeignCsv.parse("name,age\nbob,3") }.exceptionOrNull()
        assertTrue(e is ImportFormatException)
    }

    @Test
    fun `durations`() {
        assertEquals(65, ForeignCsv.strongDuration("1h 5m"))
        assertEquals(45, ForeignCsv.strongDuration("45m"))
        assertEquals(1, ForeignCsv.strongDuration("50s"))
    }
}
