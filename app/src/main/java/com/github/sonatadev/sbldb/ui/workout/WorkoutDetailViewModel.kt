package com.github.sonatadev.sbldb.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
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
    settings: SettingsRepository
) : ViewModel() {
    private val workoutId: Long = checkNotNull(savedStateHandle["workoutId"])

    val uiState: StateFlow<WorkoutDetailUiState> =
        combine(repository.workout(workoutId), settings.weightUnit) { workout, unit ->
            WorkoutDetailUiState(isLoading = false, workout = workout, unit = unit)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutDetailUiState())

    fun delete() {
        val workout = uiState.value.workout?.workout ?: return
        viewModelScope.launch { repository.delete(workout) }
    }
}
