package com.github.sonatadev.sbldb.data.repository

import androidx.room.withTransaction
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.entity.Routine
import com.github.sonatadev.sbldb.data.entity.RoutineExercise
import com.github.sonatadev.sbldb.data.entity.RoutineWithExercises
import kotlinx.coroutines.flow.Flow

class RoutineRepository(private val db: AppDatabase) {
    private val dao = db.routineDAO()

    val routines: Flow<List<RoutineWithExercises>> = dao.getAll()

    fun routine(id: Long): Flow<RoutineWithExercises?> = dao.get(id)

    suspend fun find(id: Long): RoutineWithExercises? = dao.find(id)

    suspend fun create(name: String): Long = db.withTransaction {
        dao.insert(Routine(name = name, position = dao.nextPosition()))
    }

    suspend fun rename(routine: Routine, name: String) = dao.update(routine.copy(name = name))

    suspend fun setTimesPerWeek(routine: Routine, times: Int) = dao.update(routine.copy(timesPerWeek = times.coerceIn(1, 7)))

    suspend fun delete(routine: Routine) = dao.delete(routine)

    suspend fun addExercise(routineId: Long, exerciseId: Int) = db.withTransaction {
        dao.insertExercise(RoutineExercise(routineId = routineId, exerciseId = exerciseId, position = dao.nextExercisePosition(routineId)))
    }

    suspend fun updateExercise(exercise: RoutineExercise) = dao.updateExercise(exercise)

    suspend fun removeExercise(exercise: RoutineExercise) = dao.deleteExercise(exercise)

    /** Swaps [exercise] with its neighbour in [ordered]; [offset] is -1 (up) or +1 (down). */
    suspend fun move(ordered: List<RoutineExercise>, exercise: RoutineExercise, offset: Int) {
        val index = ordered.indexOfFirst { it.routineExerciseId == exercise.routineExerciseId }
        val other = ordered.getOrNull(index + offset) ?: return
        dao.updateExercises(listOf(exercise.copy(position = other.position), other.copy(position = exercise.position)))
    }
}
