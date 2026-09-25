package com.github.sonatadev.sbldb.ui.routines

import com.github.sonatadev.sbldb.ui.formatSets
import com.github.sonatadev.sbldb.ui.components.MonoChip
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.entity.RoutineExerciseWithExercise
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.ConfirmDialog
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RoutineEditorScreen(
    onBack: () -> Unit,
    onAddExercise: (routineId: Long) -> Unit,
    onOpenPlan: () -> Unit,
    viewModel: RoutineEditorViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val routine by viewModel.routine.collectAsStateWithLifecycle()
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    var showDelete by remember { mutableStateOf(false) }
    val current = routine ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BackButton(onBack)
                ModuleLabel(stringResource(R.string.edit_routine), color = colors.muted)
            }
        }
        item {
            // Local text, saved on every change; keyed on the routine so DB echoes don't move the cursor
            var name by rememberSaveable(current.routine.routineId) { mutableStateOf(current.routine.name) }
            Column(Modifier.padding(horizontal = 6.dp)) {
                BasicTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) viewModel.rename(it)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.edge))
                MonoCaption(stringResource(R.string.routine_name_hint), Modifier.padding(top = 6.dp))
            }
        }
        item {
            Module(
                Modifier.fillMaxWidth(),
                label = stringResource(R.string.module_per_week),
                trailing = {
                    Text(
                        stringResource(R.string.weekly_plan).uppercase() + "  ›",
                        style = SbldbType.mono,
                        color = colors.accent,
                        modifier = Modifier.clickable(onClick = onOpenPlan).padding(vertical = 4.dp)
                    )
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.times_per_week), style = MaterialTheme.typography.bodyLarge, color = colors.ink, modifier = Modifier.weight(1f))
                    Stepper("", current.routine.timesPerWeek, 1..7) { viewModel.setTimesPerWeek(it) }
                }
                if (plan.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        plan.forEach { (group, sets) -> MonoChip("$group ${formatSets(sets)}") }
                    }
                    MonoCaption(stringResource(R.string.per_week_hint))
                }
            }
        }
        if (current.exercises.isEmpty()) {
            item { MonoCaption(stringResource(R.string.routine_empty), Modifier.padding(horizontal = 6.dp)) }
        }
        itemsIndexed(current.exercises, key = { _, e -> e.routineExercise.routineExerciseId }) { index, entry ->
            PlannedExercise(
                number = index + 1,
                entry = entry,
                isFirst = index == 0,
                isLast = index == current.exercises.lastIndex,
                viewModel = viewModel
            )
        }
        item {
            SecondaryButton(
                "+ " + stringResource(R.string.add_exercise),
                onClick = { onAddExercise(viewModel.routineId) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }
        item {
            SecondaryButton(stringResource(R.string.delete_routine), onClick = { showDelete = true }, color = colors.muted, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_routine),
            message = stringResource(R.string.delete_routine_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { viewModel.delete(onBack) },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun PlannedExercise(
    number: Int,
    entry: RoutineExerciseWithExercise,
    isFirst: Boolean,
    isLast: Boolean,
    viewModel: RoutineEditorViewModel
) {
    val colors = SbldbTheme.colors
    val plan = entry.routineExercise
    Module(
        modifier = Modifier.fillMaxWidth(),
        label = "%02d".format(number),
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                GlyphButton("↑", stringResource(R.string.move_up), enabled = !isFirst) { viewModel.move(plan, -1) }
                GlyphButton("↓", stringResource(R.string.move_down), enabled = !isLast) { viewModel.move(plan, 1) }
                GlyphButton("×", stringResource(R.string.remove_exercise)) { viewModel.remove(plan) }
            }
        }
    ) {
        Text(entry.exercise.name, style = MaterialTheme.typography.titleLarge, color = colors.ink)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stepper(stringResource(R.string.sets_label), plan.sets, 1..10) { viewModel.update(plan.copy(sets = it)) }
            Stepper(stringResource(R.string.reps_min), plan.repMin, 1..plan.repMax) { viewModel.update(plan.copy(repMin = it)) }
            Stepper(stringResource(R.string.reps_max), plan.repMax, plan.repMin..50) { viewModel.update(plan.copy(repMax = it)) }
            Stepper(stringResource(R.string.col_rir), plan.targetRir ?: 0, 0..5) { viewModel.update(plan.copy(targetRir = it)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModuleLabel(stringResource(R.string.rest_label), color = colors.muted, modifier = Modifier.weight(1f))
            GlyphButton("−", stringResource(R.string.rest_label) + " −", enabled = plan.restSeconds > 15) {
                viewModel.update(plan.copy(restSeconds = plan.restSeconds - 15))
            }
            Text("%d:%02d".format(plan.restSeconds / 60, plan.restSeconds % 60), style = SbldbType.monoLarge, color = colors.ink, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
            GlyphButton("+", stringResource(R.string.rest_label) + " +", enabled = plan.restSeconds < 600) {
                viewModel.update(plan.copy(restSeconds = plan.restSeconds + 15))
            }
        }
    }
}

/** Label above "−  value  +". */
@Composable
private fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val colors = SbldbTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (label.isNotEmpty()) ModuleLabel(label, color = colors.muted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphButton("−", "$label −", enabled = value > range.first) { onChange(value - 1) }
            Text("$value", style = SbldbType.monoLarge, color = colors.ink, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
            GlyphButton("+", "$label +", enabled = value < range.last) { onChange(value + 1) }
        }
    }
}

@Composable
private fun GlyphButton(glyph: String, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = SbldbTheme.colors
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, style = SbldbType.monoLarge, color = if (enabled) colors.ink else colors.empty)
    }
}
