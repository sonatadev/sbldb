package com.github.sonatadev.sbldb.ui.importer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.importer.ExerciseMatcher
import com.github.sonatadev.sbldb.data.importer.ForeignApp
import com.github.sonatadev.sbldb.data.importer.MatchCandidate
import com.github.sonatadev.sbldb.domain.WeightUnit
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.ModuleRow
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.PrimaryButton
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SearchField
import com.github.sonatadev.sbldb.ui.components.SegmentedControl
import com.github.sonatadev.sbldb.ui.formatDate
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

@Composable
fun ImportScreen(onBack: () -> Unit, viewModel: ImportViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    var picking by remember { mutableStateOf<MappingRow?>(null) }
    picking?.let { row ->
        PickDialog(row, viewModel.library, onPick = { viewModel.map(row.foreign, it) }, onDismiss = { picking = null })
    }
    val log = state.log

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(
                label = when (log?.app) {
                    ForeignApp.STRONG -> stringResource(R.string.import_from, "Strong")
                    ForeignApp.HEVY -> stringResource(R.string.import_from, "Hevy")
                    null -> stringResource(R.string.import_label)
                },
                title = stringResource(R.string.import_title),
                navigation = { BackButton(onBack) }
            )
        }
        if (state.loading) item {
            MonoCaption(stringResource(R.string.import_reading), Modifier.padding(horizontal = 6.dp))
        }
        state.error?.let { error ->
            item {
                Module(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.import_error), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    MonoCaption(error, color = colors.accent)
                }
            }
        }
        val result = state.result
        if (result != null) {
            item {
                Module(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.import_done, result.workouts, result.sets), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    if (result.skipped > 0) MonoCaption(stringResource(R.string.import_skipped, result.skipped))
                    PrimaryButton(stringResource(R.string.summary_done), onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        } else if (log != null) {
            item {
                Text(
                    stringResource(
                        R.string.import_summary,
                        log.workouts.size,
                        formatDate(log.workouts.first().startedAt),
                        formatDate(log.workouts.last().startedAt)
                    ).uppercase(),
                    style = SbldbType.mono,
                    color = colors.muted,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            // Strong doesn't say which unit its weights are in
            if (log.weightIsKg == null) item {
                Module(Modifier.fillMaxWidth(), label = stringResource(R.string.import_unit)) {
                    SegmentedControl(WeightUnit.entries, state.unit, label = { it.label }, onSelect = viewModel::setUnit)
                }
            }
            item {
                Module(
                    Modifier.fillMaxWidth(),
                    label = if (state.toCheck > 0) stringResource(R.string.import_to_check, state.toCheck) else stringResource(R.string.import_exercises)
                ) {
                    Column {
                        state.rows.forEach { row ->
                            ModuleRow(onClick = { picking = row }) {
                                Column(Modifier.weight(1f)) {
                                    MonoCaption(row.foreign + " · " + pluralStringResource(R.plurals.import_sets, row.sets, row.sets), color = colors.dim)
                                    Text(
                                        row.exerciseName ?: stringResource(if (row.sure) R.string.import_left_out else R.string.import_pick),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = when {
                                            !row.sure -> colors.accent
                                            row.exerciseId == null -> colors.muted
                                            else -> colors.ink
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text("›", style = SbldbType.monoLarge, color = colors.dim)
                            }
                        }
                    }
                    MonoCaption(stringResource(R.string.import_hint), Modifier.padding(top = 8.dp))
                }
            }
            item {
                PrimaryButton(
                    if (state.importing) stringResource(R.string.import_running) else stringResource(R.string.import_go, state.workoutsWithExercises),
                    onClick = viewModel::import,
                    enabled = !state.importing && state.workoutsWithExercises > 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Where a name from the file goes: the closest library exercises, a search over all of them, or nowhere. */
@Composable
private fun PickDialog(row: MappingRow, library: List<MatchCandidate>, onPick: (Int?) -> Unit, onDismiss: () -> Unit) {
    val colors = SbldbTheme.colors
    var query by remember { mutableStateOf("") }
    val shown = remember(query) {
        if (query.isBlank()) row.suggestions.map { it.exerciseId to it.name }
        else ExerciseMatcher.best(query, library, limit = 20).map { it.exerciseId to it.name }
    }
    fun pick(id: Int?) {
        onPick(id)
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.module,
        title = { Text(row.foreign, color = colors.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(query, { query = it }, placeholder = stringResource(R.string.import_search))
                if (query.isBlank()) ModuleLabel(stringResource(R.string.import_closest), color = colors.muted)
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(shown, key = { it.first }) { (id, name) ->
                        Text(
                            name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (id == row.exerciseId) colors.accent else colors.ink,
                            modifier = Modifier.fillMaxWidth().clickable { pick(id) }.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { pick(null) }) { Text(stringResource(R.string.import_leave_out), color = colors.muted) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.ink) } }
    )
}
