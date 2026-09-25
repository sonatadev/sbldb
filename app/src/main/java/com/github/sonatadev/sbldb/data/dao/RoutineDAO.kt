package com.github.sonatadev.sbldb.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.github.sonatadev.sbldb.data.entity.Routine
import com.github.sonatadev.sbldb.data.entity.RoutineExercise
import com.github.sonatadev.sbldb.data.entity.RoutineWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDAO {
    @Insert
    suspend fun insert(routine: Routine): Long

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

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM routine_exercises WHERE routineId = :routineId")
    suspend fun nextExercisePosition(routineId: Long): Int
}
