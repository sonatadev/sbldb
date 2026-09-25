package com.github.sonatadev.sbldb.domain

import kotlin.math.roundToLong

/** One warm-up set, in kg. */
data class WarmupSet(val weightKg: Double, val reps: Int)

/**
 * A short ramp before the first working set: enough to rehearse the movement without adding
 * fatigue. Weights are rounded to the equipment's increment and never go below the empty bar.
 */
object Warmup {
    private val RAMP = listOf(0.4 to 8, 0.6 to 5, 0.8 to 3)
    const val BARBELL_KG = 20.0

    fun ramp(workKg: Double, equipment: String, unit: WeightUnit): List<WarmupSet> {
        val step = Progression.increment(equipment, unit)
        val floor = if (equipment.equals("Barbell", ignoreCase = true)) BARBELL_KG else 0.0
        val sets = RAMP.mapNotNull { (fraction, reps) ->
            val inUnit = (unit.fromKg(workKg * fraction) / step).roundToLong() * step
            val kg = maxOf(unit.toKg(inUnit), floor)
            WarmupSet(kg, reps).takeIf { kg > 0 && kg < workKg - 1e-6 }
        }
        // Light loads round to the same weight: keep the heaviest reps count only once per weight
        return sets.distinctBy { it.weightKg }
    }
}
