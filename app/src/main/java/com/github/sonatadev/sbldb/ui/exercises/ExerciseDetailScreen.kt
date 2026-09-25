package com.github.sonatadev.sbldb.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.entity.MuscleWithRole
import com.github.sonatadev.sbldb.data.entity.Role
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.EffortMeter
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.MonoChip
import com.github.sonatadev.sbldb.ui.components.RatingDots
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.formatDate
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    onOpenAction: (Int) -> Unit,
    onOpenMuscle: (String) -> Unit,
    onEdit: (Int) -> Unit,
    viewModel: ExerciseDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    val exercise = state.exercise ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(
                label = exercise.attachment?.let { "${exercise.equipment} · $it" } ?: exercise.equipment,
                title = exercise.name,
                navigation = { BackButton(onBack) },
                actions = {
                    if (exercise.isCustom) {
                        SecondaryButton(stringResource(R.string.edit), onClick = { onEdit(exercise.exerciseId) }, color = colors.accent)
                    }
                }
            )
        }
        if (exercise.isCustom) {
            item { MonoChip(stringResource(R.string.custom_chip), filled = true, modifier = Modifier.padding(horizontal = 6.dp)) }
        }
        if (exercise.aliasList.isNotEmpty()) {
            item {
                MonoCaption(stringResource(R.string.also_known_as, exercise.aliasList.joinToString(" · ")), Modifier.padding(horizontal = 6.dp))
            }
        }
        exercise.note?.let { note ->
            item {
                Text(note, style = MaterialTheme.typography.bodyLarge, color = colors.muted, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_joint_actions), trailing = { MonoCaption(stringResource(R.string.rating_short)) }) {
                Column {
                    state.actions.forEachIndexed { i, action ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenAction(action.jointActionId) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                MonoCaption(action.joint)
                                Text(action.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                            }
                            RatingDots(action.rating)
                        }
                    }
                }
            }
        }
        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_muscles_2)) {
                MuscleLine(stringResource(R.string.primary), "1.0", state.muscles.filter { it.role == Role.PRIMARY })
                MuscleLine(stringResource(R.string.secondary), "0.5", state.muscles.filter { it.role == Role.SECONDARY })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.muscles.map { it.muscle.muscleGroup }.distinct().forEach { group ->
                        MonoChip("$group ›", onClick = { onOpenMuscle(group) })
                    }
                }
            }
        }
        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.best_e1rm_3)) {
                val best = state.bestE1rmKg
                if (best == null) {
                    Text(stringResource(R.string.no_estimate), style = MaterialTheme.typography.bodyLarge, color = colors.muted)
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(state.unit.format(best), style = SbldbType.hero(52), color = colors.accent)
                        Text(" " + state.unit.label, style = SbldbType.monoLarge, color = colors.muted, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                MonoCaption(stringResource(R.string.e1rm_hint))
            }
        }
        if (state.sessions.isEmpty()) {
            item { MonoCaption(stringResource(R.string.no_history), Modifier.width(300.dp)) }
        }
        items(state.sessions, key = { it.workoutId }) { session ->
            Module(Modifier.fillMaxWidth(), label = formatDate(session.startedAt)) {
                Column {
                    session.sets.forEachIndexed { i, set ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                        Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("%02d".format(i + 1), style = SbldbType.mono, color = colors.dim, modifier = Modifier.width(34.dp))
                            Text(
                                buildString {
                                    if (set.weightKg != null) append("${state.unit.format(set.weightKg)} ${state.unit.label} ")
                                    append("× ${set.reps ?: "–"}")
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.ink,
                                modifier = Modifier.weight(1f)
                            )
                            EffortMeter(set.rir)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleLine(label: String, weight: String, muscles: List<MuscleWithRole>) {
    if (muscles.isEmpty()) return
    val colors = SbldbTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ModuleLabel("$label · $weight", color = colors.muted)
        Text(
            muscles.joinToString { m -> m.muscle.muscleRegion?.let { "${m.muscle.muscleGroup} ($it)" } ?: m.muscle.muscleGroup },
            style = MaterialTheme.typography.bodyLarge,
            color = colors.ink
        )
    }
}
