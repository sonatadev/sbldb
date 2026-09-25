package com.github.sonatadev.sbldb.domain

import com.github.sonatadev.sbldb.domain.Suggestion.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressionTest {
    private fun sets(vararg reps: Int, kg: Double? = 80.0, rir: Int? = 2) = reps.map { PastSet(kg, it, rir) }

    @Test
    fun noHistoryGivesNoSuggestion() {
        assertNull(Progression.suggest(emptyList(), "Barbell", WeightUnit.KG))
    }

    @Test
    fun allSetsAtTopOfRangeAddsLoadAndResetsReps() {
        val s = Progression.suggest(sets(12, 12, 12), "Barbell", WeightUnit.KG)!!
        assertEquals(Kind.ADD_LOAD, s.kind)
        assertEquals(82.5, s.weightKg!!, 0.001)
        assertEquals(8, s.reps)
    }

    @Test
    fun oneSetShortAddsARepToTheWeakest() {
        val s = Progression.suggest(sets(12, 11, 9), "Barbell", WeightUnit.KG)!!
        assertEquals(Kind.ADD_REP, s.kind)
        assertEquals(80.0, s.weightKg!!, 0.001)
        assertEquals(10, s.reps)
    }

    @Test
    fun usesTheRoutineRange() {
        val s = Progression.suggest(sets(8, 8, 8), "Machine", WeightUnit.KG, repMin = 6, repMax = 8)!!
        assertEquals(Kind.ADD_LOAD, s.kind)
        assertEquals(6, s.reps)
    }

    @Test
    fun grindingPastTheTargetEffortDoesNotEarnMoreLoad() {
        // Target RIR 2 but every set went to failure: repeat the load
        val s = Progression.suggest(sets(12, 12, 12, rir = 0), "Barbell", WeightUnit.KG, targetRir = 2)!!
        assertEquals(Kind.ADD_REP, s.kind)
        assertEquals(12, s.reps)
    }

    @Test
    fun dumbbellsJumpTwoKilos() {
        val s = Progression.suggest(sets(12, 12, kg = 20.0), "Dumbbell", WeightUnit.KG)!!
        assertEquals(22.0, s.weightKg!!, 0.001)
    }

    @Test
    fun poundsJumpFiveAndRoundToTheStep() {
        // 100 kg ≈ 220.46 lb → 225 lb
        val s = Progression.suggest(sets(12, 12, kg = 100.0), "Barbell", WeightUnit.LB)!!
        assertEquals(225.0, WeightUnit.LB.fromKg(s.weightKg!!), 0.001)
    }

    @Test
    fun onlyTheHeaviestSetsDecide() {
        // A lighter back-off set short of the range does not block progression
        val last = sets(12, 12) + PastSet(70.0, 9, 2)
        assertEquals(Kind.ADD_LOAD, Progression.suggest(last, "Barbell", WeightUnit.KG)!!.kind)
    }

    @Test
    fun bodyweightAtTopSuggestsAHarderVariation() {
        val s = Progression.suggest(sets(12, 12, kg = null), "Bodyweight", WeightUnit.KG)!!
        assertEquals(Kind.HARDER_VARIATION, s.kind)
        assertNull(s.weightKg)
    }

    @Test
    fun assistedMachinesProgressByRemovingCounterweight() {
        val s = Progression.suggest(sets(12, 12, kg = 40.0), "Machine", WeightUnit.KG, assisted = true)!!
        assertEquals(Kind.LESS_ASSISTANCE, s.kind)
        assertEquals(37.5, s.weightKg!!, 0.001)
        assertEquals(true, Progression.isAssisted("Assisted Pull-Up Machine"))
    }
}
