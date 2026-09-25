package com.github.sonatadev.sbldb.ui.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.RoutineExercise
import com.github.sonatadev.sbldb.data.entity.RoutineWithExercises
import com.github.sonatadev.sbldb.data.repository.RoutineRepository
import com.github.sonatadev.sbldb.domain.PlannedRoutine
import com.github.sonatadev.sbldb.domain.PlannedSlot
import com.github.sonatadev.sbldb.domain.WeeklyPlan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutineEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository
) : ViewModel() {
    val routineId: Long = checkNotNull(savedStateHandle["routineId"])

    /** null once the routine has been deleted. */
    val routine: StateFlow<RoutineWithExercises?> = routines.routine(routineId)
        .map { r -> r?.copy(exercises = r.exercises.sortedBy { it.routineExercise.position }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Weekly sets per muscle this routine plans, times its frequency; largest first. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val plan: StateFlow<List<Pair<String, Double>>> = routine
        .mapLatest { r -> if (r == null) emptyList() else planFor(listOf(r), exercises) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTimesPerWeek(times: Int) = launch { routine.value?.let { routines.setTimesPerWeek(it.routine, times) } }

    fun rename(name: String) = launch { routine.value?.let { routines.rename(it.routine, name.trim()) } }

    fun update(exercise: RoutineExercise) = launch { routines.updateExercise(exercise) }

    fun remove(exercise: RoutineExercise) = launch { routines.removeExercise(exercise) }

    fun move(exercise: RoutineExercise, offset: Int) = launch {
        routine.value?.let { r -> routines.move(r.exercises.map { it.routineExercise }, exercise, offset) }
    }

    fun delete(onDeleted: () -> Unit) = launch {
        routine.value?.let { routines.delete(it.routine) }
        onDeleted()
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

/** Planned weekly sets per muscle group for [routines], largest first. */
internal suspend fun planFor(routines: List<RoutineWithExercises>, exercises: ExerciseRepository): List<Pair<String, Double>> {
    val ids = routines.flatMap { r -> r.exercises.map { it.routineExercise.exerciseId } }.distinct()
    if (ids.isEmpty()) return emptyList()
    val roles = exercises.muscleGroups(ids).groupBy({ it.exerciseId }, { it.muscleGroup to it.role })
    val planned = routines.map { r ->
        PlannedRoutine(r.routine.timesPerWeek, r.exercises.map { PlannedSlot(it.routineExercise.exerciseId, it.routineExercise.sets) })
    }
    return WeeklyPlan.setsPerGroup(planned, roles).toList().sortedWith(compareByDescending<Pair<String, Double>> { it.second }.thenBy { it.first })
}
