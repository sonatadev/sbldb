package com.github.sonatadev.sbldb.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "muscles",
    indices = [Index(value = ["muscleGroup", "muscleRegion"], unique = true)]
)
data class Muscle(
    @PrimaryKey(autoGenerate = true) val muscleId: Int,
    val muscleGroup: String,
    val muscleRegion: String?,
    /** Explanations for beginners and for experienced lifters. */
    val infoBasic: String? = null,
    val infoExpert: String? = null
)
