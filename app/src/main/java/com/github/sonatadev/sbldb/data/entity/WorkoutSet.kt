package com.github.sonatadev.sbldb.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A single set. Weight is always stored in kg; conversion to lb happens in the UI. */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["workoutExerciseId"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutExerciseId")]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val setId: Long = 0,
    val workoutExerciseId: Long,
    val position: Int,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val rir: Int? = null,
    val isWarmup: Boolean = false,
    val isCompleted: Boolean = false,
    /** Kept in sync with [isWarmup] (WARMUP ⇔ isWarmup). */
    @ColumnInfo(defaultValue = "NORMAL") val setType: SetType = SetType.NORMAL
)
