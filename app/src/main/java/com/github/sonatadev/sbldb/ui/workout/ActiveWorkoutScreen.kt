package com.github.sonatadev.sbldb.ui.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.github.sonatadev.sbldb.domain.RestTimer
import com.github.sonatadev.sbldb.session.WorkoutService
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

    val rest by viewModel.rest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(workout.workout.workoutId) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        WorkoutService.start(context)
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

        if (rest.isRunning) {
            item { RestModule(rest, now, onAdjust = viewModel::adjustRest, onSkip = viewModel::skipRest) }
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
                InfoLink(stringResource(R.string.rir_hint_short), onClick = { onOpenGlossary("RIR") }, modifier = Modifier.weight(1f))
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
                SetRow(set = set, label = label, isCurrent = set == current, unit = unit, targetRir = info.target?.targetRir, actions = viewModel)
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
                    viewModel.logSet(exercise, current)
                    onLogged()
                },
                accentDot = true,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            )
        }
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
internal fun RenameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
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

@Composable
private fun RestModule(rest: RestTimer, now: Long, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    val colors = SbldbTheme.colors
    val remaining = rest.remainingSeconds(now)
    Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_rest)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("%d:%02d".format(remaining / 60, remaining % 60), style = SbldbType.hero(52), color = colors.accent, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SecondaryButton("−15", onClick = { onAdjust(-15) })
                SecondaryButton("+15", onClick = { onAdjust(15) })
                SecondaryButton(stringResource(R.string.skip), onClick = onSkip, color = colors.accent)
            }
        }
        LinearProgressIndicator(
            progress = { rest.progress(now) },
            color = colors.accent,
            trackColor = colors.empty,
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
    }
}
