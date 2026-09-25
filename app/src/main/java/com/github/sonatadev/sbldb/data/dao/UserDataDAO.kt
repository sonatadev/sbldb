package com.github.sonatadev.sbldb.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.github.sonatadev.sbldb.data.entity.BodyEntry
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.ExerciseNote
import com.github.sonatadev.sbldb.data.entity.MuscleTarget
import com.github.sonatadev.sbldb.data.entity.Role
import com.github.sonatadev.sbldb.data.entity.RoutineWithExercises
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

/** Everything that belongs to the user rather than to the bundled library. */
@Dao
interface UserDataDAO {
    // ---- backup reads ----
    @Transaction
    @Query("SELECT * FROM workouts ORDER BY startedAt")
    suspend fun allWorkouts(): List<WorkoutWithExercises>

    @Transaction
    @Query("SELECT * FROM routines ORDER BY position")
    suspend fun allRoutines(): List<RoutineWithExercises>

    @Query("SELECT * FROM exercises WHERE isCustom = 1 ORDER BY name")
    suspend fun customExercises(): List<Exercise>

    @Query(
        """
        SELECT ja.joint, ja.name, eja.rating FROM exercise_joint_actions eja
        JOIN joint_actions ja ON ja.jointActionId = eja.jointActionId
        WHERE eja.exerciseId = :exerciseId ORDER BY eja.rating DESC
        """
    )
    suspend fun actionRatings(exerciseId: Int): List<ActionRating>

    @Query("SELECT * FROM exercise_notes")
    suspend fun allExerciseNotes(): List<ExerciseNote>

    @Query("SELECT * FROM body_entries ORDER BY date")
    suspend fun allBodyEntries(): List<BodyEntry>

    @Query("SELECT * FROM muscle_targets")
    suspend fun allTargets(): List<MuscleTarget>

    @Query("SELECT exerciseId, name FROM exercises")
    suspend fun exerciseNames(): List<ExerciseName>

    // ---- wiping user data before an import (library rows stay) ----
    @Query("DELETE FROM workouts")
    suspend fun deleteWorkouts()

    @Query("DELETE FROM routines")
    suspend fun deleteRoutines()

    @Query("DELETE FROM exercise_notes")
    suspend fun deleteExerciseNotes()

    @Query("DELETE FROM body_entries")
    suspend fun deleteBodyEntries()

    @Query("DELETE FROM muscle_targets")
    suspend fun deleteTargets()

    @Query("DELETE FROM exercises WHERE isCustom = 1")
    suspend fun deleteCustomExercises()

    // ---- writes ----
    @Upsert
    suspend fun upsertExerciseNote(note: ExerciseNote)

    @Query("DELETE FROM exercise_notes WHERE exerciseId = :exerciseId")
    suspend fun deleteExerciseNote(exerciseId: Int)

    @Query("SELECT * FROM exercise_notes WHERE exerciseId = :exerciseId")
    fun exerciseNote(exerciseId: Int): Flow<ExerciseNote?>

    @Insert
    suspend fun insertBodyEntry(entry: BodyEntry): Long

    @Query("DELETE FROM body_entries WHERE id = :id")
    suspend fun deleteBodyEntry(id: Long)

    @Query("SELECT * FROM body_entries ORDER BY date DESC")
    fun bodyEntries(): Flow<List<BodyEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTarget(target: MuscleTarget)

    @Query("DELETE FROM muscle_targets WHERE muscleGroup = :group")
    suspend fun deleteTarget(group: String)

    @Query("SELECT * FROM muscle_targets")
    fun targets(): Flow<List<MuscleTarget>>

    /** Muscles of the given joint actions with their role, for deriving a custom exercise's muscles. */
    @Query("SELECT jointActionId, muscleId, role FROM joint_action_muscles WHERE jointActionId IN (:actionIds)")
    suspend fun actionMuscles(actionIds: List<Int>): List<ActionMuscleLink>

    @Query("SELECT jointActionId FROM joint_actions WHERE joint = :joint AND name = :name")
    suspend fun actionId(joint: String, name: String): Int?

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE exerciseId = :exerciseId")
    suspend fun timesLogged(exerciseId: Int): Int
}

data class ActionRating(val joint: String, val name: String, val rating: Int)

data class ExerciseName(val exerciseId: Int, val name: String)

data class ActionMuscleLink(val jointActionId: Int, val muscleId: Int, val role: Role)
