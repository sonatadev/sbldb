package com.github.sonatadev.sbldb.data.repository

import com.github.sonatadev.sbldb.data.entity.ExerciseNote
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.dao.ExerciseMuscleGroup
import com.github.sonatadev.sbldb.data.dao.ExercisePrimaryGroup
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.MuscleWithRole
import com.github.sonatadev.sbldb.data.entity.SetHistoryRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.github.sonatadev.sbldb.data.entity.MuscleTarget
import com.github.sonatadev.sbldb.domain.VolumeTarget

class ExerciseRepository(db: AppDatabase) {
    private val exerciseDao = db.exerciseDAO()
    private val muscleDao = db.muscleDAO()

    val exercises: Flow<List<Exercise>> = exerciseDao.getAllExercises()
    val primaryGroups: Flow<List<ExercisePrimaryGroup>> = exerciseDao.getPrimaryGroups()
    val muscleGroups: Flow<List<String>> = muscleDao.getMuscleGroups()

    private val userData = db.userDataDAO()

    /** The user's own weekly set targets; groups without one use 10–20. */
    val volumeTargets: Flow<Map<String, VolumeTarget>> =
        userData.targets().map { list -> list.associate { it.muscleGroup to VolumeTarget.of(it.minSets, it.maxSets) } }

    suspend fun setVolumeTarget(group: String, target: VolumeTarget) {
        if (target == VolumeTarget.DEFAULT) userData.deleteTarget(group)
        else userData.upsertTarget(MuscleTarget(group, target.minSets, target.maxSets))
    }

    suspend fun resetVolumeTargets() = userData.deleteTargets()

    /** Personal notes per exercise id (seat height, grip, cues). */
    val notes: Flow<Map<Int, String>> = userData.exerciseNotes().map { list -> list.associate { it.exerciseId to it.text } }

    fun note(exerciseId: Int): Flow<String?> = userData.exerciseNote(exerciseId).map { it?.text }

    /** A blank note deletes it. */
    suspend fun setNote(exerciseId: Int, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) userData.deleteExerciseNote(exerciseId) else userData.upsertExerciseNote(ExerciseNote(exerciseId, trimmed))
    }

    fun exercise(exerciseId: Int): Flow<Exercise?> = exerciseDao.getExercise(exerciseId)

    fun muscles(exerciseId: Int): Flow<List<MuscleWithRole>> = exerciseDao.getMusclesForExercise(exerciseId)

    suspend fun muscleGroups(exerciseIds: List<Int>): List<ExerciseMuscleGroup> = exerciseDao.getMuscleGroups(exerciseIds)

    fun setHistory(exerciseId: Int): Flow<List<SetHistoryRow>> = exerciseDao.getSetHistory(exerciseId)
}
