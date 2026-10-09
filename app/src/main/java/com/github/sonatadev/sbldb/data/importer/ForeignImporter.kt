package com.github.sonatadev.sbldb.data.importer

import androidx.room.withTransaction
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.entity.SetType
import com.github.sonatadev.sbldb.data.entity.Workout
import com.github.sonatadev.sbldb.data.entity.WorkoutExercise
import com.github.sonatadev.sbldb.data.entity.WorkoutSet
import com.github.sonatadev.sbldb.domain.WeightUnit

data class ImportResult(val workouts: Int, val skipped: Int, val sets: Int)

/** Writes a log from Strong or Hevy into the database, next to what is already there. */
object ForeignImporter {
    /** The library as the matcher sees it: every visible exercise and attachment variant. */
    suspend fun candidates(db: AppDatabase): List<MatchCandidate> =
        db.exerciseDAO().allExercises().filter { !it.isArchived }.map { MatchCandidate(it.exerciseId, it.name, it.equipment, it.aliasList) }

    /**
     * Adds every workout of [log] that isn't there yet (same start time), with the exercises
     * [mapping] gives an id; unmapped exercises are left out. Weights are in [unit] unless the file
     * says kg. Seconds go in the reps field for timed exercises, as the app logs holds.
     */
    suspend fun import(db: AppDatabase, log: ForeignLog, mapping: Map<String, Int?>, unit: WeightUnit): ImportResult = db.withTransaction {
        val dao = db.workoutDAO()
        val exercises = db.exerciseDAO().allExercises().associateBy { it.exerciseId }
        val toKg: (Double) -> Double = if (log.weightIsKg == true) { kg -> kg } else unit::toKg
        var added = 0
        var skipped = 0
        var sets = 0
        for (w in log.workouts) {
            val kept = w.exercises.filter { mapping[it.name] != null }
            if (kept.isEmpty()) continue
            if (dao.countStartedAt(w.startedAt) > 0) {
                skipped++
                continue
            }
            val workoutId = dao.insertWorkout(Workout(name = w.name, startedAt = w.startedAt, endedAt = w.endedAt, notes = w.notes))
            kept.forEachIndexed { position, e ->
                val exerciseId = mapping.getValue(e.name)!!
                val timed = exercises[exerciseId]?.isTimed == true
                val weId = dao.insertWorkoutExercise(WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = position, note = e.note))
                e.sets.forEachIndexed { i, s ->
                    dao.insertSet(
                        WorkoutSet(
                            workoutExerciseId = weId,
                            position = i,
                            weightKg = s.weight?.let(toKg),
                            reps = if (timed) s.seconds ?: s.reps else s.reps ?: s.seconds,
                            rir = s.rir,
                            isWarmup = s.type == SetType.WARMUP,
                            isCompleted = true,
                            setType = s.type
                        )
                    )
                    sets++
                }
            }
            added++
        }
        ImportResult(added, skipped, sets)
    }
}
