package com.github.sonatadev.sbldb.data.repository

import java.time.LocalTime
import java.time.LocalDate
import com.github.sonatadev.sbldb.data.entity.WorkoutExerciseWithSets
import com.github.sonatadev.sbldb.data.entity.Side
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
            val last = dao.getLastPerformance(planned.exerciseId)
            val sides = sidesOf(planned.exerciseId)
            var position = 0
            repeat(planned.sets.coerceAtLeast(1)) {
                sides.forEach { side ->
                    dao.insertSet(
                        WorkoutSet(
                            workoutExerciseId = workoutExerciseId,
                            position = position++,
                            weightKg = (last.firstOrNull { it.side == side } ?: last.firstOrNull())?.weightKg,
                            reps = planned.repMax,
                            side = side
                        )
                    )
                }
            }
        }
        workoutId
    }

    /**
     * Logs a workout done on [day] without having tracked it: a finished workout at [timeOfDay]
     * lasting an hour, with [routineId]'s exercises and sets already done at last time's load and
     * the top of the rep range, ready to be corrected. Without a routine it starts empty.
     */
    suspend fun logPast(
        day: LocalDate,
        routineId: Long?,
        timeOfDay: LocalTime = LocalTime.of(18, 0),
        zone: ZoneId = ZoneId.systemDefault()
    ): Long = db.withTransaction {
        val start = day.atTime(timeOfDay).atZone(zone).toInstant().toEpochMilli()
        val routine = routineId?.let { db.routineDAO().find(it) }
        val workoutId = dao.insertWorkout(
            Workout(
                name = routine?.routine?.name ?: defaultName(start),
                startedAt = start,
                endedAt = start + 60 * 60_000L,
                routineId = routine?.routine?.routineId
            )
        )
        routine?.exercises?.sortedBy { it.routineExercise.position }?.forEachIndexed { position, entry ->
            val planned = entry.routineExercise
            val workoutExerciseId = dao.insertWorkoutExercise(WorkoutExercise(workoutId = workoutId, exerciseId = planned.exerciseId, position = position))
            val last = dao.getLastPerformance(planned.exerciseId)
            var setPosition = 0
            repeat(planned.sets.coerceAtLeast(1)) {
                sidesOf(planned.exerciseId).forEach { side ->
                    dao.insertSet(
                        WorkoutSet(
                            workoutExerciseId = workoutExerciseId,
                            position = setPosition++,
                            weightKg = (last.firstOrNull { it.side == side } ?: last.firstOrNull())?.weightKg,
                            reps = planned.repMax,
                            rir = planned.targetRir,
                            isCompleted = true,
                            side = side
                        )
                    )
                }
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

    /** Drops unfinished sets and empty exercises; deletes the workout if nothing is left. Returns whether it was kept. */
    suspend fun finish(workout: Workout): Boolean = db.withTransaction {
        dao.deleteUncompletedSets(workout.workoutId)
        dao.deleteEmptyExercises(workout.workoutId)
        if (dao.nextExercisePosition(workout.workoutId) == 0) {
            dao.deleteWorkout(workout)
            false
        } else {
            dao.updateWorkout(workout.copy(endedAt = System.currentTimeMillis()))
            true
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
        val last = dao.getLastPerformance(exerciseId)
        sidesOf(exerciseId).forEachIndexed { i, side ->
            val previous = last.firstOrNull { it.side == side } ?: last.firstOrNull()
            dao.insertSet(
                WorkoutSet(
                    workoutExerciseId = workoutExerciseId, position = i, weightKg = previous?.weightKg, reps = previous?.reps,
                    isCompleted = finished, side = side
                )
            )
        }
    }

    /** A set per side for one-sided exercises (left, then right), a single set otherwise. */
    private suspend fun sidesOf(exerciseId: Int): List<Side?> =
        if (db.exerciseDAO().findById(exerciseId)?.isUnilateral == true) listOf(Side.LEFT, Side.RIGHT) else listOf(null)

    /** Moves a finished workout in time; [durationMillis] keeps the end after the start. */
    suspend fun updateTimes(workout: Workout, startedAt: Long, durationMillis: Long) =
        dao.updateWorkout(workout.copy(startedAt = startedAt, endedAt = startedAt + durationMillis.coerceAtLeast(0)))

    suspend fun removeExercise(workoutExercise: WorkoutExercise) = dao.deleteWorkoutExercise(workoutExercise)

    /** Swaps [workoutExercise] with its neighbour [offset] places away in [ordered] (the workout's exercises in order). */
    suspend fun moveExercise(ordered: List<WorkoutExercise>, workoutExercise: WorkoutExercise, offset: Int) = db.withTransaction {
        val index = ordered.indexOfFirst { it.workoutExerciseId == workoutExercise.workoutExerciseId }
        if (index < 0 || ordered.getOrNull(index + offset) == null) return@withTransaction
        // Positions may repeat after edits: renumber, then swap the two
        val positions = ordered.indices.toMutableList().also { it[index] = index + offset; it[index + offset] = index }
        ordered.forEachIndexed { i, we -> if (we.position != positions[i]) dao.updateWorkoutExercise(we.copy(position = positions[i])) }
    }

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

    /**
     * Adds a set copying weight, reps and effort from the last working set, so repeating a set is
     * one tap. One-sided exercises get a left and a right set, each copying its own side.
     */
    suspend fun addSet(exercise: WorkoutExerciseWithSets, completed: Boolean = false) = db.withTransaction {
        val id = exercise.workoutExercise.workoutExerciseId
        val ordered = exercise.sets.sortedBy { it.position }
        val sides = if (exercise.exercise.isUnilateral) listOf(Side.LEFT, Side.RIGHT) else listOf(null)
        sides.forEach { side ->
            val previous = ordered.lastOrNull { !it.isWarmup && it.side == side } ?: ordered.lastOrNull()
            dao.insertSet(
                WorkoutSet(
                    workoutExerciseId = id,
                    position = dao.nextSetPosition(id),
                    weightKg = previous?.weightKg,
                    reps = previous?.reps,
                    rir = previous?.rir,
                    isCompleted = completed,
                    side = side
                )
            )
        }
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
