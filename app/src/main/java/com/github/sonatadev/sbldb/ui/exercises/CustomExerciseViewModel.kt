package com.github.sonatadev.sbldb.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.CustomExerciseInput
import com.github.sonatadev.sbldb.data.CustomExercises
import com.github.sonatadev.sbldb.data.MuscleDerivation
import com.github.sonatadev.sbldb.data.entity.JointActionSummary
import com.github.sonatadev.sbldb.data.entity.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomExerciseForm(
    val name: String = "",
    val equipment: String = "Dumbbell",
    val attachment: String = "",
    val note: String = "",
    val aliases: String = "",
    /** Joint action id → rating 1–5, in the order the user added them. */
    val ratings: Map<Int, Int> = linkedMapOf()
)

/** A muscle the exercise would count toward, as derived from the chosen joint actions. */
data class DerivedMuscle(val label: String, val role: Role)

data class CustomExerciseUiState(
    val isNew: Boolean = true,
    val loaded: Boolean = false,
    val form: CustomExerciseForm = CustomExerciseForm(),
    val derived: List<DerivedMuscle> = emptyList(),
    val error: String? = null,
    /** Set once saved or removed: the id of the saved exercise, or -1 after removal. */
    val finishedWith: Int? = null
)

class CustomExerciseViewModel(
    savedStateHandle: SavedStateHandle,
    private val db: AppDatabase
) : ViewModel() {
    private val exerciseId: Int? = savedStateHandle.get<Int>("exerciseId")?.takeIf { it > 0 }

    val actions: StateFlow<List<JointActionSummary>> =
        db.jointActionDAO().getSummaries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(CustomExerciseUiState(isNew = exerciseId == null))
    val uiState: StateFlow<CustomExerciseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val form = exerciseId?.let { id ->
                val exercise = db.exerciseDAO().getExercise(id).first() ?: return@let null
                val ratings = db.jointActionDAO().getActionsForExercise(id).first()
                CustomExerciseForm(
                    name = exercise.name,
                    equipment = exercise.equipment,
                    attachment = exercise.attachment.orEmpty(),
                    note = exercise.note.orEmpty(),
                    aliases = exercise.aliasList.joinToString(", "),
                    ratings = ratings.associateTo(linkedMapOf()) { it.jointActionId to it.rating }
                )
            } ?: CustomExerciseForm()
            _uiState.update { it.copy(loaded = true, form = form) }
            refreshDerived(form.ratings)
        }
    }

    fun edit(transform: (CustomExerciseForm) -> CustomExerciseForm) {
        val before = _uiState.value.form.ratings
        _uiState.update { it.copy(form = transform(it.form), error = null) }
        val after = _uiState.value.form.ratings
        if (after != before) viewModelScope.launch { refreshDerived(after) }
    }

    fun setRating(actionId: Int, rating: Int) = edit { it.copy(ratings = LinkedHashMap(it.ratings).apply { put(actionId, rating) }) }

    fun removeAction(actionId: Int) = edit { it.copy(ratings = LinkedHashMap(it.ratings).apply { remove(actionId) }) }

    private suspend fun refreshDerived(ratings: Map<Int, Int>) {
        val links = if (ratings.isEmpty()) emptyList() else db.userDataDAO().actionMuscles(ratings.keys.toList())
        val roles = MuscleDerivation.combine(links.map { Triple(it.muscleId, it.role, ratings.getValue(it.jointActionId)) })
        val muscles = db.muscleDAO().getAllMuscle().first().associateBy { it.muscleId }
        val derived = roles.mapNotNull { (id, role) ->
            muscles[id]?.let { DerivedMuscle(it.muscleRegion?.let { region -> "${it.muscleGroup} · $region" } ?: it.muscleGroup, role) }
        }.sortedWith(compareBy({ it.role != Role.PRIMARY }, { it.label }))
        _uiState.update { it.copy(derived = derived) }
    }

    fun save() {
        val form = _uiState.value.form
        viewModelScope.launch {
            val input = CustomExerciseInput(
                name = form.name,
                equipment = form.equipment,
                attachment = form.attachment,
                note = form.note,
                aliases = form.aliases.split(',').map { it.trim() },
                ratings = form.ratings
            )
            runCatching { CustomExercises.save(db, input, exerciseId) }
                .onSuccess { id -> _uiState.update { it.copy(finishedWith = id) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun remove() {
        val id = exerciseId ?: return
        viewModelScope.launch {
            val exercise = db.exerciseDAO().getExercise(id).first() ?: return@launch
            runCatching { CustomExercises.remove(db, exercise) }
                .onSuccess { _uiState.update { it.copy(finishedWith = -1) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }
}
