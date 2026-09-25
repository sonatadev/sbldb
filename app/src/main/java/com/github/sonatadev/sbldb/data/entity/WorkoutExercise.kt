package com.github.sonatadev.sbldb.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["workoutId"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        // RESTRICT: an exercise that appears in the history cannot be deleted
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["exerciseId"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("workoutId"), Index("exerciseId")]
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val workoutExerciseId: Long = 0,
    val workoutId: Long,
    val exerciseId: Int,
    val position: Int
)
