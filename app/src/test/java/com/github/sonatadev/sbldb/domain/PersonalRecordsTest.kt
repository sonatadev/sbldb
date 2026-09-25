package com.github.sonatadev.sbldb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRecordsTest {
    private val history = PersonalRecords.of(listOf(PastSet(100.0, 5, 2), PastSet(90.0, 8, 1), PastSet(80.0, 10, 2)))

    @Test
    fun recordsAreCollected() {
        assertEquals(100.0, history.heaviest!!, 0.0)
        assertEquals(116.67, history.bestE1rm!!, 0.01)
        assertEquals(8, history.repsAtWeight[90.0])
    }

    @Test
    fun heavierSetBeatsWeightAndE1rm() {
        assertEquals(setOf(PrKind.WEIGHT, PrKind.E1RM), PersonalRecords.beaten(PastSet(105.0, 5, 1), history))
    }

    @Test
    fun moreRepsAtAKnownLoadIsARepRecord() {
        assertEquals(setOf(PrKind.REPS), PersonalRecords.beaten(PastSet(80.0, 11, 1), history))
    }

    @Test
    fun matchingARecordIsNotBeatingIt() {
        assertTrue(PersonalRecords.beaten(PastSet(100.0, 5, 0), history).isEmpty())
    }

    @Test
    fun firstEverSessionHasNoRecords() {
        assertTrue(PersonalRecords.beaten(PastSet(60.0, 10, 2), Records()).isEmpty())
    }

    @Test
    fun laterSetsInTheSessionMustBeatEarlierOnes() {
        val result = PersonalRecords.beatenInSession(history, listOf(PastSet(105.0, 5, 1), PastSet(105.0, 5, 0), PastSet(105.0, 6, 0)))
        assertTrue(PrKind.WEIGHT in result[0])
        assertTrue(result[1].isEmpty())
        assertEquals(setOf(PrKind.E1RM, PrKind.REPS), result[2])
    }

    @Test
    fun bodyweightRepsCount() {
        val bw = PersonalRecords.of(listOf(PastSet(null, 12, 1)))
        assertEquals(setOf(PrKind.REPS), PersonalRecords.beaten(PastSet(null, 14, 1), bw))
    }
}
