package com.github.sonatadev.sbldb.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.entity.WorkoutExerciseWithSets
import com.github.sonatadev.sbldb.data.entity.WorkoutSet
import com.github.sonatadev.sbldb.domain.OneRepMax
import com.github.sonatadev.sbldb.domain.WeightUnit
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.CompactNumberField
import com.github.sonatadev.sbldb.ui.components.ConfirmDialog
import com.github.sonatadev.sbldb.ui.components.DotMatrix
import com.github.sonatadev.sbldb.ui.components.EffortMeter
import com.github.sonatadev.sbldb.ui.components.InfoLink
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.MonoChip
import com.github.sonatadev.sbldb.ui.components.PrimaryButton
import com.github.sonatadev.sbldb.ui.components.RoundButton
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.components.StatusDot
import com.github.sonatadev.sbldb.ui.formatClock
import com.github.sonatadev.sbldb.ui.formatSets
import com.github.sonatadev.sbldb.ui.formatTime
import com.github.sonatadev.sbldb.ui.theme.Geist
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import kotlinx.coroutines.delay

/** The exercise to focus on: the first one with a set still to do, else the last one. */
private fun currentExerciseId(exercises: List<WorkoutExerciseWithSets>): Long? =
    (exercises.firstOrNull { e -> e.sets.any { !it.isCompleted } } ?: exercises.lastOrNull())
        ?.workoutExercise?.workoutExerciseId

@Composable
fun ActiveWorkoutScreen(
    onAddExercise: (workoutId: Long) -> Unit,
    onOpenExercise: (exerciseId: Int) -> Unit,
    onOpenGlossary: (String?) -> Unit,
    onClose: () -> Unit,
    viewModel: ActiveWorkoutViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val workout = state.workout
    val colors = SbldbTheme.colors

    // Finished or discarded (or never started): leave the screen
    LaunchedEffect(state.isLoading, workout == null) {
        if (!state.isLoading && workout == null) onClose()
    }
    if (workout == null) return

    var showFinish by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    // null = follow the current exercise automatically
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val exercises = workout.exercises.sortedBy { it.workoutExercise.position }
    val focusedId = expandedId?.takeIf { id -> exercises.any { it.workoutExercise.workoutExerciseId == id } }
        ?: currentExerciseId(exercises)
    val workingSets = exercises.flatMap { it.sets }.filter { !it.isWarmup }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundButton(glyph = "‹", description = stringResource(R.string.back), onClick = onClose)
                Text(
                    workout.workout.name.uppercase(),
                    style = SbldbType.mono,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClickLabel = stringResource(R.string.workout_name)) { showRename = true }
                        .padding(vertical = 10.dp)
                )
                SecondaryButton(stringResource(R.string.finish), onClick = { showFinish = true })
            }
        }

        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Module(Modifier.weight(1.25f).fillMaxHeight(), label = stringResource(R.string.module_time)) {
                    Text(formatClock(now - workout.workout.startedAt), style = SbldbType.hero(48), color = colors.accent)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusDot(colors.accent)
                        MonoCaption(stringResource(R.string.live_since, formatTime(workout.workout.startedAt)))
                    }
                }
                Module(Modifier.weight(1f).fillMaxHeight(), label = stringResource(R.string.module_hard_sets)) {
                    val done = workingSets.count { it.isCompleted }
                    DotMatrix(total = workingSets.size.coerceAtMost(20), done = done.coerceAtMost(20))
                    MonoCaption(stringResource(R.string.sets_done_of, done, workingSets.size))
                }
            }
        }

        if (exercises.isEmpty()) {
            item {
                Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_now)) {
                    Text(stringResource(R.string.empty_workout), style = MaterialTheme.typography.bodyLarge, color = colors.muted)
                }
            }
        }

        items(exercises, key = { it.workoutExercise.workoutExerciseId }) { exercise ->
            val index = exercises.indexOf(exercise) + 3
            val info = state.info[exercise.exercise.exerciseId] ?: ExerciseInfo()
            if (exercise.workoutExercise.workoutExerciseId == focusedId) {
                FocusedExercise(
                    number = index,
                    exercise = exercise,
                    info = info,
                    unit = state.unit,
                    viewModel = viewModel,
                    onOpenExercise = { onOpenExercise(exercise.exercise.exerciseId) },
                    onLogged = { expandedId = null }
                )
            } else {
                CollapsedExercise(index, exercise, onClick = { expandedId = exercise.workoutExercise.workoutExerciseId })
            }
        }

        item {
            SecondaryButton(
                text = "+ " + stringResource(R.string.add_exercise),
                onClick = { onAddExercise(workout.workout.workoutId) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                InfoLink(stringResource(R.string.rir_hint_short), onClick = { onOpenGlossary("RIR") }, modifier = Modifier.weight(1f, fill = false))
                Box(Modifier.weight(1f))
                TextButton(onClick = { showDiscard = true }) {
                    Text(stringResource(R.string.discard).uppercase(), style = SbldbType.mono, color = colors.muted)
                }
            }
        }
    }

    if (showFinish) {
        ConfirmDialog(
            title = stringResource(R.string.finish_title),
            message = stringResource(R.string.finish_message),
            confirmLabel = stringResource(R.string.finish),
            onConfirm = viewModel::finish,
            onDismiss = { showFinish = false }
        )
    }
    if (showDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.discard_title),
            message = stringResource(R.string.discard_message),
            confirmLabel = stringResource(R.string.discard),
            onConfirm = viewModel::discard,
            onDismiss = { showDiscard = false }
        )
    }
    if (showRename) {
        RenameDialog(
            initial = workout.workout.name,
            onConfirm = viewModel::rename,
            onDismiss = { showRename = false }
        )
    }
}

