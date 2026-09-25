package com.github.sonatadev.sbldb.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MuscleWithRole(
    @Embedded val muscle: Muscle,
    val role: Role
)

data class WorkoutExerciseWithSets(
    @Embedded val workoutExercise: WorkoutExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "exerciseId")
    val exercise: Exercise,
    @Relation(parentColumn = "workoutExerciseId", entityColumn = "workoutExerciseId")
    val sets: List<WorkoutSet>
)

data class WorkoutWithExercises(
    @Embedded val workout: Workout,
    @Relation(
        entity = WorkoutExercise::class,
        parentColumn = "workoutId",
        entityColumn = "workoutId"
    )
    val exercises: List<WorkoutExerciseWithSets>
)

/** One row per (completed set × targeted muscle), used by the volume calculator. */
data class VolumeRow(
    val setId: Long,
    val workoutId: Long,
    val startedAt: Long,
    val exerciseId: Int,
    val muscleGroup: String,
    val muscleRegion: String?,
    val role: Role,
    val rir: Int?
)

/** A completed working set with the start time of its workout, for history and e1RM. */
data class SetHistoryRow(
    val workoutId: Long,
    val startedAt: Long,
    val weightKg: Double?,
    val reps: Int?,
    val rir: Int?,
    val setType: SetType = SetType.NORMAL
) {
    /** Straight sets at full range: drop sets and partials would distort records and progression. */
    val isStraight: Boolean get() = setType == SetType.NORMAL || setType == SetType.FAILURE || setType == SetType.MYO
}

/** A joint action with its prime movers, for the library list. */
data class JointActionSummary(
    val jointActionId: Int,
    val joint: String,
    val name: String,
    val position: Int,
    /** Comma-separated primary muscle groups. */
    val primaryGroups: String?,
    val whatBasic: String?,
    val whatExpert: String?
)

data class JointActionMuscleRow(
    val muscleGroup: String,
    val muscleRegion: String?,
    val role: Role,
    val noteBasic: String?,
    val noteExpert: String?
)

/** A joint action a muscle group takes part in, with its strongest role there. */
data class MuscleActionRow(
    val jointActionId: Int,
    val joint: String,
    val name: String,
    val role: Role
)

/** An exercise with its rating for a given joint action, or a joint action with its rating for an exercise. */
data class RatedExercise(
    val exerciseId: Int,
    val name: String,
    val equipment: String,
    val attachment: String?,
    val note: String?,
    val rating: Int
)

data class RatedJointAction(
    val jointActionId: Int,
    val joint: String,
    val name: String,
    val rating: Int
)

/** Number of finished workouts that started on a given day (epoch day in the device zone). */
data class WorkoutDay(val workoutId: Long, val startedAt: Long)
