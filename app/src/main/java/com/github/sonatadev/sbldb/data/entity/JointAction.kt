package com.github.sonatadev.sbldb.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A movement of one joint, e.g. Shoulder · Horizontal Adduction. The app's library is built on these. */
@Entity(tableName = "joint_actions", indices = [Index(value = ["joint", "name"], unique = true)])
data class JointAction(
    @PrimaryKey(autoGenerate = true) val jointActionId: Int = 0,
    val joint: String,
    val name: String,
    val description: String,
    /** Order in the library, following the YAML file. */
    val position: Int,
    val whatBasic: String? = null,
    val whatExpert: String? = null,
    val whyBasic: String? = null,
    val whyExpert: String? = null,
    val feelBasic: String? = null,
    val feelExpert: String? = null,
    /** [com.github.sonatadev.sbldb.domain.ActionAnimation] in its encoded form, if the content has one. */
    val animation: String? = null
)

/** A muscle that produces a joint action; PRIMARY = prime mover, SECONDARY = helper. */
@Entity(
    tableName = "joint_action_muscles",
    primaryKeys = ["jointActionId", "muscleId"],
    foreignKeys = [
        ForeignKey(entity = JointAction::class, parentColumns = ["jointActionId"], childColumns = ["jointActionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Muscle::class, parentColumns = ["muscleId"], childColumns = ["muscleId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("jointActionId"), Index("muscleId")]
)
data class JointActionMuscle(
    val jointActionId: Int,
    val muscleId: Int,
    val role: Role,
    /** How this muscle takes part in the action, for beginners and for experienced lifters. */
    val noteBasic: String? = null,
    val noteExpert: String? = null
)

/** A term explained in the glossary. */
@Entity(tableName = "glossary")
data class GlossaryTerm(
    @PrimaryKey val term: String,
    val basic: String,
    val expert: String,
    val position: Int
)

/** How well (1–5) an exercise trains a joint action. */
@Entity(
    tableName = "exercise_joint_actions",
    primaryKeys = ["exerciseId", "jointActionId"],
    foreignKeys = [
        ForeignKey(entity = Exercise::class, parentColumns = ["exerciseId"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = JointAction::class, parentColumns = ["jointActionId"], childColumns = ["jointActionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("exerciseId"), Index("jointActionId")]
)
data class ExerciseJointAction(val exerciseId: Int, val jointActionId: Int, val rating: Int)
