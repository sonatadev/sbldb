package com.github.sonatadev.sbldb.data

import androidx.room.withTransaction
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.ExerciseJointAction
import com.github.sonatadev.sbldb.data.entity.ExerciseMuscle

/**
 * Attachment variants: a cable exercise is one entry in the library, and logging it with another
 * attachment uses a variant row ("Triceps Pushdown · Straight Bar") created the first time it's needed.
 * A variant copies its exercise's joint actions and muscles, unless the library gives it its own
 * because the attachment changes the movement.
 */
object Variants {
    /** Every attachment that can go on a cable, offered on any cable exercise. */
    val ATTACHMENTS = listOf(
        "D-Handle", "Rope", "Straight Bar", "EZ Bar", "V-Bar", "Wide Bar", "Neutral-Grip Wide Bar", "Ankle Strap", "Forearm Strap"
    )

    private const val SEPARATOR = " · "

    fun name(parent: String, attachment: String) = "$parent$SEPARATOR$attachment"

    /** "Lat Pulldown · V-Bar" → ("Lat Pulldown", "V-Bar"); null for a plain name. */
    fun split(name: String): Pair<String, String>? =
        name.lastIndexOf(SEPARATOR).takeIf { it > 0 }?.let { name.substring(0, it) to name.substring(it + SEPARATOR.length) }

    /** Whether an attachment can be picked for [exercise]. */
    fun takesAttachments(exercise: Exercise) = exercise.equipment.equals("Cable", ignoreCase = true)

    /**
     * The exercise to log for [exerciseId]'s family with [attachment]: the family's own row for its
     * default attachment, else the variant, created (copying the family's movement) if it's new.
     */
    suspend fun idFor(db: AppDatabase, exerciseId: Int, attachment: String): Int = db.withTransaction {
        val dao = db.exerciseDAO()
        val current = requireNotNull(dao.findById(exerciseId)) { "No exercise $exerciseId" }
        val parent = if (current.parentId != null) requireNotNull(dao.findById(current.parentId)) else current
        if (parent.attachment == attachment) return@withTransaction parent.exerciseId
        dao.findVariant(parent.exerciseId, attachment)?.let {
            // Picked again after being removed from the list: it comes back
            if (it.isArchived) dao.updateExercise(it.copy(isArchived = false))
            return@withTransaction it.exerciseId
        }
        val id = dao.insertExercise(
            Exercise(
                exerciseId = 0,
                name = name(parent.name, attachment),
                equipment = parent.equipment,
                attachment = attachment,
                isCustom = parent.isCustom,
                parentId = parent.exerciseId,
                isTimed = parent.isTimed,
                isUnilateral = parent.isUnilateral
            )
        ).toInt()
        copyMovement(db, from = parent.exerciseId, to = id)
        id
    }

    /** Gives [to] the same joint actions and muscles as [from]. */
    suspend fun copyMovement(db: AppDatabase, from: Int, to: Int) {
        val actions = db.jointActionDAO()
        actions.deleteExerciseLinks(to)
        db.userDataDAO().exerciseLinks(from).forEach { actions.insertExerciseLink(ExerciseJointAction(to, it.jointActionId, it.rating)) }
        val muscles = db.exerciseMuscleDAO()
        muscles.deleteForExercise(to)
        muscles.forExercise(from).forEach { muscles.insertExerciseMuscle(ExerciseMuscle(to, it.muscleId, it.role)) }
    }

    /** After an exercise's movement changed: its variants without a movement of their own follow it. */
    suspend fun refreshVariants(db: AppDatabase, parentId: Int) {
        val dao = db.exerciseDAO()
        val parent = dao.findById(parentId) ?: return
        dao.variantsOf(parentId).forEach { variant ->
            if (variant.isTimed != parent.isTimed || variant.isUnilateral != parent.isUnilateral) {
                dao.updateExercise(variant.copy(isTimed = parent.isTimed, isUnilateral = parent.isUnilateral))
            }
            if (!variant.hasOwnActions) copyMovement(db, parentId, variant.exerciseId)
        }
    }

    /**
     * Takes an attachment the user added off the list. Variants already logged or in a routine are
     * only hidden, so their history stays; the others are deleted. The library's attachments stay.
     */
    suspend fun removeCustom(db: AppDatabase, attachment: String) = db.withTransaction {
        if (attachment in ATTACHMENTS) return@withTransaction
        db.exerciseDAO().variantsWith(attachment).forEach { variant ->
            val used = db.userDataDAO().timesLogged(variant.exerciseId) > 0 || db.routineDAO().usesExercise(variant.exerciseId)
            if (used) db.exerciseDAO().updateExercise(variant.copy(isArchived = true)) else db.exerciseDAO().deleteExercise(variant)
        }
    }

    /**
     * Finds an exercise by a name from a backup or an older version: the current name, a former name
     * kept as an alias, or "Exercise · Attachment" for a variant that doesn't exist yet.
     */
    suspend fun resolve(db: AppDatabase, name: String): Int? {
        val dao = db.exerciseDAO()
        dao.findId(name)?.let { return it }
        // A merged exercise's old name lives on as an alias of its variant: those win over a plain alias
        dao.allExercises().sortedByDescending { it.isVariant }
            .firstOrNull { e -> e.aliasList.any { it.equals(name, ignoreCase = true) } }?.let { return it.exerciseId }
        val (parentName, attachment) = split(name) ?: return null
        val parent = resolve(db, parentName) ?: return null
        return idFor(db, parent, attachment)
    }
}
