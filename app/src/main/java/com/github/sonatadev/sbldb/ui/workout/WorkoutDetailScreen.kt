package com.github.sonatadev.sbldb.ui.workout

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.entity.Workout
import com.github.sonatadev.sbldb.data.entity.WorkoutExerciseWithSets
import com.github.sonatadev.sbldb.domain.WeightUnit
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.CompactNumberField
import com.github.sonatadev.sbldb.ui.components.ConfirmDialog
import com.github.sonatadev.sbldb.ui.components.EffortMeter
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.PrimaryButton
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.formatDate
import com.github.sonatadev.sbldb.ui.formatDuration
import com.github.sonatadev.sbldb.ui.formatTime
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit,
    onOpenExercise: (Int) -> Unit,
    onAddExercise: (workoutId: Long) -> Unit,
    viewModel: WorkoutDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    var showDelete by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isLoading, state.workout == null) {
        if (!state.isLoading && state.workout == null) onBack()
    }
    val item = state.workout ?: return
    val workout = item.workout
    val exercises = item.exercises.sortedBy { it.workoutExercise.position }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            val duration = workout.endedAt?.let { " · " + formatDuration(it - workout.startedAt) }.orEmpty()
            ScreenHeader(
                label = formatDate(workout.startedAt) + duration,
                title = workout.name,
                navigation = { BackButton(onBack) },
                actions = {
                    SecondaryButton(
                        if (editing) stringResource(R.string.done) else stringResource(R.string.edit),
                        onClick = { editing = !editing },
                        color = colors.accent
                    )
                }
            )
        }
        if (editing) {
            item {
                WhenModule(workout, onRename = { showRename = true }, onChange = viewModel::updateTimes)
            }
        }
        items(exercises, key = { it.workoutExercise.workoutExerciseId }) { exercise ->
            val number = exercises.indexOf(exercise) + 1
            if (editing) {
                EditableExercise(number, exercise, state.unit, viewModel, onOpenExercise = { onOpenExercise(exercise.exercise.exerciseId) })
            } else {
                ReadOnlyExercise(number, exercise, state.unit, onOpenExercise = { onOpenExercise(exercise.exercise.exerciseId) })
            }
        }
        item {
            if (editing) {
                PrimaryButton(
                    "+ " + stringResource(R.string.add_exercise),
                    onClick = { onAddExercise(viewModel.workoutId) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                SecondaryButton(
                    stringResource(R.string.delete_workout),
                    onClick = { showDelete = true },
                    color = colors.muted,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_workout),
            message = stringResource(R.string.delete_workout_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = viewModel::delete,
            onDismiss = { showDelete = false }
        )
    }
    if (showRename) {
        RenameDialog(initial = workout.name, onConfirm = viewModel::rename, onDismiss = { showRename = false })
    }
}

@Composable
private fun ReadOnlyExercise(number: Int, exercise: WorkoutExerciseWithSets, unit: WeightUnit, onOpenExercise: () -> Unit) {
    val colors = SbldbTheme.colors
    Module(Modifier.fillMaxWidth(), label = "%02d".format(number)) {
        Text(
            exercise.exercise.name,
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
            modifier = Modifier.clickable(onClick = onOpenExercise)
        )
        Column {
            var workingIndex = 0
            exercise.sets.sortedBy { it.position }.forEach { set ->
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (set.isWarmup) stringResource(R.string.warmup_short) else "%02d".format(++workingIndex),
                        style = SbldbType.mono,
                        color = colors.dim,
                        modifier = Modifier.width(34.dp)
                    )
                    Text(
                        buildString {
                            if (set.weightKg != null) append("${unit.format(set.weightKg)} ${unit.label} ")
                            append("× ${set.reps ?: "–"}")
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    if (!set.isWarmup) {
                        EffortMeter(set.rir)
                        Text(set.rir?.let { "  RIR $it" } ?: "  RIR –", style = SbldbType.label, color = colors.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableExercise(
    number: Int,
    exercise: WorkoutExerciseWithSets,
    unit: WeightUnit,
    viewModel: WorkoutDetailViewModel,
    onOpenExercise: () -> Unit
) {
    val colors = SbldbTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    Module(
        Modifier.fillMaxWidth(),
        label = "%02d".format(number),
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
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
            modifier = Modifier.clickable(onClick = onOpenExercise)
        )
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModuleLabel(stringResource(R.string.col_set), Modifier.width(SetColumn))
            ModuleLabel(stringResource(R.string.col_load, unit.label), Modifier.weight(1f))
            ModuleLabel(stringResource(R.string.col_effort), Modifier.width(EffortColumn))
            Box(Modifier.width(ActionColumn))
        }
        Column {
            var workingIndex = 0
            exercise.sets.sortedBy { it.position }.forEach { set ->
                val label = if (set.isWarmup) null else ++workingIndex
                SetRow(set = set, label = label, isCurrent = false, unit = unit, targetRir = null, actions = viewModel, alwaysEditable = true)
            }
        }
        Text(
            "+ " + stringResource(R.string.add_set).uppercase(),
            style = SbldbType.mono,
            color = colors.accent,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button) { viewModel.addSet(exercise) }
                .padding(horizontal = 6.dp, vertical = 8.dp)
        )
    }
}

/** Name, date, start time and duration of a finished workout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhenModule(workout: Workout, onRename: () -> Unit, onChange: (startedAt: Long, durationMillis: Long) -> Unit) {
    val colors = SbldbTheme.colors
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(workout.startedAt).atZone(zone)
    val durationMillis = (workout.endedAt ?: workout.startedAt) - workout.startedAt
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var minutes by rememberSaveable(workout.workoutId) { mutableStateOf((durationMillis / 60_000).toString()) }

    fun moveTo(date: LocalDate, time: LocalTime) =
        onChange(date.atTime(time).atZone(zone).toInstant().toEpochMilli(), durationMillis)

    Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_when)) {
        EditRow(stringResource(R.string.workout_name), workout.name, onRename)
        EditRow(stringResource(R.string.date), formatDate(workout.startedAt)) { pickDate = true }
        EditRow(stringResource(R.string.start_time), formatTime(workout.startedAt)) { pickTime = true }
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.duration_minutes), style = MaterialTheme.typography.bodyLarge, color = colors.ink, modifier = Modifier.weight(1f))
            CompactNumberField(
                value = minutes,
                onValueChange = { text ->
                    minutes = text
                    text.toLongOrNull()?.let { onChange(workout.startedAt, it * 60_000) }
                },
                modifier = Modifier.width(88.dp)
            )
        }
    }

    if (pickDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = start.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        moveTo(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate(), start.toLocalTime())
                    }
                    pickDate = false
                }) { Text(stringResource(android.R.string.ok), color = colors.accent) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel), color = colors.ink) } }
        ) { DatePicker(state = pickerState) }
    }
    if (pickTime) {
        val timeState = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            containerColor = colors.module,
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    moveTo(start.toLocalDate(), LocalTime.of(timeState.hour, timeState.minute))
                    pickTime = false
                }) { Text(stringResource(android.R.string.ok), color = colors.accent) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel), color = colors.ink) } }
        )
    }
}

@Composable
private fun EditRow(label: String, value: String, onClick: () -> Unit) {
    val colors = SbldbTheme.colors
    Row(
        Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.ink, modifier = Modifier.weight(1f))
        Text("$value  ›", style = SbldbType.mono, color = colors.accent)
    }
}
