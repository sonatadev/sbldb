package com.github.sonatadev.sbldb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSwapTest {
    // Actions: 1 horizontal adduction, 2 elbow extension, 3 shoulder flexion, 4 elbow flexion
    private val ratings = mapOf(
        10 to mapOf(1 to 5, 2 to 3, 3 to 3), // barbell bench
        11 to mapOf(1 to 5, 2 to 3, 3 to 3), // dumbbell bench (twin)
        12 to mapOf(1 to 5),                  // cable fly
        13 to mapOf(2 to 5),                  // pushdown
        14 to mapOf(4 to 5),                  // curl
        15 to mapOf(1 to 4, 2 to 4, 3 to 4)   // dip-ish
    )

    @Test
    fun twinComesFirst() {
        assertEquals(11, ExerciseSwap.rank(10, ratings).first())
    }

    @Test
    fun exercisesWithoutTheMainActionAreLeftOut() {
        val ranked = ExerciseSwap.rank(10, ratings)
        assertFalse(14 in ranked)
        assertFalse(13 in ranked) // trains only a secondary action of the bench
        assertTrue(12 in ranked)
    }

    @Test
    fun closerProfileBeatsBroaderOne() {
        val ranked = ExerciseSwap.rank(12, ratings)
        // A fly swaps better with the bench presses than with the broad dip profile
        assertTrue(ranked.indexOf(10) < ranked.indexOf(15))
    }

    @Test
    fun unknownExerciseGivesNothing() {
        assertTrue(ExerciseSwap.rank(99, ratings).isEmpty())
    }
}
