package com.github.sonatadev.sbldb.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.github.sonatadev.sbldb.data.entity.SetHistoryRow
import com.github.sonatadev.sbldb.data.entity.VolumeRow
import com.github.sonatadev.sbldb.data.entity.Workout
import com.github.sonatadev.sbldb.data.entity.WorkoutExercise
import com.github.sonatadev.sbldb.data.entity.WorkoutSet
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDAO {
    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Delete
    suspend fun deleteWorkout(workout: Workout)

    @Query("SELECT * FROM workouts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun getActiveWorkout(): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun findActiveWorkout(): Workout?

    @Query("SELECT * FROM workouts WHERE workoutId = :workoutId")
    suspend fun findWorkout(workoutId: Long): Workout?

    @Transaction
    @Query("SELECT * FROM workouts WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun getFinishedWorkouts(): Flow<List<WorkoutWithExercises>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE workoutId = :workoutId")
    fun getWorkoutWithExercises(workoutId: Long): Flow<WorkoutWithExercises?>

    @Insert
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise): Long

    @Update
    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise)

    @Delete
    suspend fun deleteWorkoutExercise(workoutExercise: WorkoutExercise)

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun nextExercisePosition(workoutId: Long): Int

    @Insert
    suspend fun insertSet(set: WorkoutSet): Long

    @Delete
    suspend fun deleteSet(set: WorkoutSet)

    // Single-column updates, so fast edits of different fields never overwrite each other
    @Query("UPDATE workout_sets SET weightKg = :weightKg WHERE setId = :setId")
    suspend fun updateWeight(setId: Long, weightKg: Double?)

    @Query("UPDATE workout_sets SET reps = :reps WHERE setId = :setId")
    suspend fun updateReps(setId: Long, reps: Int?)

    @Query("UPDATE workout_sets SET rir = :rir WHERE setId = :setId")
    suspend fun updateRir(setId: Long, rir: Int?)

    @Query("UPDATE workout_sets SET isCompleted = :completed WHERE setId = :setId")
    suspend fun updateCompleted(setId: Long, completed: Boolean)

    @Query("UPDATE workout_sets SET isWarmup = :warmup, setType = CASE WHEN :warmup THEN 'WARMUP' ELSE 'NORMAL' END WHERE setId = :setId")
    suspend fun updateWarmup(setId: Long, warmup: Boolean)

    /** [type] is a [com.github.sonatadev.sbldb.data.entity.SetType] name; isWarmup is kept in sync. */
    @Query("UPDATE workout_sets SET setType = :type, isWarmup = (:type = 'WARMUP') WHERE setId = :setId")
    suspend fun updateSetType(setId: Long, type: String)

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun nextSetPosition(workoutExerciseId: Long): Int

    /** Removes sets that were never marked completed, used when finishing a workout. */
    @Query(
        """
        DELETE FROM workout_sets WHERE isCompleted = 0 AND workoutExerciseId IN
            (SELECT workoutExerciseId FROM workout_exercises WHERE workoutId = :workoutId)
        """
    )
    suspend fun deleteUncompletedSets(workoutId: Long)

    /** Removes exercises left without any set, used when finishing a workout. */
    @Query(
        """
        DELETE FROM workout_exercises WHERE workoutId = :workoutId AND workoutExerciseId NOT IN
            (SELECT workoutExerciseId FROM workout_sets)
        """
    )
    suspend fun deleteEmptyExercises(workoutId: Long)

    /** Completed working sets of the most recent finished workout that included the exercise. */
    @Query(
        """
        SELECT w.workoutId, w.startedAt, s.weightKg, s.reps, s.rir, s.setType FROM workout_sets s
        JOIN workout_exercises we ON we.workoutExerciseId = s.workoutExerciseId
        JOIN workouts w ON w.workoutId = we.workoutId
        WHERE we.exerciseId = :exerciseId AND s.isCompleted = 1 AND s.isWarmup = 0
            AND w.workoutId = (
                SELECT w2.workoutId FROM workouts w2
                JOIN workout_exercises we2 ON we2.workoutId = w2.workoutId
                JOIN workout_sets s2 ON s2.workoutExerciseId = we2.workoutExerciseId
                WHERE we2.exerciseId = :exerciseId AND w2.endedAt IS NOT NULL
                    AND s2.isCompleted = 1 AND s2.isWarmup = 0
                ORDER BY w2.startedAt DESC LIMIT 1
            )
        ORDER BY we.position, s.position
        """
    )
    suspend fun getLastPerformance(exerciseId: Int): List<SetHistoryRow>

    /** Finished workouts started within [from, to), for the calendar. */
    @Transaction
    @Query("SELECT * FROM workouts WHERE endedAt IS NOT NULL AND startedAt >= :from AND startedAt < :to ORDER BY startedAt")
    fun getFinishedBetween(from: Long, to: Long): Flow<List<WorkoutWithExercises>>

    /** One row per (completed working set × muscle hit by the exercise), within [from, to). */
    @Query(
        """
        SELECT s.setId, w.workoutId, w.startedAt, we.exerciseId, m.muscleGroup, m.muscleRegion, em.role, s.rir
        FROM workout_sets s
        JOIN workout_exercises we ON we.workoutExerciseId = s.workoutExerciseId
        JOIN workouts w ON w.workoutId = we.workoutId
        JOIN exercise_muscles em ON em.exerciseId = we.exerciseId
        JOIN muscles m ON m.muscleId = em.muscleId
        WHERE s.isCompleted = 1 AND s.isWarmup = 0 AND w.endedAt IS NOT NULL
            AND w.startedAt >= :from AND w.startedAt < :to
        """
    )
    fun getVolumeRows(from: Long, to: Long): Flow<List<VolumeRow>>
}
