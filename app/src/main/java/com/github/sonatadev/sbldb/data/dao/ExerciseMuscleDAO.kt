package com.github.sonatadev.sbldb.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.github.sonatadev.sbldb.data.entity.ExerciseMuscle
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseMuscleDAO {
    @Insert
    suspend fun insertExerciseMuscle(exerciseMuscle: ExerciseMuscle)

    @Query("SELECT * FROM exercise_muscles")
    fun getAllExerciseMuscle(): Flow<List<ExerciseMuscle>>

    @Query("DELETE FROM exercise_muscles WHERE exerciseId = :exerciseId")
    suspend fun deleteForExercise(exerciseId: Int)

    @Delete
    suspend fun deleteExerciseMuscle(exerciseMuscle: ExerciseMuscle)
}