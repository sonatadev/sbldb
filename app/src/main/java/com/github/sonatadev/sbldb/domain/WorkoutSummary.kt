package com.github.sonatadev.sbldb.domain

import com.github.sonatadev.sbldb.data.entity.SetHistoryRow
import com.github.sonatadev.sbldb.data.entity.VolumeRow

/** A personal best set during the session: the records it beat and the set itself (its best e1RM). */
data class RecordHit(
    val exerciseId: Int,
    val name: String,
    val kinds: Set<PrKind>,
    val weightKg: Double?,
    val reps: Int?
)

/** A muscle trained in the session: its sets today and where its week stands after them. */
data class MuscleToday(val muscle: MuscleGroupVolume, val today: Double)

object WorkoutSummary {
    /**
     * Records beaten in workout [workoutId], one entry per exercise. [history] is each exercise's
     * full set history (any order within a workout is by position). Only straight sets count, and
     * counterweight machines only for reps, as in the workout screen.
     */
    fun records(history: Map<Pair<Int, String>, List<SetHistoryRow>>, workoutId: Long, startedAt: Long): List<RecordHit> =
        history.mapNotNull { (exercise, sets) ->
            val straight = sets.filter { it.isStraight }
            val before = PersonalRecords.of(
                straight.filter { it.workoutId != workoutId && it.startedAt < startedAt }.map { PastSet(it.weightKg, it.reps, it.rir) }
            )
            val session = straight.filter { it.workoutId == workoutId }
            val beaten = PersonalRecords.beatenInSession(before, session.map { PastSet(it.weightKg, it.reps, it.rir) })
                .let { kinds -> if (Progression.isAssisted(exercise.second)) kinds.map { it.intersect(setOf(PrKind.REPS)) } else kinds }
            val hits = session.zip(beaten).filter { it.second.isNotEmpty() }
            if (hits.isEmpty()) return@mapNotNull null
            val best = hits.maxBy { (set, _) -> OneRepMax.epley(set.weightKg, set.reps) ?: set.weightKg ?: 0.0 }.first
            RecordHit(exercise.first, exercise.second, hits.flatMap { it.second }.toSet(), best.weightKg, best.reps)
        }

    /**
     * Muscles the session trained, most sets first: sets from this workout, and the week's
     * total (this workout included) against each muscle's target zone.
     */
    fun muscles(weekRows: List<VolumeRow>, workoutId: Long, targets: Map<String, VolumeTarget>): List<MuscleToday> {
        val today = VolumeCalculator.calculate(weekRows.filter { it.workoutId == workoutId }).associate { it.muscleGroup to it.sets }
        val week = VolumeCalculator.calculate(weekRows, targets = targets).associateBy { it.muscleGroup }
        return today.filterValues { it > 0 }
            .mapNotNull { (group, sets) -> week[group]?.let { MuscleToday(it, sets) } }
            .sortedWith(compareByDescending<MuscleToday> { it.today }.thenBy { it.muscle.muscleGroup })
    }

    /** Weight × reps over the session's working sets, in kg. */
    fun tonnage(sets: List<Pair<Double?, Int?>>): Double = sets.sumOf { (kg, reps) -> (kg ?: 0.0) * (reps ?: 0) }
}
