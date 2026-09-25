package com.github.sonatadev.sbldb.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.RoutineExercise
import com.github.sonatadev.sbldb.data.entity.SetHistoryRow
import com.github.sonatadev.sbldb.data.entity.WorkoutExercise
import com.github.sonatadev.sbldb.data.entity.WorkoutExerciseWithSets
import com.github.sonatadev.sbldb.data.entity.WorkoutSet
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.data.repository.RoutineRepository
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.data.repository.WorkoutRepository
import com.github.sonatadev.sbldb.domain.VolumeCalculator
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A muscle group an exercise works, with the fractional set it earns ("CHEST 1.0"). */
data class MuscleChip(val group: String, val weight: Double) {
    val isPrimary: Boolean get() = weight == VolumeCalculator.PRIMARY_WEIGHT
}

/** Per-exercise data that does not change while logging: last performance and muscles. */
data class ExerciseInfo(
    val previous: List<SetHistoryRow> = emptyList(),
    val muscles: List<MuscleChip> = emptyList(),
    /** Plan from the routine this workout was started from, if any. */
    val target: RoutineExercise? = null
)

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val workout: WorkoutWithExercises? = null,
    val unit: WeightUnit = WeightUnit.KG,
    val info: Map<Int, ExerciseInfo> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModel(
    private val repository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    settings: SettingsRepository
) : ViewModel() {

    private val workout = repository.activeWorkout
        .map { it?.workoutId }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.workout(id) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val info = workout
        .map { w -> w?.workout?.routineId to w?.exercises?.map { it.exercise.exerciseId }?.distinct().orEmpty() }
        .distinctUntilChanged()
        .mapLatest { (routineId, ids) -> loadInfo(ids, routineId) }
        .onStart { emit(emptyMap()) }

    val uiState: StateFlow<ActiveWorkoutUiState> =
        combine(workout, settings.weightUnit, info) { workout, unit, info ->
            ActiveWorkoutUiState(isLoading = false, workout = workout, unit = unit, info = info)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

    private suspend fun loadInfo(ids: List<Int>, routineId: Long?): Map<Int, ExerciseInfo> {
        if (ids.isEmpty()) return emptyMap()
        val muscles = exerciseRepository.muscleGroups(ids).groupBy { it.exerciseId }
        val targets = routineId?.let { routineRepository.find(it) }?.exercises
            ?.associate { it.routineExercise.exerciseId to it.routineExercise }.orEmpty()
        return ids.associateWith { id ->
            val chips = muscles[id].orEmpty()
                .groupBy { it.muscleGroup }
                .map { (group, rows) -> MuscleChip(group, rows.maxOf { VolumeCalculator.weight(it.role) }) }
                .sortedWith(compareByDescending<MuscleChip> { it.weight }.thenBy { it.group })
            ExerciseInfo(previous = repository.lastPerformance(id), muscles = chips, target = targets[id])
        }
    }

    fun rename(name: String) = launch { current()?.let { repository.rename(it.workout, name.trim()) } }

    fun addSet(exercise: WorkoutExerciseWithSets) = launch {
        repository.addSet(exercise.workoutExercise.workoutExerciseId, exercise.sets.maxByOrNull { it.position })
    }

    fun removeExercise(exercise: WorkoutExercise) = launch { repository.removeExercise(exercise) }

    fun updateWeight(set: WorkoutSet, weightKg: Double?) = launch { repository.updateWeight(set.setId, weightKg) }

    fun updateReps(set: WorkoutSet, reps: Int?) = launch { repository.updateReps(set.setId, reps) }

    fun updateRir(set: WorkoutSet, rir: Int?) = launch { repository.updateRir(set.setId, rir) }

    fun toggleCompleted(set: WorkoutSet) = launch { repository.updateCompleted(set.setId, !set.isCompleted) }

    fun toggleWarmup(set: WorkoutSet) = launch { repository.updateWarmup(set.setId, !set.isWarmup) }

    fun deleteSet(set: WorkoutSet) = launch { repository.deleteSet(set) }

    fun finish() = launch { current()?.let { repository.finish(it.workout) } }

    fun discard() = launch { current()?.let { repository.delete(it.workout) } }

    private fun current(): WorkoutWithExercises? = uiState.value.workout

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

