package com.github.sonatadev.sbldb.data.repository

import com.github.sonatadev.sbldb.domain.WarmupSet
import androidx.room.withTransaction
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.entity.SetHistoryRow
import com.github.sonatadev.sbldb.data.entity.VolumeRow
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.SetType
import com.github.sonatadev.sbldb.domain.ExerciseSwap
import kotlinx.coroutines.flow.first
import com.github.sonatadev.sbldb.data.entity.Workout
import com.github.sonatadev.sbldb.data.entity.WorkoutExercise
import com.github.sonatadev.sbldb.data.entity.WorkoutSet
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class WorkoutRepository(
    private val db: AppDatabase,
    /** Name for a new workout by time of day; the app passes translated strings. */
    private val workoutName: (PartOfDay) -> String = { "${it.name.lowercase().replaceFirstChar(Char::uppercase)} workout" }
) {
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

    /**
     * Adds the exercise with one set, pre-filled from the last performance if available.
     * In a finished workout (editing history) the set is already marked as done.
     */
    suspend fun addExercise(workoutId: Long, exerciseId: Int) = db.withTransaction {
        val finished = dao.findWorkout(workoutId)?.endedAt != null
        val position = dao.nextExercisePosition(workoutId)
        val workoutExerciseId = dao.insertWorkoutExercise(WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = position))
        val last = dao.getLastPerformance(exerciseId).firstOrNull()
        dao.insertSet(
            WorkoutSet(workoutExerciseId = workoutExerciseId, position = 0, weightKg = last?.weightKg, reps = last?.reps, isCompleted = finished)
        )
    }

    /** Moves a finished workout in time; [durationMillis] keeps the end after the start. */
    suspend fun updateTimes(workout: Workout, startedAt: Long, durationMillis: Long) =
        dao.updateWorkout(workout.copy(startedAt = startedAt, endedAt = startedAt + durationMillis.coerceAtLeast(0)))

    suspend fun removeExercise(workoutExercise: WorkoutExercise) = dao.deleteWorkoutExercise(workoutExercise)

    suspend fun setWorkoutNote(workoutExercise: WorkoutExercise, text: String) =
        dao.updateWorkoutExercise(workoutExercise.copy(note = text.trim().ifEmpty { null }))

    /** Replaces the exercise but keeps its sets, for when the machine is taken. */
    suspend fun swapExercise(workoutExercise: WorkoutExercise, exerciseId: Int) =
        dao.updateWorkoutExercise(workoutExercise.copy(exerciseId = exerciseId))

    /** Up to [limit] replacements for [exerciseId], closest first. */
    suspend fun swapCandidates(exerciseId: Int, limit: Int = 10): List<Exercise> {
        val ratings = db.jointActionDAO().allExerciseLinks()
            .groupBy({ it.exerciseId }, { it.jointActionId to it.rating })
            .mapValues { it.value.toMap() }
        val ids = ExerciseSwap.rank(exerciseId, ratings, limit)
        val exercises = db.exerciseDAO()
        return ids.mapNotNull { exercises.getExercise(it).first() }
    }

    /** Adds a set copying weight and reps from [previous], so repeating a set is one tap. */
    suspend fun addSet(workoutExerciseId: Long, previous: WorkoutSet?, completed: Boolean = false) = db.withTransaction {
        dao.insertSet(
            WorkoutSet(
                workoutExerciseId = workoutExerciseId,
                position = dao.nextSetPosition(workoutExerciseId),
                weightKg = previous?.weightKg,
                reps = previous?.reps,
                rir = previous?.rir,
                isCompleted = completed
            )
        )
    }

    suspend fun updateWeight(setId: Long, weightKg: Double?) = dao.updateWeight(setId, weightKg)

    /** Puts warm-up sets before the existing ones. */
    suspend fun addWarmups(workoutExerciseId: Long, warmups: List<WarmupSet>) = db.withTransaction {
        dao.shiftSets(workoutExerciseId, warmups.size)
        warmups.forEachIndexed { i, w ->
            dao.insertSet(
                WorkoutSet(
                    workoutExerciseId = workoutExerciseId,
                    position = i,
                    weightKg = w.weightKg,
                    reps = w.reps,
                    isWarmup = true,
                    setType = SetType.WARMUP
                )
            )
        }
    }

    /** Sets load and reps on several sets at once (progression suggestion). */
    suspend fun prefill(setIds: Collection<Long>, weightKg: Double?, reps: Int) = db.withTransaction {
        setIds.forEach { id ->
            dao.updateWeight(id, weightKg)
            dao.updateReps(id, reps)
        }
    }

    suspend fun updateReps(setId: Long, reps: Int?) = dao.updateReps(setId, reps)

    suspend fun updateRir(setId: Long, rir: Int?) = dao.updateRir(setId, rir)

    suspend fun updateCompleted(setId: Long, completed: Boolean) = dao.updateCompleted(setId, completed)

    suspend fun updateWarmup(setId: Long, warmup: Boolean) = dao.updateWarmup(setId, warmup)

    suspend fun updateSetType(setId: Long, type: SetType) = dao.updateSetType(setId, type.name)

    suspend fun deleteSet(set: WorkoutSet) = dao.deleteSet(set)

    suspend fun lastPerformance(exerciseId: Int): List<SetHistoryRow> = dao.getLastPerformance(exerciseId)

    private fun defaultName(millis: Long): String {
        val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        val part = when (time.hour) {
            in 5..11 -> PartOfDay.MORNING
            in 12..17 -> PartOfDay.AFTERNOON
            else -> PartOfDay.EVENING
        }
        return workoutName(part) + " · " + time.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
    }

    enum class PartOfDay { MORNING, AFTERNOON, EVENING }
}
