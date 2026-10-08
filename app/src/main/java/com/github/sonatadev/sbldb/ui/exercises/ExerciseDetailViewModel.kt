package com.github.sonatadev.sbldb.ui.exercises

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filterNotNull
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
import com.github.sonatadev.sbldb.domain.LoadMetric
import com.github.sonatadev.sbldb.domain.LoadProgress
import com.github.sonatadev.sbldb.domain.OneRepMax
import com.github.sonatadev.sbldb.domain.PastSet
import com.github.sonatadev.sbldb.domain.PersonalRecords
import com.github.sonatadev.sbldb.domain.Records
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionHistory(val workoutId: Long, val startedAt: Long, val sets: List<SetHistoryRow>)

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val muscles: List<MuscleWithRole> = emptyList(),
    val bestE1rmKg: Double? = null,
    val sessions: List<SessionHistory> = emptyList(),
    val unit: WeightUnit = WeightUnit.KG,
    val actions: List<RatedJointAction> = emptyList(),
    val records: Records = Records(),
    /** One point per session for each load metric, oldest first: (startedAt, kg). */
    val loadSeries: Map<LoadMetric, List<Pair<Long, Double>>> = emptyMap(),
    val note: String? = null,
    /** The exercise and its attachment variants, when it has any. */
    val family: List<Exercise> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository,
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
            unit = unit,
            records = PersonalRecords.of(history.filter { it.isStraight }.map { PastSet(it.weightKg, it.reps, it.rir, exercise?.isTimed == true) }),
            loadSeries = LoadMetric.entries.associateWith { LoadProgress.series(history, it) }
        )
    }

    val uiState: StateFlow<ExerciseDetailUiState> =
        combine(
            base,
            jointActions.actionsForExercise(exerciseId),
            repository.note(exerciseId),
            repository.exercise(exerciseId).filterNotNull().flatMapLatest { repository.family(it.familyId) }
        ) { state, actions, note, family ->
            state.copy(actions = actions, note = note, family = family)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState())

    fun setNote(text: String) {
        viewModelScope.launch { repository.setNote(exerciseId, text) }
    }
}
