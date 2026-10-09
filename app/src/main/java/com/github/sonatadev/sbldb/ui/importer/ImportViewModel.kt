package com.github.sonatadev.sbldb.ui.importer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.importer.ExerciseMatcher
import com.github.sonatadev.sbldb.data.importer.ForeignCsv
import com.github.sonatadev.sbldb.data.importer.ForeignImporter
import com.github.sonatadev.sbldb.data.importer.ForeignLog
import com.github.sonatadev.sbldb.data.importer.ImportResult
import com.github.sonatadev.sbldb.data.importer.Match
import com.github.sonatadev.sbldb.data.importer.MatchCandidate
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One exercise name from the file and what it will become. */
data class MappingRow(
    val foreign: String,
    val sets: Int,
    /** Library exercise it goes to; null = left out. */
    val exerciseId: Int?,
    val exerciseName: String?,
    /** Picked automatically and clear enough: nothing to check. */
    val sure: Boolean,
    val suggestions: List<Match>
)

data class ImportUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val log: ForeignLog? = null,
    val rows: List<MappingRow> = emptyList(),
    val unit: WeightUnit = WeightUnit.KG,
    val importing: Boolean = false,
    val result: ImportResult? = null
) {
    val toCheck: Int get() = rows.count { !it.sure }
    val workoutsWithExercises: Int get() = log?.workouts?.count { w -> w.exercises.any { e -> rows.firstOrNull { it.foreign == e.name }?.exerciseId != null } } ?: 0
}

class ImportViewModel(
    savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val db: AppDatabase,
    settings: SettingsRepository
) : ViewModel() {
    private val uri: Uri = Uri.parse(checkNotNull(savedStateHandle.get<String>("uri")))

    private val state = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = state

    /** The whole library, for picking by hand. */
    var library: List<MatchCandidate> = emptyList()
        private set

    init {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = application.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Can't open the file")
                    val log = ForeignCsv.parse(text)
                    library = ForeignImporter.candidates(db)
                    val sets = log.workouts.flatMap { it.exercises }.groupBy { it.name }.mapValues { (_, e) -> e.sumOf { it.sets.size } }
                    val rows = log.exerciseNames.map { name ->
                        val sure = ExerciseMatcher.sure(name, library)
                        val suggestions = ExerciseMatcher.best(name, library)
                        MappingRow(name, sets[name] ?: 0, sure?.exerciseId, sure?.name, sure != null, suggestions)
                    }.sortedWith(compareBy<MappingRow> { it.sure }.thenByDescending { it.sets })
                    log to rows
                }
            }.onSuccess { (log, rows) ->
                state.value = ImportUiState(loading = false, log = log, rows = rows, unit = settings.weightUnit.first())
            }.onFailure { e ->
                state.value = ImportUiState(loading = false, error = e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun setUnit(unit: WeightUnit) = state.update { it.copy(unit = unit) }

    /** Sends [foreign] to a library exercise, or leaves it out with null. */
    fun map(foreign: String, exerciseId: Int?) = state.update { s ->
        val name = exerciseId?.let { id -> library.firstOrNull { it.exerciseId == id }?.name }
        s.copy(rows = s.rows.map { if (it.foreign == foreign) it.copy(exerciseId = exerciseId, exerciseName = name, sure = true) else it })
    }

    fun import() {
        val s = state.value
        val log = s.log ?: return
        state.update { it.copy(importing = true) }
        viewModelScope.launch {
            val result = ForeignImporter.import(db, log, s.rows.associate { it.foreign to it.exerciseId }, s.unit)
            state.update { it.copy(importing = false, result = result) }
        }
    }
}
