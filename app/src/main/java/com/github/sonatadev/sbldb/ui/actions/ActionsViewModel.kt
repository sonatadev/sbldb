package com.github.sonatadev.sbldb.ui.actions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.JointAction
import com.github.sonatadev.sbldb.data.entity.JointActionMuscleRow
import com.github.sonatadev.sbldb.data.entity.JointActionSummary
import com.github.sonatadev.sbldb.data.entity.RatedExercise
import com.github.sonatadev.sbldb.data.repository.JointActionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class JointGroup(val joint: String, val actions: List<JointActionSummary>)

data class ActionsUiState(val query: String = "", val joints: List<JointGroup> = emptyList())

class ActionsViewModel(repository: JointActionRepository) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<ActionsUiState> = combine(repository.summaries, query) { actions, query ->
        val q = query.trim()
        val filtered = actions.filter {
            q.isEmpty() || it.name.contains(q, true) || it.joint.contains(q, true) || it.primaryGroups.orEmpty().contains(q, true)
        }
        // groupBy keeps the library order of the first action of each joint
        ActionsUiState(query, filtered.groupBy { it.joint }.map { (joint, list) -> JointGroup(joint, list) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActionsUiState())

    fun setQuery(value: String) {
        query.value = value
    }
}

data class ActionDetailUiState(
    val action: JointAction? = null,
    val muscles: List<JointActionMuscleRow> = emptyList(),
    val exercises: List<RatedExercise> = emptyList()
)

class ActionDetailViewModel(savedStateHandle: SavedStateHandle, repository: JointActionRepository) : ViewModel() {
    private val actionId: Int = checkNotNull(savedStateHandle["actionId"])

    val uiState: StateFlow<ActionDetailUiState> = combine(
        repository.action(actionId),
        repository.muscles(actionId),
        repository.exercises(actionId),
        ::ActionDetailUiState
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActionDetailUiState())
}