private val SetColumn = 30.dp
private val EffortColumn = 96.dp
private val ActionColumn = 32.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusedExercise(
    number: Int,
    exercise: WorkoutExerciseWithSets,
    info: ExerciseInfo,
    unit: WeightUnit,
    viewModel: ActiveWorkoutViewModel,
    onOpenExercise: () -> Unit,
    onLogged: () -> Unit
) {
    val colors = SbldbTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    val sets = exercise.sets.sortedBy { it.position }
    val current = sets.firstOrNull { !it.isCompleted }

    Module(
        modifier = Modifier.fillMaxWidth(),
        label = "%02d · %s".format(number, stringResource(R.string.module_now)),
        trailing = {
            Box {
                Text(
                    "•••",
                    style = SbldbType.mono,
                    color = colors.muted,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.remove_exercise)) { menuOpen = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = colors.module) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove_exercise), color = colors.ink) },
                        onClick = {
                            menuOpen = false
                            viewModel.removeExercise(exercise.workoutExercise)
                        }
                    )
                }
            }
        }
    ) {
        Text(
            exercise.exercise.name,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.ink,
            modifier = Modifier.clickable(onClick = onOpenExercise)
        )
        if (info.muscles.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                info.muscles.forEach { chip ->
                    MonoChip("${chip.group} ${formatSets(chip.weight).let { if ('.' in it) it else "$it.0" }}", filled = chip.isPrimary)
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModuleLabel(stringResource(R.string.col_set), Modifier.width(SetColumn))
            ModuleLabel(stringResource(R.string.col_load, unit.label), Modifier.weight(1f))
            ModuleLabel(stringResource(R.string.col_effort), Modifier.width(EffortColumn))
            Box(Modifier.width(ActionColumn))
        }

        Column {
            var workingIndex = 0
            sets.forEach { set ->
                val label = if (set.isWarmup) null else ++workingIndex
                SetRow(set = set, label = label, isCurrent = set == current, unit = unit, targetRir = info.target?.targetRir, viewModel = viewModel)
            }
        }

        info.target?.let { target ->
            MonoCaption(
                buildString {
                    append(stringResource(R.string.target))
                    append(" ${target.repMin}–${target.repMax}")
                    target.targetRir?.let { append(" @ RIR $it") }
                },
                color = colors.accent
            )
        }
        val last = info.previous.firstOrNull()
        val lastE1rm = info.previous.mapNotNull { OneRepMax.epley(it.weightKg, it.reps) }.maxOrNull()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonoCaption(
                if (last == null) stringResource(R.string.first_time)
                else buildString {
                    append(stringResource(R.string.last_time))
                    append(" ")
                    append(last.weightKg?.let { unit.format(it) + " × " } ?: "× ")
                    append(last.reps ?: "–")
                    if (lastE1rm != null) append(" · e1RM ${unit.format(lastE1rm)}")
                },
                Modifier.weight(1f)
            )
            Text(
                "+ " + stringResource(R.string.add_set).uppercase(),
                style = SbldbType.mono,
                color = colors.ink,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button) { viewModel.addSet(exercise) }
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            )
        }

        if (current != null) {
            val currentNumber = sets.filter { !it.isWarmup }.indexOf(current) + 1
            PrimaryButton(
                text = if (current.isWarmup) stringResource(R.string.log_warmup) else stringResource(R.string.log_set, currentNumber),
                onClick = {
                    viewModel.toggleCompleted(current)
                    onLogged()
                },
                accentDot = true,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SetRow(
    set: WorkoutSet,
    label: Int?,
    isCurrent: Boolean,
    unit: WeightUnit,
    targetRir: Int?,
    viewModel: ActiveWorkoutViewModel
) {
    val colors = SbldbTheme.colors
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (set.isCompleted) 44.dp else 54.dp)
                .then(if (set.isCompleted) Modifier.background(colors.accentTint, MaterialTheme.shapes.small) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label?.let { "%02d".format(it) } ?: stringResource(R.string.warmup_short),
                style = SbldbType.mono,
                color = when {
                    isCurrent -> colors.accent
                    set.isWarmup -> colors.muted
                    else -> colors.dim
                },
                modifier = Modifier
                    .width(SetColumn)
                    .clickable(onClickLabel = stringResource(R.string.toggle_warmup)) { viewModel.toggleWarmup(set) }
                    .padding(vertical = 12.dp)
            )
            if (set.isCompleted) {
                Text(
                    buildString {
                        if (set.weightKg != null) append("${unit.format(set.weightKg)} ${unit.label} ")
                        append("× ${set.reps ?: "–"}")
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    color = colors.ink,
                    modifier = Modifier.weight(1f)
                )
                Row(Modifier.width(EffortColumn), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EffortMeter(set.rir)
                    Text(set.rir?.let { "RIR $it" } ?: "RIR –", style = SbldbType.label, color = colors.muted)
                }
                Box(
                    Modifier
                        .size(ActionColumn)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClickLabel = stringResource(R.string.undo_set)) { viewModel.toggleCompleted(set) },
                    contentAlignment = Alignment.Center
                ) { StatusDot(colors.accent, size = 8.dp) }
            } else {
                EditableLoad(set, unit, viewModel, Modifier.weight(1f))
                EditableEffort(set, targetRir, viewModel, Modifier.width(EffortColumn))
                Box(
                    Modifier
                        .size(ActionColumn)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClickLabel = stringResource(R.string.delete_set)) { viewModel.deleteSet(set) },
                    contentAlignment = Alignment.Center
                ) { Text("×", style = SbldbType.monoLarge, color = colors.dim) }
            }
        }
    }
}

