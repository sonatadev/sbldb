package com.github.sonatadev.sbldb.data.repository

import androidx.room.withTransaction
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.entity.Routine
import com.github.sonatadev.sbldb.data.entity.RoutineExercise
import com.github.sonatadev.sbldb.data.entity.RoutineWithExercises
import kotlinx.coroutines.flow.Flow
import com.github.sonatadev.sbldb.data.entity.PlannedRoutine
import com.github.sonatadev.sbldb.data.entity.PlannedWorkout
import java.time.LocalDate
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import com.github.sonatadev.sbldb.domain.PastSet
import com.github.sonatadev.sbldb.domain.RoutinePlan
import com.github.sonatadev.sbldb.domain.SlotPlan

class RoutineRepository(private val db: AppDatabase) {
    private val dao = db.routineDAO()

    val routines: Flow<List<RoutineWithExercises>> = dao.getAll()

    fun routine(id: Long): Flow<RoutineWithExercises?> = dao.get(id)

    /** Routines planned between two days (inclusive). */
    fun plannedBetween(from: LocalDate, to: LocalDate): Flow<List<PlannedRoutine>> = dao.plannedBetween(from.toEpochDay(), to.toEpochDay())

    suspend fun plan(date: LocalDate, routineId: Long) = dao.insertPlanned(PlannedWorkout(date = date.toEpochDay(), routineId = routineId))

    suspend fun unplan(plannedId: Long) = dao.deletePlanned(plannedId)

    suspend fun find(id: Long): RoutineWithExercises? = dao.find(id)

    suspend fun create(name: String): Long = db.withTransaction {
        dao.insert(Routine(name = name, position = dao.nextPosition()))
    }

    /** A new routine with the exercises of [workout], planned from the sets that were done. */
    suspend fun createFromWorkout(workout: WorkoutWithExercises): Long = db.withTransaction {
        val routineId = dao.insert(Routine(name = workout.workout.name.substringBefore(" · "), position = dao.nextPosition()))
        workout.exercises.sortedBy { it.workoutExercise.position }.forEachIndexed { i, exercise ->
            val working = exercise.sets.filter { it.isCompleted && !it.isWarmup }.map { PastSet(it.weightKg, it.reps, it.rir) }
            val plan = RoutinePlan.fromSets(working) ?: SlotPlan(3, 8, 12, 1)
            dao.insertExercise(
                RoutineExercise(
                    routineId = routineId,
                    exerciseId = exercise.exercise.exerciseId,
                    position = i,
                    sets = plan.sets,
                    repMin = plan.repMin,
                    repMax = plan.repMax,
                    targetRir = plan.targetRir
                )
            )
        }
        routineId
    }

    suspend fun rename(routine: Routine, name: String) = dao.update(routine.copy(name = name))

    suspend fun setTimesPerWeek(routine: Routine, times: Int) = dao.update(routine.copy(timesPerWeek = times.coerceIn(1, 7)))

    suspend fun delete(routine: Routine) = dao.delete(routine)

    suspend fun addExercise(routineId: Long, exerciseId: Int, jointActionId: Int? = null) = db.withTransaction {
        dao.insertExercise(
            RoutineExercise(routineId = routineId, exerciseId = exerciseId, position = dao.nextExercisePosition(routineId), jointActionId = jointActionId)
        )
    }

    /** Keeps the slot's sets, reps and movement but trains it with another exercise. */
    suspend fun replaceExercise(routineExerciseId: Long, exerciseId: Int, jointActionId: Int?) {
        val current = dao.findExercise(routineExerciseId) ?: return
        dao.updateExercise(current.copy(exerciseId = exerciseId, jointActionId = jointActionId ?: current.jointActionId))
    }

    suspend fun updateExercise(exercise: RoutineExercise) = dao.updateExercise(exercise)

    suspend fun removeExercise(exercise: RoutineExercise) = dao.deleteExercise(exercise)

    /** Swaps [exercise] with its neighbour in [ordered]; [offset] is -1 (up) or +1 (down). */
    suspend fun move(ordered: List<RoutineExercise>, exercise: RoutineExercise, offset: Int) {
        val index = ordered.indexOfFirst { it.routineExerciseId == exercise.routineExerciseId }
        val other = ordered.getOrNull(index + offset) ?: return
        dao.updateExercises(listOf(exercise.copy(position = other.position), other.copy(position = exercise.position)))
    }
}
