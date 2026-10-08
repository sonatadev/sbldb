package com.github.sonatadev.sbldb.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.data.repository.WorkoutRepository
import com.github.sonatadev.sbldb.domain.MuscleToday
import com.github.sonatadev.sbldb.domain.RecordHit
import com.github.sonatadev.sbldb.domain.VolumeCalculator
import com.github.sonatadev.sbldb.domain.WeekRange
import com.github.sonatadev.sbldb.domain.WeightUnit
import com.github.sonatadev.sbldb.domain.WorkoutSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class WorkoutSummaryUiState(
    val loaded: Boolean = false,
    val workout: WorkoutWithExercises? = null,
    val hardSets: Int = 0,
    val tonnageKg: Double = 0.0,
    val records: List<RecordHit> = emptyList(),
    val muscles: List<MuscleToday> = emptyList(),
    val unit: WeightUnit = WeightUnit.KG
)

/** What a finished workout did: computed once, the workout no longer changes. */
class WorkoutSummaryViewModel(
    savedStateHandle: SavedStateHandle,
    workouts: WorkoutRepository,
    exercises: ExerciseRepository,
    settings: SettingsRepository
) : ViewModel() {
    val workoutId: Long = checkNotNull(savedStateHandle["workoutId"])

    private val state = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = state

    init {
        viewModelScope.launch {
            val workout = workouts.workout(workoutId).firstOrNull()
            if (workout == null) {
                state.value = WorkoutSummaryUiState(loaded = true)
                return@launch
            }
            val working = workout.exercises.flatMap { it.sets }.filter { it.isCompleted && !it.isWarmup }
            val history = workout.exercises.map { it.exercise }.distinctBy { it.exerciseId }
                .associate { (it.exerciseId to it.name) to exercises.setHistory(it.exerciseId).first() }
            val week = WeekRange.containing(workout.workout.startedAt)
            state.value = WorkoutSummaryUiState(
                loaded = true,
                workout = workout,
                // A left and a right set make one
                hardSets = working.filter { VolumeCalculator.isHardSet(it.rir) }.sumOf { if (it.side != null) 0.5 else 1.0 }.toInt(),
                // Seconds held aren't reps: holds stay out of the load lifted
                tonnageKg = WorkoutSummary.tonnage(
                    workout.exercises.filter { !it.exercise.isTimed }.flatMap { it.sets }.filter { it.isCompleted && !it.isWarmup }.map { it.weightKg to it.reps }
                ),
                records = WorkoutSummary.records(
                    history, workoutId, workout.workout.startedAt,
                    timed = workout.exercises.filter { it.exercise.isTimed }.map { it.exercise.exerciseId }.toSet()
                ),
                muscles = WorkoutSummary.muscles(
                    workouts.volumeRows(week.startMillis, week.endMillis).first(),
                    workoutId,
                    exercises.volumeTargets.first()
                ),
                unit = settings.weightUnit.first()
            )
        }
    }
}
