package com.github.sonatadev.sbldb.ui.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.JointActionSummary
import com.github.sonatadev.sbldb.data.entity.RatedExercise
import com.github.sonatadev.sbldb.data.repository.JointActionRepository
import com.github.sonatadev.sbldb.data.repository.RoutineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MovementPickerUiState(
    val loaded: Boolean = false,
    val query: String = "",
    /** Joint actions matching the search, in library order. */
    val actions: List<JointActionSummary> = emptyList(),
    /** The movement picked in step 1, if any. */
    val action: JointActionSummary? = null,
    /** Exercises for that movement, best first. */
    val exercises: List<RatedExercise> = emptyList()
)

/**
 * Expert flow for building a routine: pick the movement (joint action) first, then the exercise
 * that will train it. With a slot to replace, the chosen exercise takes that slot's place.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MovementPickerViewModel(
    savedStateHandle: SavedStateHandle,
    jointActions: JointActionRepository,
    private val routines: RoutineRepository
) : ViewModel() {
    private val routineId: Long = checkNotNull(savedStateHandle["routineId"])
    private val replaceId: Long? = savedStateHandle.get<Long>("replace")?.takeIf { it > 0 }
    private val preselected: Int? = savedStateHandle.get<Int>("action")?.takeIf { it > 0 }
    private val selected = MutableStateFlow(preselected)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<MovementPickerUiState> = combine(
        jointActions.summaries,
        selected,
        query,
        selected.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else jointActions.exercises(id) }
    ) { all, id, q, exercises ->
        val matching = all.filter {
            q.isBlank() || it.name.contains(q, true) || it.joint.contains(q, true) || it.primaryGroups.orEmpty().contains(q, true)
        }
        MovementPickerUiState(true, q, matching, all.firstOrNull { it.jointActionId == id }, exercises)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovementPickerUiState())

    fun setQuery(text: String) {
        query.value = text
    }

    fun select(actionId: Int) {
        selected.value = actionId
    }

    /** Back from step 2 to step 1; false when the screen itself should close. */
    fun back(): Boolean {
        if (selected.value == null || preselected != null) return false
        selected.value = null
        return true
    }

    fun pick(exerciseId: Int, onDone: () -> Unit) {
        val actionId = selected.value
        viewModelScope.launch {
            if (replaceId != null) routines.replaceExercise(replaceId, exerciseId, actionId)
            else routines.addExercise(routineId, exerciseId, actionId)
            onDone()
        }
    }
}
