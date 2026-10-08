package com.github.sonatadev.sbldb.data.entity

import androidx.room.ColumnInfo
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
    val aliases: String? = null,
    /** Created by the user; never touched by the content sync. */
    @ColumnInfo(defaultValue = "0") val isCustom: Boolean = false,
    /** Hidden from lists (a custom exercise that still appears in the history). */
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    /**
     * Set on an attachment variant ("Lat Pulldown · V-Bar"): the exercise it belongs to. Variants are
     * how the same cable exercise is logged with another [attachment], so records and progression stay
     * per attachment. The parent itself stands for its default attachment.
     */
    @ColumnInfo(index = true) val parentId: Int? = null,
    /** A variant whose attachment changes the movement, with its own joint actions from the library. */
    @ColumnInfo(defaultValue = "0") val hasOwnActions: Boolean = false
) {
    val isVariant: Boolean get() = parentId != null

    /** The exercise a variant belongs to, or this one. */
    val familyId: Int get() = parentId ?: exerciseId

    val aliasList: List<String> get() = aliases?.split(" | ")?.filter { it.isNotBlank() }.orEmpty()
}
