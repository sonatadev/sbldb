package com.github.sonatadev.sbldb.data.repository

import androidx.room.withTransaction
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.entity.SetHistoryRow
import com.github.sonatadev.sbldb.data.entity.VolumeRow
import com.github.sonatadev.sbldb.data.entity.Workout
import com.github.sonatadev.sbldb.data.entity.WorkoutExercise
import com.github.sonatadev.sbldb.data.entity.WorkoutSet
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class WorkoutRepository(private val db: AppDatabase) {
    private val dao = db.workoutDAO()

    val activeWorkout: Flow<Workout?> = dao.getActiveWorkout()
    val history: Flow<List<WorkoutWithExercises>> = dao.getFinishedWorkouts()

    fun workout(workoutId: Long): Flow<WorkoutWithExercises?> = dao.getWorkoutWithExercises(workoutId)

    fun volumeRows(from: Long, to: Long): Flow<List<VolumeRow>> = dao.getVolumeRows(from, to)

    fun finishedBetween(from: Long, to: Long): Flow<List<WorkoutWithExercises>> = dao.getFinishedBetween(from, to)

    /**
     * Starts a workout from a routine: one exercise per routine entry, each with its planned sets
     * pre-filled with last time's weight and the top of the rep range (RIR is left for the lifter). Resumes instead if a workout
     * is already in progress.
     */
    suspend fun startFromRoutine(routineId: Long): Long = db.withTransaction {
        dao.findActiveWorkout()?.let { return@withTransaction it.workoutId }
        val routine = requireNotNull(db.routineDAO().find(routineId)) { "Routine $routineId not found" }
        val workoutId = dao.insertWorkout(
            Workout(name = routine.routine.name, startedAt = System.currentTimeMillis(), routineId = routineId)
        )
        routine.exercises.sortedBy { it.routineExercise.position }.forEachIndexed { position, entry ->
            val planned = entry.routineExercise
            val workoutExerciseId = dao.insertWorkoutExercise(
                WorkoutExercise(workoutId = workoutId, exerciseId = planned.exerciseId, position = position)
            )
            val lastWeight = dao.getLastPerformance(planned.exerciseId).firstOrNull()?.weightKg
            repeat(planned.sets.coerceAtLeast(1)) { index ->
                dao.insertSet(
                    WorkoutSet(
                        workoutExerciseId = workoutExerciseId,
                        position = index,
                        weightKg = lastWeight,
                        reps = planned.repMax
                    )
                )
            }
        }
        workoutId
    }

    /** Returns the id of the workout in progress, creating one if there is none. */
    suspend fun startOrResume(): Long = db.withTransaction {
        dao.findActiveWorkout()?.workoutId ?: run {
            val now = System.currentTimeMillis()
            dao.insertWorkout(Workout(name = defaultName(now), startedAt = now))
        }
    }

    suspend fun rename(workout: Workout, name: String) = dao.updateWorkout(workout.copy(name = name))

    /** Drops unfinished sets and empty exercises; deletes the workout if nothing is left. */
    suspend fun finish(workout: Workout) = db.withTransaction {
        dao.deleteUncompletedSets(workout.workoutId)
        dao.deleteEmptyExercises(workout.workoutId)
        if (dao.nextExercisePosition(workout.workoutId) == 0) {
            dao.deleteWorkout(workout)
        } else {
            dao.updateWorkout(workout.copy(endedAt = System.currentTimeMillis()))
        }
    }

    suspend fun delete(workout: Workout) = dao.deleteWorkout(workout)

    /** Adds the exercise with one empty set, pre-filled from the last performance if available. */
    suspend fun addExercise(workoutId: Long, exerciseId: Int) = db.withTransaction {
        val position = dao.nextExercisePosition(workoutId)
        val workoutExerciseId = dao.insertWorkoutExercise(WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = position))
        val last = dao.getLastPerformance(exerciseId).firstOrNull()
        dao.insertSet(WorkoutSet(workoutExerciseId = workoutExerciseId, position = 0, weightKg = last?.weightKg, reps = last?.reps))
    }

    suspend fun removeExercise(workoutExercise: WorkoutExercise) = dao.deleteWorkoutExercise(workoutExercise)

    /** Adds a set copying weight and reps from [previous], so repeating a set is one tap. */
    suspend fun addSet(workoutExerciseId: Long, previous: WorkoutSet?) = db.withTransaction {
        dao.insertSet(
            WorkoutSet(
                workoutExerciseId = workoutExerciseId,
                position = dao.nextSetPosition(workoutExerciseId),
                weightKg = previous?.weightKg,
                reps = previous?.reps,
                rir = previous?.rir
            )
        )
    }

    suspend fun updateWeight(setId: Long, weightKg: Double?) = dao.updateWeight(setId, weightKg)

    suspend fun updateReps(setId: Long, reps: Int?) = dao.updateReps(setId, reps)

    suspend fun updateRir(setId: Long, rir: Int?) = dao.updateRir(setId, rir)

    suspend fun updateCompleted(setId: Long, completed: Boolean) = dao.updateCompleted(setId, completed)

    suspend fun updateWarmup(setId: Long, warmup: Boolean) = dao.updateWarmup(setId, warmup)

    suspend fun deleteSet(set: WorkoutSet) = dao.deleteSet(set)

    suspend fun lastPerformance(exerciseId: Int): List<SetHistoryRow> = dao.getLastPerformance(exerciseId)

    private fun defaultName(millis: Long): String {
        val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        val part = when (time.hour) {
            in 5..11 -> "Morning"
            in 12..17 -> "Afternoon"
            else -> "Evening"
        }
        return "$part workout · " + time.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH))
    }
}
