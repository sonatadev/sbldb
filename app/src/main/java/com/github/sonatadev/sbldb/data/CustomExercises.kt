package com.github.sonatadev.sbldb.data

import androidx.room.withTransaction
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.ExerciseJointAction
import com.github.sonatadev.sbldb.data.entity.ExerciseMuscle

/** A user-defined exercise as entered in the editor or read from a backup. */
data class CustomExerciseInput(
    val name: String,
    val equipment: String,
    val attachment: String?,
    val note: String?,
    val aliases: List<String>,
    /** Joint action id → rating 1–5. */
    val ratings: Map<Int, Int>
)

object CustomExercises {
    /** Creates or updates a custom exercise; muscles for volume are derived like bundled ones. */
    suspend fun save(db: AppDatabase, input: CustomExerciseInput, existingId: Int? = null): Int = db.withTransaction {
        require(input.name.isNotBlank()) { "Name is required" }
        require(input.ratings.isNotEmpty()) { "Pick at least one joint action" }
        val exerciseDao = db.exerciseDAO()
        val clash = exerciseDao.findByName(input.name.trim())
        require(clash == null || clash.exerciseId == existingId) { "An exercise called '${input.name.trim()}' already exists" }

        val row = Exercise(
            exerciseId = existingId ?: 0,
            name = input.name.trim(),
            equipment = input.equipment.trim().ifEmpty { "Other" },
            attachment = input.attachment?.trim()?.takeIf { it.isNotEmpty() },
            note = input.note?.trim()?.takeIf { it.isNotEmpty() },
            aliases = input.aliases.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.joinToString(" | "),
            isCustom = true
        )
        val id = if (existingId != null) existingId.also { exerciseDao.updateExercise(row) } else exerciseDao.insertExercise(row).toInt()

        val actionDao = db.jointActionDAO()
        actionDao.deleteExerciseLinks(id)
        input.ratings.forEach { (actionId, rating) -> actionDao.insertExerciseLink(ExerciseJointAction(id, actionId, rating.coerceIn(1, 5))) }

        deriveMuscles(db, id, input.ratings)
        id
    }

    /** Re-derives every custom exercise's muscles, after the library changed which muscles an action trains. */
    suspend fun rederiveAll(db: AppDatabase) {
        db.userDataDAO().customExercises().forEach { exercise ->
            val ratings = db.userDataDAO().exerciseLinks(exercise.exerciseId).associate { it.jointActionId to it.rating }
            deriveMuscles(db, exercise.exerciseId, ratings)
        }
    }

    private suspend fun deriveMuscles(db: AppDatabase, exerciseId: Int, ratings: Map<Int, Int>) {
        val links = if (ratings.isEmpty()) emptyList() else db.userDataDAO().actionMuscles(ratings.keys.toList())
        val roles = MuscleDerivation.combine(links.map { Triple(it.muscleId, it.role, ratings.getValue(it.jointActionId)) })
        db.exerciseMuscleDAO().deleteForExercise(exerciseId)
        roles.forEach { (muscleId, role) -> db.exerciseMuscleDAO().insertExerciseMuscle(ExerciseMuscle(exerciseId, muscleId, role)) }
    }

    /** Deletes a custom exercise, or archives it when it appears in logged workouts or routines. */
    suspend fun remove(db: AppDatabase, exercise: Exercise) = db.withTransaction {
        require(exercise.isCustom) { "Only custom exercises can be removed" }
        val used = db.userDataDAO().timesLogged(exercise.exerciseId) > 0 || db.routineDAO().usesExercise(exercise.exerciseId)
        if (used) db.exerciseDAO().updateExercise(exercise.copy(isArchived = true)) else db.exerciseDAO().deleteExercise(exercise)
    }
}