@Composable
private fun EditableLoad(set: WorkoutSet, unit: WeightUnit, viewModel: ActiveWorkoutViewModel, modifier: Modifier) {
    val colors = SbldbTheme.colors
    // Local text state keyed on the set: DB re-emissions while typing must not reset the cursor
    var weight by rememberSaveable(set.setId, unit) { mutableStateOf(set.weightKg?.let(unit::format).orEmpty()) }
    var reps by rememberSaveable(set.setId) { mutableStateOf(set.reps?.toString().orEmpty()) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CompactNumberField(
            value = weight,
            onValueChange = {
                weight = it
                viewModel.updateWeight(set, it.toDoubleOrNull()?.let(unit::toKg))
            },
            placeholder = unit.label,
            decimal = true,
            modifier = Modifier.weight(1.3f)
        )
        Text("×", color = colors.dim, fontFamily = Geist, fontSize = 16.sp)
        CompactNumberField(
            value = reps,
            onValueChange = {
                reps = it
                viewModel.updateReps(set, it.toIntOrNull())
            },
            placeholder = stringResource(R.string.col_reps).lowercase(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EditableEffort(set: WorkoutSet, targetRir: Int?, viewModel: ActiveWorkoutViewModel, modifier: Modifier) {
    var rir by rememberSaveable(set.setId) { mutableStateOf(set.rir?.toString().orEmpty()) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        EffortMeter(rir.toIntOrNull() ?: set.rir, outlineOnly = true)
        CompactNumberField(
            value = rir,
            onValueChange = {
                rir = it
                viewModel.updateRir(set, it.toIntOrNull())
            },
            placeholder = targetRir?.toString() ?: stringResource(R.string.col_rir),
            textStyle = SbldbType.mono.copy(fontSize = 14.sp),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CollapsedExercise(number: Int, exercise: WorkoutExerciseWithSets, onClick: () -> Unit) {
    val colors = SbldbTheme.colors
    val working = exercise.sets.filter { !it.isWarmup }
    val allDone = exercise.sets.isNotEmpty() && exercise.sets.all { it.isCompleted }
    Module(
        modifier = Modifier.fillMaxWidth(),
        label = "%02d · %s".format(number, stringResource(if (allDone) R.string.module_done else R.string.module_next)),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                exercise.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (allDone) colors.muted else colors.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                working.forEach { set ->
                    StatusDot(if (set.isCompleted) colors.accent else colors.empty, size = 9.dp)
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = SbldbTheme.colors
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.module,
        title = { Text(stringResource(R.string.workout_name), color = colors.ink) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name); onDismiss() }) {
                Text(stringResource(android.R.string.ok), color = colors.accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.ink) } }
    )
}
