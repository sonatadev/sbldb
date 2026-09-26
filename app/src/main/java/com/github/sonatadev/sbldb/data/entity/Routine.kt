package com.github.sonatadev.sbldb.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** A pre-planned session, e.g. "Upper" or "Lower". */
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val routineId: Long = 0,
    val name: String,
    val position: Int,
    /** How many times a week the routine is planned, for the weekly volume planner. */
    @ColumnInfo(defaultValue = "1") val timesPerWeek: Int = 1
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(entity = Routine::class, parentColumns = ["routineId"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["exerciseId"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("routineId"), Index("exerciseId")]
)
data class RoutineExercise(
    @PrimaryKey(autoGenerate = true) val routineExerciseId: Long = 0,
    val routineId: Long,
    val exerciseId: Int,
    val position: Int,
    val sets: Int = 3,
    val repMin: Int = 8,
    val repMax: Int = 12,
    val targetRir: Int? = 1,
    @ColumnInfo(defaultValue = "120") val restSeconds: Int = 120
)

data class RoutineExerciseWithExercise(
    @Embedded val routineExercise: RoutineExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "exerciseId")
    val exercise: Exercise
)

data class RoutineWithExercises(
    @Embedded val routine: Routine,
    @Relation(entity = RoutineExercise::class, parentColumn = "routineId", entityColumn = "routineId")
    val exercises: List<RoutineExerciseWithExercise>
)

/** A routine planned for a day. [date] is an epoch day in the device's time zone. */
@Entity(
    tableName = "planned_workouts",
    foreignKeys = [
        ForeignKey(entity = Routine::class, parentColumns = ["routineId"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("date"), Index("routineId")]
)
data class PlannedWorkout(
    @PrimaryKey(autoGenerate = true) val plannedId: Long = 0,
    val date: Long,
    val routineId: Long
)

/** A planned workout with the name of its routine, for the calendar. */
data class PlannedRoutine(val plannedId: Long, val date: Long, val routineId: Long, val name: String)
