package com.github.sonatadev.sbldb.domain

import com.github.sonatadev.sbldb.data.entity.PlannedRoutine
import com.github.sonatadev.sbldb.data.entity.RoutineWithExercises
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Which routine is up next, for the Home screen and the widget. */
object NextRoutine {
    data class Pick(val routine: RoutineWithExercises, val isPlanned: Boolean)

    /**
     * A routine planned for today wins until it has been done; otherwise the one done least
     * recently (never-done routines first). Routines without exercises are never picked.
     */
    fun pick(
        routines: List<RoutineWithExercises>,
        history: List<WorkoutWithExercises>,
        plannedToday: List<PlannedRoutine>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Pick? {
        val usable = routines.filter { it.exercises.isNotEmpty() }
        val lastDone = history.mapNotNull { w -> w.workout.routineId?.let { it to w.workout.startedAt } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.max() }
        val doneToday = history.filter { Instant.ofEpochMilli(it.workout.startedAt).atZone(zone).toLocalDate() == today }
            .mapNotNull { it.workout.routineId }.toSet()
        plannedToday.firstOrNull { it.routineId !in doneToday }
            ?.let { p -> usable.firstOrNull { it.routine.routineId == p.routineId } }
            ?.let { return Pick(it, isPlanned = true) }
        return usable.minByOrNull { lastDone[it.routine.routineId] ?: Long.MIN_VALUE }?.let { Pick(it, isPlanned = false) }
    }
}
