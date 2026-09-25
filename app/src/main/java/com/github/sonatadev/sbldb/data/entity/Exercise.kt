package com.github.sonatadev.sbldb.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises", indices = [Index(value = ["name"], unique = true)])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val exerciseId: Int,
    val name: String,
    val equipment: String,
    val attachment: String?,
    /** One line on why the exercise works, from the bundled content. */
    val note: String? = null,
    /** Other names people search for, separated by " | " (e.g. "Transverse row | Pulley presa larga"). */
    val aliases: String? = null
) {
    val aliasList: List<String> get() = aliases?.split(" | ")?.filter { it.isNotBlank() }.orEmpty()
}
