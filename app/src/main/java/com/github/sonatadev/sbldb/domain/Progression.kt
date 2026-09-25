package com.github.sonatadev.sbldb.domain

import kotlin.math.roundToLong

/** One working set from the last session. */
data class PastSet(val weightKg: Double?, val reps: Int?, val rir: Int?)

/** What to aim for today, in kg. [weightKg] is null for sets without external load. */
data class Suggestion(val kind: Kind, val weightKg: Double?, val reps: Int) {
    enum class Kind {
        /** Every set reached the top of the range: add load and restart from the bottom. */
        ADD_LOAD,
        /** Same load, one more rep than the weakest set last time. */
        ADD_REP,
        /** Bodyweight at the top of the range: add load or pick a harder variation. */
        HARDER_VARIATION,
        /** Assisted machine at the top of the range: less counterweight makes it harder. */
        LESS_ASSISTANCE
    }
}

/**
 * Double progression: work up through the rep range at a fixed load, and add load once every
 * working set reaches the top of the range at (or easier than) the target effort.
 */
object Progression {
    const val DEFAULT_REP_MIN = 8
    const val DEFAULT_REP_MAX = 12

    /** Smallest sensible jump for the equipment, in the user's unit. */
    fun increment(equipment: String, unit: WeightUnit): Double = when (unit) {
        WeightUnit.LB -> 5.0
        WeightUnit.KG -> if (equipment.equals("Dumbbell", ignoreCase = true)) 2.0 else 2.5
    }

    fun suggest(
        last: List<PastSet>,
        equipment: String,
        unit: WeightUnit,
        repMin: Int = DEFAULT_REP_MIN,
        repMax: Int = DEFAULT_REP_MAX,
        targetRir: Int? = null,
        /** The load is a counterweight (assisted pull-up/dip): progress by lowering it. */
        assisted: Boolean = false
    ): Suggestion? {
        val working = last.filter { it.reps != null && it.reps > 0 }
        if (working.isEmpty()) return null
        // With assistance the hardest sets are the ones with the least counterweight
        val topWeight = if (assisted) working.minOf { it.weightKg ?: 0.0 } else working.maxOf { it.weightKg ?: 0.0 }
        val topSets = working.filter { (it.weightKg ?: 0.0) == topWeight }
        val load = topWeight.takeIf { it > 0.0 }

        // A set counts as "earned" when it hit the top of the range without grinding past the target effort
        val earned = topSets.all { set ->
            set.reps!! >= repMax && (targetRir == null || set.rir == null || set.rir >= targetRir - 1)
        }
        if (earned) {
            if (load == null) return Suggestion(Suggestion.Kind.HARDER_VARIATION, null, repMin)
            val step = increment(equipment, unit)
            if (assisted) {
                val next = roundTo(unit.fromKg(load) - step, step).coerceAtLeast(0.0)
                return Suggestion(Suggestion.Kind.LESS_ASSISTANCE, unit.toKg(next).takeIf { it > 0 }, repMin)
            }
            val next = roundTo(unit.fromKg(load) + step, step)
            return Suggestion(Suggestion.Kind.ADD_LOAD, unit.toKg(next), repMin)
        }
        val weakest = topSets.minOf { it.reps!! }
        return Suggestion(Suggestion.Kind.ADD_REP, load, (weakest + 1).coerceIn(repMin, repMax))
    }

    fun isAssisted(exerciseName: String): Boolean = exerciseName.contains("assisted", ignoreCase = true)

    /** Rounds [value] to the nearest multiple of [step]. */
    private fun roundTo(value: Double, step: Double): Double = (value / step).roundToLong() * step
}
