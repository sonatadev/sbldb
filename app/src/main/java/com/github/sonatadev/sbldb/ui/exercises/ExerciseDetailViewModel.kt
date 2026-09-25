package com.github.sonatadev.sbldb.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.MuscleWithRole
import com.github.sonatadev.sbldb.data.entity.RatedJointAction
import com.github.sonatadev.sbldb.data.entity.SetHistoryRow
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.data.repository.JointActionRepository
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.domain.OneRepMax
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SessionHistory(val workoutId: Long, val startedAt: Long, val sets: List<SetHistoryRow>)

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val muscles: List<MuscleWithRole> = emptyList(),
    val bestE1rmKg: Double? = null,
    val sessions: List<SessionHistory> = emptyList(),
    val unit: WeightUnit = WeightUnit.KG,
    val actions: List<RatedJointAction> = emptyList()
)

class ExerciseDetailViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ExerciseRepository,
    jointActions: JointActionRepository,
    settings: SettingsRepository
) : ViewModel() {
    private val exerciseId: Int = checkNotNull(savedStateHandle["exerciseId"])

    private val base = combine(
        repository.exercise(exerciseId),
        repository.muscles(exerciseId),
        repository.setHistory(exerciseId),
        settings.weightUnit
    ) { exercise, muscles, history, unit ->
        ExerciseDetailUiState(
            exercise = exercise,
            muscles = muscles,
            bestE1rmKg = history.mapNotNull { OneRepMax.epley(it.weightKg, it.reps) }.maxOrNull(),
            sessions = history
                .groupBy { it.workoutId }
                .map { (id, sets) -> SessionHistory(id, sets.first().startedAt, sets) },
            unit = unit
        )
    }

    val uiState: StateFlow<ExerciseDetailUiState> =
        combine(base, jointActions.actionsForExercise(exerciseId)) { state, actions -> state.copy(actions = actions) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState())
}
