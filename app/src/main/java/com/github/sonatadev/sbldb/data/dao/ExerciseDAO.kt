package com.github.sonatadev.sbldb.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Update
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.MuscleWithRole
import com.github.sonatadev.sbldb.data.entity.Role
import com.github.sonatadev.sbldb.data.entity.SetHistoryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDAO {
    @Insert
    suspend fun insertExercise(exercise: Exercise): Long

    @Query("SELECT exerciseId FROM exercises WHERE name = :name")
    suspend fun findId(name: String): Int?

    @Query("SELECT * FROM exercises WHERE name = :name")
    suspend fun findByName(name: String): Exercise?

    @Update
    suspend fun updateExercise(exercise: Exercise)

    /** The library list: attachment variants are reached through their exercise. */
    @Query("SELECT * FROM exercises WHERE isArchived = 0 AND parentId IS NULL ORDER BY name")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE exerciseId = :exerciseId")
    suspend fun findById(exerciseId: Int): Exercise?

    @Query("SELECT * FROM exercises WHERE parentId = :parentId AND attachment = :attachment")
    suspend fun findVariant(parentId: Int, attachment: String): Exercise?

    @Query("SELECT * FROM exercises WHERE parentId = :parentId")
    suspend fun variantsOf(parentId: Int): List<Exercise>

    @Query("SELECT * FROM exercises WHERE parentId IS NOT NULL AND attachment = :attachment")
    suspend fun variantsWith(attachment: String): List<Exercise>

    /** An exercise and its attachment variants, the exercise first. */
    @Query("SELECT * FROM exercises WHERE exerciseId = :familyId OR parentId = :familyId ORDER BY parentId IS NOT NULL, attachment")
    fun getFamily(familyId: Int): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises")
    suspend fun allExercises(): List<Exercise>

    /** Every attachment used on a cable exercise, the user's own included. */
    @Query("SELECT DISTINCT attachment FROM exercises WHERE attachment IS NOT NULL AND isArchived = 0 AND LOWER(equipment) = 'cable' ORDER BY attachment")
    suspend fun cableAttachments(): List<String>

    @Query("SELECT * FROM exercises WHERE exerciseId = :exerciseId")
    fun getExercise(exerciseId: Int): Flow<Exercise?>

    @Query(
        """
        SELECT m.*, em.role FROM exercise_muscles em
        JOIN muscles m ON m.muscleId = em.muscleId
        WHERE em.exerciseId = :exerciseId
        ORDER BY em.role, m.muscleGroup, m.muscleRegion
        """
    )
    fun getMusclesForExercise(exerciseId: Int): Flow<List<MuscleWithRole>>

    /** Exercise id → muscle group, for every PRIMARY muscle. Used to filter the exercise list. */
    @Query(
        """
        SELECT DISTINCT em.exerciseId, m.muscleGroup FROM exercise_muscles em
        JOIN muscles m ON m.muscleId = em.muscleId
        WHERE em.role = 'PRIMARY'
        """
    )
    fun getPrimaryGroups(): Flow<List<ExercisePrimaryGroup>>

    /** Completed working sets for an exercise across finished workouts, newest first. */
    @Query(
        """
        SELECT w.workoutId, w.startedAt, s.weightKg, s.reps, s.rir, s.setType, s.side FROM workout_sets s
        JOIN workout_exercises we ON we.workoutExerciseId = s.workoutExerciseId
        JOIN workouts w ON w.workoutId = we.workoutId
        WHERE we.exerciseId = :exerciseId AND s.isCompleted = 1 AND s.isWarmup = 0
            AND w.endedAt IS NOT NULL
        ORDER BY w.startedAt DESC, we.position, s.position
        """
    )
    fun getSetHistory(exerciseId: Int): Flow<List<SetHistoryRow>>

    /** Completed working sets of every exercise in finished workouts started from [from] on, oldest first. */
    @Query(
        """
        SELECT we.exerciseId, e.name, e.isTimed, w.workoutId, w.startedAt, s.weightKg, s.reps, s.rir, s.setType, s.side FROM workout_sets s
        JOIN workout_exercises we ON we.workoutExerciseId = s.workoutExerciseId
        JOIN workouts w ON w.workoutId = we.workoutId
        JOIN exercises e ON e.exerciseId = we.exerciseId
        WHERE s.isCompleted = 1 AND s.isWarmup = 0 AND w.endedAt IS NOT NULL AND w.startedAt >= :from
        ORDER BY w.startedAt, we.position, s.position
        """
    )
    fun getAllSetsSince(from: Long): Flow<List<ExerciseSetRow>>

    /** Muscle groups (with role) hit by each of the given exercises. */
    @Query(
        """
        SELECT em.exerciseId, m.muscleGroup, em.role FROM exercise_muscles em
        JOIN muscles m ON m.muscleId = em.muscleId
        WHERE em.exerciseId IN (:exerciseIds)
        """
    )
    suspend fun getMuscleGroups(exerciseIds: List<Int>): List<ExerciseMuscleGroup>

    @Delete
    suspend fun deleteExercise(exercise: Exercise)
}

/** A [SetHistoryRow] with the exercise it belongs to, for the progress overview. */
data class ExerciseSetRow(val exerciseId: Int, val name: String, val isTimed: Boolean, @Embedded val set: SetHistoryRow)

data class ExercisePrimaryGroup(val exerciseId: Int, val muscleGroup: String)

data class ExerciseMuscleGroup(val exerciseId: Int, val muscleGroup: String, val role: Role)
