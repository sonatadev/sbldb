package com.github.sonatadev.sbldb.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.data.repository.JointActionRepository
import com.github.sonatadev.sbldb.data.repository.RoutineRepository
import com.github.sonatadev.sbldb.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [matchedAlias] is set when the search matched another name rather than the exercise name. */
data class ExerciseListItem(val exercise: Exercise, val primaryGroups: List<String>, val matchedAlias: String? = null)

data class ExerciseListUiState(
    val query: String = "",
    val selectedJoint: String? = null,
    val joints: List<String> = emptyList(),
    val exercises: List<ExerciseListItem> = emptyList()
)

/** Where a picked exercise goes. */
enum class PickTarget { WORKOUT, ROUTINE }

/** Browses all exercises; opened with a pick target it adds the tapped exercise there instead. */
class ExerciseListViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ExerciseRepository,
    jointActions: JointActionRepository,
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository
) : ViewModel() {
    val pickTarget: PickTarget? = savedStateHandle.get<String>("kind")?.let { PickTarget.valueOf(it.uppercase()) }
    private val targetId: Long? = savedStateHandle.get<Long>("targetId")

    private val query = MutableStateFlow("")
    private val selectedJoint = MutableStateFlow<String?>(null)

    private val filters = combine(query, selectedJoint) { q, j -> q to j }

    val uiState: StateFlow<ExerciseListUiState> = combine(
        repository.exercises,
        repository.primaryGroups,
        jointActions.exerciseJoints,
        jointActions.summaries,
        filters
    ) { exercises, primary, exerciseJoints, actions, (query, joint) ->
        val groupsByExercise = primary.groupBy({ it.exerciseId }, { it.muscleGroup })
        val jointsByExercise = exerciseJoints.groupBy({ it.exerciseId }, { it.muscleGroup })
        val q = query.trim()
        val items = exercises
            .filter { joint == null || joint in jointsByExercise[it.exerciseId].orEmpty() }
            .mapNotNull { exercise ->
                val alias = if (q.isEmpty() || exercise.name.contains(q, ignoreCase = true)) null
                else exercise.aliasList.firstOrNull { it.contains(q, ignoreCase = true) } ?: return@mapNotNull null
                ExerciseListItem(exercise, groupsByExercise[exercise.exerciseId].orEmpty().sorted(), alias)
            }
        ExerciseListUiState(query, joint, actions.map { it.joint }.distinct(), items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseListUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun selectJoint(joint: String?) {
        selectedJoint.value = joint
    }

    fun pick(exerciseId: Int, onDone: () -> Unit) {
        val id = targetId ?: return
        viewModelScope.launch {
            when (pickTarget) {
                PickTarget.WORKOUT -> workoutRepository.addExercise(id, exerciseId)
                PickTarget.ROUTINE -> routineRepository.addExercise(id, exerciseId)
                null -> return@launch
            }
            onDone()
        }
    }
}
