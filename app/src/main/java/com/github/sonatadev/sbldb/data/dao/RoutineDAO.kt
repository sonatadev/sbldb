package com.github.sonatadev.sbldb.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.github.sonatadev.sbldb.data.entity.PlannedRoutine
import com.github.sonatadev.sbldb.data.entity.PlannedWorkout
import com.github.sonatadev.sbldb.data.entity.Routine
import com.github.sonatadev.sbldb.data.entity.RoutineExercise
import com.github.sonatadev.sbldb.data.entity.RoutineWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDAO {
    @Insert
    suspend fun insert(routine: Routine): Long

    @Insert
    suspend fun insertPlanned(planned: PlannedWorkout): Long

    @Query("DELETE FROM planned_workouts WHERE plannedId = :plannedId")
    suspend fun deletePlanned(plannedId: Long)

    @Query(
        """
        SELECT p.plannedId, p.date, p.routineId, r.name FROM planned_workouts p
        JOIN routines r ON r.routineId = p.routineId
        WHERE p.date BETWEEN :fromDay AND :toDay ORDER BY p.date, p.plannedId
        """
    )
    fun plannedBetween(fromDay: Long, toDay: Long): Flow<List<PlannedRoutine>>

    @Query("SELECT * FROM planned_workouts ORDER BY date")
    suspend fun allPlanned(): List<PlannedWorkout>

    @Query("DELETE FROM planned_workouts")
    suspend fun deleteAllPlanned()

    @Update
    suspend fun update(routine: Routine)

    @Delete
    suspend fun delete(routine: Routine)

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM routines")
    suspend fun nextPosition(): Int

    @Transaction
    @Query("SELECT * FROM routines ORDER BY position")
    fun getAll(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE routineId = :id")
    fun get(id: Long): Flow<RoutineWithExercises?>

    @Transaction
    @Query("SELECT * FROM routines WHERE routineId = :id")
    suspend fun find(id: Long): RoutineWithExercises?

    @Insert
    suspend fun insertExercise(exercise: RoutineExercise): Long

    @Update
    suspend fun updateExercise(exercise: RoutineExercise)

    @Update
    suspend fun updateExercises(exercises: List<RoutineExercise>)

    @Delete
    suspend fun deleteExercise(exercise: RoutineExercise)

    @Query("SELECT EXISTS(SELECT 1 FROM routine_exercises WHERE exerciseId = :exerciseId)")
    suspend fun usesExercise(exerciseId: Int): Boolean

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM routine_exercises WHERE routineId = :routineId")
    suspend fun nextExercisePosition(routineId: Long): Int
}
