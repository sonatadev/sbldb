package com.github.sonatadev.sbldb.ui.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.RoutineExercise
import com.github.sonatadev.sbldb.data.entity.RoutineWithExercises
import com.github.sonatadev.sbldb.data.repository.RoutineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutineEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val routines: RoutineRepository
) : ViewModel() {
    val routineId: Long = checkNotNull(savedStateHandle["routineId"])

    /** null once the routine has been deleted. */
    val routine: StateFlow<RoutineWithExercises?> = routines.routine(routineId)
        .map { r -> r?.copy(exercises = r.exercises.sortedBy { it.routineExercise.position }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
