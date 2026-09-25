package com.github.sonatadev.sbldb.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A personal, persistent note on an exercise, such as machine settings. */
@Entity(
    tableName = "exercise_notes",
    foreignKeys = [ForeignKey(entity = Exercise::class, parentColumns = ["exerciseId"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)]
)
data class ExerciseNote(@PrimaryKey val exerciseId: Int, val text: String)

/** A body weight and measurements entry. [date] is an epoch day. */
@Entity(tableName = "body_entries", indices = [Index("date")])
data class BodyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weightKg: Double? = null,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val armCm: Double? = null,
    val thighCm: Double? = null
)

/** A personal weekly volume target for a muscle group, replacing the default 10–20. */
@Entity(tableName = "muscle_targets")
data class MuscleTarget(@PrimaryKey val muscleGroup: String, val minSets: Int, val maxSets: Int)
