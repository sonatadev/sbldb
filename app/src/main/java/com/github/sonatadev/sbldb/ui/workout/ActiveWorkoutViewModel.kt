package com.github.sonatadev.sbldb.ui.workout

import com.github.sonatadev.sbldb.domain.Warmup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.RoutineExercise
import com.github.sonatadev.sbldb.data.entity.SetHistoryRow
import com.github.sonatadev.sbldb.data.entity.WorkoutExercise
import com.github.sonatadev.sbldb.data.entity.WorkoutExerciseWithSets
import com.github.sonatadev.sbldb.data.entity.SetType
import com.github.sonatadev.sbldb.data.entity.WorkoutSet
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import com.github.sonatadev.sbldb.data.backup.BackupManager
import com.github.sonatadev.sbldb.session.WorkoutSession
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.data.repository.RoutineRepository
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.data.repository.WorkoutRepository
import com.github.sonatadev.sbldb.domain.VolumeCalculator
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import com.github.sonatadev.sbldb.domain.PastSet
import com.github.sonatadev.sbldb.domain.PersonalRecords
import com.github.sonatadev.sbldb.domain.Records
import com.github.sonatadev.sbldb.domain.Suggestion
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
    val target: RoutineExercise? = null,
    /** Personal bests before this workout, for PR chips. */
    val records: Records = Records()
)

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val workout: WorkoutWithExercises? = null,
    val unit: WeightUnit = WeightUnit.KG,
    val info: Map<Int, ExerciseInfo> = emptyMap(),
    /** Personal notes by exercise id. */
    val notes: Map<Int, String> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModel(
    private val repository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    settings: SettingsRepository,
    private val backupManager: BackupManager,
    private val session: WorkoutSession
) : ViewModel(), SetActions {
    val rest = session.rest

    private val defaultRest = settings.restSeconds.stateIn(viewModelScope, SharingStarted.Eagerly, 120)

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
        combine(workout, settings.weightUnit, info, exerciseRepository.notes) { workout, unit, info, notes ->
            ActiveWorkoutUiState(isLoading = false, workout = workout, unit = unit, info = info, notes = notes)
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
            val history = exerciseRepository.setHistory(id).first().filter { it.isStraight }.map { PastSet(it.weightKg, it.reps, it.rir) }
            ExerciseInfo(previous = repository.lastPerformance(id), muscles = chips, target = targets[id], records = PersonalRecords.of(history))
        }
    }

    fun rename(name: String) = launch { current()?.let { repository.rename(it.workout, name.trim()) } }

    fun addSet(exercise: WorkoutExerciseWithSets) = launch {
        repository.addSet(exercise.workoutExercise.workoutExerciseId, exercise.sets.maxByOrNull { it.position })
    }

    fun removeExercise(exercise: WorkoutExercise) = launch { repository.removeExercise(exercise) }

    fun setExerciseNote(exerciseId: Int, text: String) = launch { exerciseRepository.setNote(exerciseId, text) }

    fun setWorkoutNote(exercise: WorkoutExercise, text: String) = launch { repository.setWorkoutNote(exercise, text) }

    fun addWarmups(exercise: WorkoutExerciseWithSets, workKg: Double, unit: WeightUnit) = launch {
        val ramp = Warmup.ramp(workKg, exercise.exercise.equipment, unit)
        if (ramp.isNotEmpty()) repository.addWarmups(exercise.workoutExercise.workoutExerciseId, ramp)
    }

    suspend fun swapCandidates(exerciseId: Int): List<Exercise> = repository.swapCandidates(exerciseId)

    fun swap(exercise: WorkoutExercise, exerciseId: Int) = launch { repository.swapExercise(exercise, exerciseId) }

    override fun updateWeight(set: WorkoutSet, weightKg: Double?) = launch { repository.updateWeight(set.setId, weightKg) }

    override fun updateReps(set: WorkoutSet, reps: Int?) = launch { repository.updateReps(set.setId, reps) }

    override fun updateRir(set: WorkoutSet, rir: Int?) = launch { repository.updateRir(set.setId, rir) }

    override fun toggleCompleted(set: WorkoutSet) = launch { repository.updateCompleted(set.setId, !set.isCompleted) }

    /** Completes a set and starts the rest timer for its exercise. */
    fun logSet(exercise: WorkoutExerciseWithSets, set: WorkoutSet) = launch {
        repository.updateCompleted(set.setId, true)
        val seconds = uiState.value.info[exercise.exercise.exerciseId]?.target?.restSeconds ?: defaultRest.value
        session.startRest(seconds, exercise.exercise.name)
    }

    private val _prefillVersion = MutableStateFlow(0)
    /** Bumped after sets were filled from outside the fields, so they re-read their values. */
    val prefillVersion: StateFlow<Int> = _prefillVersion.asStateFlow()

    /** Fills the sets still to do with the suggested load and reps. */
    fun applySuggestion(exercise: WorkoutExerciseWithSets, suggestion: Suggestion) = launch {
        val pending = exercise.sets.filter { !it.isCompleted && !it.isWarmup }.map { it.setId }.toSet()
        if (pending.isEmpty()) return@launch
        repository.prefill(pending, suggestion.weightKg, suggestion.reps)
        // Wait until the new values are visible so the fields re-read them, not the stale ones
        withTimeoutOrNull(2_000) {
            workout.first { w ->
                w?.exercises?.flatMap { it.sets }?.filter { it.setId in pending }
                    ?.all { it.reps == suggestion.reps && it.weightKg == suggestion.weightKg } ?: true
            }
        }
        _prefillVersion.update { it + 1 }
    }

    fun adjustRest(deltaSeconds: Int) = session.adjustRest(deltaSeconds)

    fun skipRest() = session.skipRest()

    override fun toggleWarmup(set: WorkoutSet) = launch { repository.updateWarmup(set.setId, !set.isWarmup) }

    override fun setType(set: WorkoutSet, type: SetType) = launch { repository.updateSetType(set.setId, type) }

    override fun deleteSet(set: WorkoutSet) = launch { repository.deleteSet(set) }

    fun finish() = launch {
        session.skipRest()
        current()?.let { repository.finish(it.workout) }
        // Keeps the backup folder current; silently skipped when none is set
        backupManager.autoBackup()
    }

    fun discard() = launch {
        session.skipRest()
        current()?.let { repository.delete(it.workout) }
    }

    private fun current(): WorkoutWithExercises? = uiState.value.workout

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

