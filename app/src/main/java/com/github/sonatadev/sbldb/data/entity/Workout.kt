package com.github.sonatadev.sbldb.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A training session. [endedAt] is null while the workout is still in progress. */
@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(entity = Routine::class, parentColumns = ["routineId"], childColumns = ["routineId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("startedAt"), Index("routineId")]
)
data class Workout(
    @PrimaryKey(autoGenerate = true) val workoutId: Long = 0,
    val name: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val notes: String? = null,
    /** The routine this workout was started from, if any. */
    val routineId: Long? = null
)
