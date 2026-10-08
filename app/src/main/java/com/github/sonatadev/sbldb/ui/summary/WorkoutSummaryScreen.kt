package com.github.sonatadev.sbldb.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.domain.PrKind
import com.github.sonatadev.sbldb.domain.RecordHit
import com.github.sonatadev.sbldb.domain.WeightUnit
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.DotRow
import com.github.sonatadev.sbldb.ui.components.HeroText
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleRow
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.MonoChip
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.PrimaryButton
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.formatClock
import com.github.sonatadev.sbldb.ui.formatSet
import com.github.sonatadev.sbldb.ui.formatSets
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

@Composable
fun WorkoutSummaryScreen(
    onDone: () -> Unit,
    onOpenWorkout: (Long) -> Unit,
    viewModel: WorkoutSummaryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    // Nothing was kept (no completed set): nothing to sum up
    LaunchedEffect(state.loaded, state.workout == null) { if (state.loaded && state.workout == null) onDone() }
    val workout = state.workout ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModuleLabel(stringResource(R.string.summary_label, workout.workout.name), color = colors.muted)
                HeroText(formatClock((workout.workout.endedAt ?: workout.workout.startedAt) - workout.workout.startedAt), 56, colors.accent)
                Text(
                    stringResource(R.string.summary_line, state.hardSets, workout.exercises.size, state.unit.formatRounded(state.tonnageKg), state.unit.label).uppercase(),
                    style = SbldbType.mono,
                    color = colors.muted
                )
            }
        }
        if (state.records.isNotEmpty()) item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.summary_records, state.records.size)) {
                Column {
                    state.records.forEach { RecordRow(it, state.unit) }
                }
            }
        }
        if (state.muscles.isNotEmpty()) item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.summary_muscles)) {
                Column {
                    state.muscles.forEach { (muscle, today) ->
                        ModuleRow {
                            Column(Modifier.weight(1f)) {
                                Text(muscle.muscleGroup, style = MaterialTheme.typography.bodyLarge, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                MonoCaption("+" + formatSets(today) + " " + stringResource(R.string.summary_today), color = colors.accent)
                            }
                            DotRow(muscle.sets, target = muscle.target)
                            Text(formatSets(muscle.sets), style = SbldbType.mono, color = colors.ink, textAlign = TextAlign.End, modifier = Modifier.width(32.dp))
                        }
                    }
                    MonoCaption(stringResource(R.string.summary_muscles_hint), Modifier.padding(top = 8.dp))
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                PrimaryButton(stringResource(R.string.summary_done), onClick = onDone, modifier = Modifier.fillMaxWidth())
                SecondaryButton(stringResource(R.string.summary_details), onClick = { onOpenWorkout(workout.workout.workoutId) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RecordRow(hit: RecordHit, unit: WeightUnit) {
    val colors = SbldbTheme.colors
    ModuleRow {
        Column(Modifier.weight(1f)) {
            Text(hit.name, style = MaterialTheme.typography.bodyLarge, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MonoCaption(formatSet(hit.weightKg, hit.reps, null, unit))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            PrKind.entries.filter { it in hit.kinds }.forEach { kind ->
                MonoChip(
                    stringResource(
                        when (kind) {
                            PrKind.E1RM -> R.string.pr_e1rm
                            PrKind.WEIGHT -> R.string.pr_weight
                            PrKind.REPS -> R.string.pr_reps
                        }
                    ),
                    filled = true
                )
            }
        }
    }
}
