package com.github.sonatadev.sbldb.data.importer

import com.github.sonatadev.sbldb.data.SeedParser
import com.github.sonatadev.sbldb.data.Variants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/** Against the real library, with names as Strong and Hevy write them. */
class ExerciseMatcherTest {
    private val library = SeedParser.parseExercises(File("src/main/assets/exercises.yaml").inputStream())
        .flatMap { e -> listOf(e) + e.variants.map { e.variant(it) } }
        .mapIndexed { i, e -> MatchCandidate(i, e.name, e.equipment, e.aliases) }

    private fun match(name: String) = ExerciseMatcher.sure(name, library)?.name

    @Test
    fun `common names find their exercise`() {
        assertEquals("Barbell Bench Press", match("Bench Press (Barbell)"))
        assertEquals("Lat Pulldown", match("Lat Pulldown (Cable)"))
        assertEquals("Barbell Back Squat", match("Squat (Barbell)"))
        assertEquals("Romanian Deadlift", match("Romanian Deadlift (Barbell)"))
        assertEquals("Leg Extension", match("Leg Extension (Machine)"))
        assertEquals("Dumbbell Curl", match("Bicep Curl (Dumbbell)"))
        assertEquals("Pull-Up", match("Pull Up"))
    }

    @Test
    fun `a near tie is left for the user, with both offered`() {
        assertNull(match("Incline Bench Press (Dumbbell)"))
        val offered = ExerciseMatcher.best("Incline Bench Press (Dumbbell)", library).map { it.name }
        assertEquals(true, "Incline Dumbbell Press" in offered && "Dumbbell Bench Press" in offered)
    }

    @Test
    fun `nonsense is left for the user`() {
        assertNull(match("Tire Flip"))
    }

    @Test
    fun `suggestions are offered even when unsure`() {
        val names = ExerciseMatcher.best("Cable Crossover", library).map { it.name }
        assertEquals(true, names.any { "Fly" in it })
    }

    @Test
    fun `attachment variants are found by their name`() {
        assertEquals(Variants.name("Lat Pulldown", "V-Bar"), ExerciseMatcher.best("Lat Pulldown · V-Bar", library, 1).first().name)
    }
}
