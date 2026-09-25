package com.github.sonatadev.sbldb.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.WorkoutExercise
import com.github.sonatadev.sbldb.data.entity.WorkoutExerciseWithSets
import com.github.sonatadev.sbldb.data.entity.SetType
import com.github.sonatadev.sbldb.data.entity.WorkoutSet
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import com.github.sonatadev.sbldb.data.repository.RoutineRepository
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.data.repository.WorkoutRepository
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutDetailUiState(
    val isLoading: Boolean = true,
    val workout: WorkoutWithExercises? = null,
    val unit: WeightUnit = WeightUnit.KG
)

class WorkoutDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
    settings: SettingsRepository,
    private val routines: RoutineRepository
) : ViewModel(), SetActions {
    val workoutId: Long = checkNotNull(savedStateHandle["workoutId"])

    val uiState: StateFlow<WorkoutDetailUiState> =
        combine(repository.workout(workoutId), settings.weightUnit) { workout, unit ->
            WorkoutDetailUiState(isLoading = false, workout = workout, unit = unit)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutDetailUiState())

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun delete() {
        val workout = uiState.value.workout?.workout ?: return
        launch { repository.delete(workout) }
    }

    fun saveAsRoutine(onCreated: (Long) -> Unit) {
        val workout = uiState.value.workout ?: return
        viewModelScope.launch { onCreated(routines.createFromWorkout(workout)) }
    }

    fun rename(name: String) {
        val workout = uiState.value.workout?.workout ?: return
        if (name.isNotBlank()) launch { repository.rename(workout, name.trim()) }
    }

    fun updateTimes(startedAt: Long, durationMillis: Long) {
        val workout = uiState.value.workout?.workout ?: return
        launch { repository.updateTimes(workout, startedAt, durationMillis) }
    }

    fun addSet(exercise: WorkoutExerciseWithSets) = launch {
        repository.addSet(exercise.workoutExercise.workoutExerciseId, exercise.sets.maxByOrNull { it.position }, completed = true)
    }

    fun removeExercise(exercise: WorkoutExercise) = launch { repository.removeExercise(exercise) }

    override fun updateWeight(set: WorkoutSet, weightKg: Double?) = launch { repository.updateWeight(set.setId, weightKg) }

    override fun updateReps(set: WorkoutSet, reps: Int?) = launch { repository.updateReps(set.setId, reps) }

    override fun updateRir(set: WorkoutSet, rir: Int?) = launch { repository.updateRir(set.setId, rir) }

    override fun toggleCompleted(set: WorkoutSet) = launch { repository.updateCompleted(set.setId, !set.isCompleted) }

    override fun toggleWarmup(set: WorkoutSet) = launch { repository.updateWarmup(set.setId, !set.isWarmup) }

    override fun setType(set: WorkoutSet, type: SetType) = launch { repository.updateSetType(set.setId, type) }

    override fun deleteSet(set: WorkoutSet) = launch { repository.deleteSet(set) }
}
