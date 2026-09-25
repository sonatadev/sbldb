package com.github.sonatadev.sbldb.data.repository

import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.dao.ExerciseMuscleGroup
import com.github.sonatadev.sbldb.data.dao.ExercisePrimaryGroup
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.MuscleWithRole
import com.github.sonatadev.sbldb.data.entity.SetHistoryRow
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(db: AppDatabase) {
    private val exerciseDao = db.exerciseDAO()
    private val muscleDao = db.muscleDAO()

    val exercises: Flow<List<Exercise>> = exerciseDao.getAllExercises()
    val primaryGroups: Flow<List<ExercisePrimaryGroup>> = exerciseDao.getPrimaryGroups()
    val muscleGroups: Flow<List<String>> = muscleDao.getMuscleGroups()

    fun exercise(exerciseId: Int): Flow<Exercise?> = exerciseDao.getExercise(exerciseId)

    fun muscles(exerciseId: Int): Flow<List<MuscleWithRole>> = exerciseDao.getMusclesForExercise(exerciseId)

    suspend fun muscleGroups(exerciseIds: List<Int>): List<ExerciseMuscleGroup> = exerciseDao.getMuscleGroups(exerciseIds)

    fun setHistory(exerciseId: Int): Flow<List<SetHistoryRow>> = exerciseDao.getSetHistory(exerciseId)
}
