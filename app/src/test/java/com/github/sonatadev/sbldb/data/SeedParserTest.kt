package com.github.sonatadev.sbldb.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Parses the real YAML shipped in assets/, so broken content fails the build. */
class SeedParserTest {
    private fun asset(name: String) = File("src/main/assets/$name").inputStream()

    private val muscles = SeedParser.parseMuscles(asset("muscles.yaml"))
    private val actions = SeedParser.parseJointActions(asset("joint_actions.yaml"))
    private val exercises = SeedParser.parseExercises(asset("exercises.yaml"))
    // Parsing already fails when any basic or expert text is missing
    private val glossary = SeedParser.parseGlossary(asset("glossary.yaml"))
    private val knownMuscles = muscles.flatMap { m -> m.regions.map { MuscleRef(m.muscleGroup, it) } }.toSet()
    private val actionsByKey = actions.associateBy { it.key }

    @Test
    fun `content is not empty`() {
        assertTrue(muscles.isNotEmpty())
        assertTrue("${actions.size} actions", actions.size >= 30)
        assertTrue("${exercises.size} exercises", exercises.size >= 80)
    }

    @Test
    fun `every muscle used by a joint action exists`() {
        val missing = actions.flatMap { a -> (a.primary + a.secondary).filter { it !in knownMuscles }.map { "${a.key}: $it" } }
        assertTrue("Unknown muscles: $missing", missing.isEmpty())
    }

    @Test
    fun `every joint action has a prime mover`() {
        assertTrue(actions.filter { it.primary.isEmpty() }.map { it.key }.toString(), actions.all { it.primary.isNotEmpty() })
    }

    @Test
    fun `every action used by an exercise exists and ratings are 1 to 5`() {
        val problems = exercises.flatMap { e ->
            e.actions.mapNotNull { (key, rating) ->
                when {
                    key !in actionsByKey -> "${e.name}: unknown action '$key'"
                    rating !in 1..5 -> "${e.name}: rating $rating for '$key'"
                    else -> null
                }
            }
        }
        assertTrue(problems.toString(), problems.isEmpty())
    }

    @Test
    fun `every joint action is trained by at least one exercise`() {
        val used = exercises.flatMap { it.actions.keys }.toSet()
        val unused = actions.map { it.key }.filter { it !in used }
        assertTrue("No exercise for: $unused", unused.isEmpty())
    }

    @Test
    fun `muscle overrides and derived muscles only use known muscles`() {
        val missing = exercises.flatMap { e -> MuscleDerivation.derive(e, actionsByKey).keys.filter { it !in knownMuscles }.map { "${e.name}: $it" } }
        assertTrue("Unknown muscles: $missing", missing.isEmpty())
    }

    @Test
    fun `every exercise has at least one primary muscle`() {
        val none = exercises.filter { e -> MuscleDerivation.derive(e, actionsByKey).values.none { it == com.github.sonatadev.sbldb.data.entity.Role.PRIMARY } }
        // Isometric or low-rated movements may legitimately have no primary muscle
        val allowed = setOf("Plank", "Farmer's Carry", "Pallof Press", "Jefferson Curl")
        assertTrue(none.map { it.name }.filter { it !in allowed }.toString(), none.all { it.name in allowed })
    }

    @Test
    fun `every joint action has a valid animation`() {
        actions.forEach { a ->
            val animation = a.animation
            assertTrue("${a.key} has no animation", animation != null)
            assertEquals("${a.key}: ${animation!!.problem()}", null, animation.problem())
        }
    }

    @Test
    fun `glossary has unique terms`() {
        assertTrue(glossary.size >= 10)
        assertTrue(glossary.map { it.term.lowercase() }.let { it.size == it.toSet().size })
    }

    @Test
    fun `every muscle of a joint action is explained and listed once`() {
        val problems = actions.flatMap { a ->
            a.muscles.groupBy { it.muscle }.filter { it.value.size > 1 }.map { "${a.key}: ${it.key} listed twice" }
        }
        assertTrue(problems.toString(), problems.isEmpty())
    }

    @Test
    fun `aliases never point to a different exercise's name`() {
        val names = exercises.map { it.name.lowercase() }.toSet()
        val clashes = exercises.flatMap { e -> e.aliases.filter { it.lowercase() in names && !it.equals(e.name, true) }.map { "${e.name}: $it" } }
        assertTrue("Aliases equal to another exercise: $clashes", clashes.isEmpty())
    }

    @Test
    fun `chest-supported rows have an unsupported counterpart`() {
        val names = exercises.map { it.name }.toSet()
        val pairs = mapOf(
            "Seal Row" to "Barbell Row",
            "Chest-Supported Dumbbell Row" to "Bent-Over Dumbbell Row",
            "Chest-Supported T-Bar Row" to "T-Bar Row",
            "Chest-Supported Machine Row" to "Seated Cable Row",
            "Chest-Supported Wide-Grip Machine Row" to "Wide-Grip Cable Row",
            "Chest-Supported Rear Delt Row" to "Bent-Over Rear Delt Row",
            "Chest-Supported Reverse Fly" to "Bent-Over Dumbbell Reverse Fly",
            "Kelso Shrug" to "Bent-Over Kelso Shrug",
            "Incline Dumbbell Y-Raise" to "Standing Dumbbell Y-Raise"
        )
        val missing = pairs.flatMap { (a, b) -> listOf(a, b) }.filter { it !in names }
        assertTrue("Missing: $missing", missing.isEmpty())
    }

    @Test
    fun `names are unique`() {
        assertTrue(exercises.map { it.name }.let { it.size == it.toSet().size })
        assertTrue(actions.map { it.key }.let { it.size == it.toSet().size })
        assertTrue(knownMuscles.size == muscles.sumOf { it.regions.size })
    }
}
