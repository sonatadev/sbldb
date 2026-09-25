package com.github.sonatadev.sbldb.domain

import com.github.sonatadev.sbldb.data.entity.Role

/** One exercise slot of a routine: how many sets it plans. */
data class PlannedSlot(val exerciseId: Int, val sets: Int)

/** A routine and how often it is done in a week. */
data class PlannedRoutine(val timesPerWeek: Int, val slots: List<PlannedSlot>)

/**
 * Planned weekly sets per muscle group, with the same fractional rules as logged volume:
 * each set counts once per group with the exercise's highest role there (1 or 0.5).
 */
object WeeklyPlan {
    fun setsPerGroup(routines: List<PlannedRoutine>, roles: Map<Int, List<Pair<String, Role>>>): Map<String, Double> {
        val totals = mutableMapOf<String, Double>()
        routines.forEach { routine ->
            routine.slots.forEach { slot ->
                roles[slot.exerciseId].orEmpty()
                    .groupBy({ it.first }, { VolumeCalculator.weight(it.second) })
                    .forEach { (group, weights) ->
                        totals.merge(group, weights.max() * slot.sets * routine.timesPerWeek, Double::plus)
                    }
            }
        }
        return totals
    }
}
