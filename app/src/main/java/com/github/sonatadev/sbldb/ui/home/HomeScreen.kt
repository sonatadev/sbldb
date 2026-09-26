package com.github.sonatadev.sbldb.ui.home

import com.github.sonatadev.sbldb.ui.components.HeroText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.entity.RoutineWithExercises
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.DotRow
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.PrimaryButton
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.formatClock
import com.github.sonatadev.sbldb.ui.formatDate
import com.github.sonatadev.sbldb.ui.formatDuration
import com.github.sonatadev.sbldb.ui.formatSets
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.weekdayLetters
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthName: DateTimeFormatter get() = DateTimeFormatter.ofPattern("LLLL", Locale.getDefault())
private val heroDate: DateTimeFormatter get() = DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())

@Composable
fun HomeScreen(
    onOpenActiveWorkout: () -> Unit,
    onEditRoutine: (Long) -> Unit,
    onOpenVolume: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenWorkout: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    val today = LocalDate.now()
    val newRoutineName = stringResource(R.string.new_routine_name, state.routines.size + 1)

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(
                label = stringResource(
                    R.string.home_label,
                    today.format(monthName),
                    pluralStringResource(R.plurals.session_count, state.sessionsThisWeek, state.sessionsThisWeek)
                ),
                title = today.format(heroDate).uppercase(),
                titleHero = true
            )
        }

        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_this_week)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeekStrip(state.week.start, today, state.trainedDays, Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                        Text("${state.hardSetsThisWeek}", style = SbldbType.hero(40), color = colors.accent)
                        MonoCaption(stringResource(R.string.hard_sets))
                    }
                }
            }
        }

        item {
            val active = state.activeWorkout
            val next = state.nextRoutine
            Module(
                Modifier.fillMaxWidth(),
                label = stringResource(if (active != null) R.string.module_in_progress else R.string.module_next_up),
                trailing = if (active == null && state.nextIsPlanned) ({ MonoCaption(stringResource(R.string.planned_today), color = colors.accent) }) else null
            ) {
                when {
                    active != null -> {
                        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                        LaunchedEffect(active.workoutId) {
                            while (true) {
                                now = System.currentTimeMillis()
                                delay(1_000)
                            }
                        }
                        Text(active.name, style = MaterialTheme.typography.headlineSmall, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        HeroText(formatClock(now - active.startedAt), 44, colors.accent)
                        PrimaryButton(stringResource(R.string.resume_workout), onClick = onOpenActiveWorkout, accentDot = true, modifier = Modifier.fillMaxWidth())
                    }
                    next != null -> {
                        NextRoutine(next)
                        PrimaryButton(
                            stringResource(R.string.start_routine, next.routine.name),
                            onClick = { viewModel.startRoutine(next.routine.routineId, onOpenActiveWorkout) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        SecondaryButton(stringResource(R.string.start_empty_workout), onClick = { viewModel.startEmpty(onOpenActiveWorkout) }, modifier = Modifier.fillMaxWidth())
                    }
                    else -> {
                        Text(stringResource(R.string.ready_to_train), style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                        MonoCaption(stringResource(R.string.no_routine_hint))
                        PrimaryButton(stringResource(R.string.start_empty_workout), onClick = { viewModel.startEmpty(onOpenActiveWorkout) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_volume_check), onClick = onOpenVolume) {
                if (state.lagging.isEmpty()) {
                    Text(stringResource(R.string.all_in_zone), style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                } else {
                    MonoCaption(stringResource(R.string.lagging_hint))
                    state.lagging.forEach { group ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(group.muscleGroup, style = MaterialTheme.typography.bodyLarge, color = colors.ink, modifier = Modifier.weight(1f), maxLines = 1)
                            DotRow(group.sets, target = group.target)
                            Text(formatSets(group.sets), style = SbldbType.mono, color = colors.ink, textAlign = TextAlign.End, modifier = Modifier.width(32.dp))
                        }
                    }
                }
            }
        }

        state.lastSession?.let { last ->
            item {
                val sets = last.exercises.sumOf { it.sets.size }
                Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_last_session), onClick = { onOpenWorkout(last.workout.workoutId) }) {
                    Text(last.workout.name, style = MaterialTheme.typography.titleLarge, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val duration = last.workout.endedAt?.let { " · " + formatDuration(it - last.workout.startedAt) }.orEmpty()
                    MonoCaption(formatDate(last.workout.startedAt) + " · " + pluralStringResource(R.plurals.set_count, sets, sets) + duration)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ModuleLabel(stringResource(R.string.module_routines), color = colors.muted, modifier = Modifier.weight(1f))
                if (state.routines.isNotEmpty()) {
                    Text(
                        stringResource(R.string.weekly_plan).uppercase() + "  ›",
                        style = SbldbType.mono,
                        color = colors.accent,
                        modifier = Modifier.clickable(onClick = onOpenPlan).padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
        itemsIndexed(state.routines, key = { _, r -> r.routine.routineId }) { _, routine ->
            val sets = routine.exercises.sumOf { it.routineExercise.sets }
            Module(Modifier.fillMaxWidth(), onClick = { onEditRoutine(routine.routine.routineId) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(routine.routine.name, style = MaterialTheme.typography.titleLarge, color = colors.ink)
                        MonoCaption(
                            pluralStringResource(R.plurals.exercise_count, routine.exercises.size, routine.exercises.size) +
                                " · " + pluralStringResource(R.plurals.set_count, sets, sets)
                        )
                    }
                    if (state.activeWorkout == null && routine.exercises.isNotEmpty()) {
                        SecondaryButton(
                            stringResource(R.string.start),
                            onClick = { viewModel.startRoutine(routine.routine.routineId, onOpenActiveWorkout) },
                            color = colors.accent
                        )
                    }
                }
            }
        }
        item {
            SecondaryButton(
                text = "+ " + stringResource(R.string.new_routine),
                onClick = { viewModel.createRoutine(newRoutineName, onEditRoutine) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }
    }
}

@Composable
private fun NextRoutine(routine: RoutineWithExercises) {
    val colors = SbldbTheme.colors
    val sets = routine.exercises.sumOf { it.routineExercise.sets }
    Text(routine.routine.name, style = SbldbType.hero(44), color = colors.accent, maxLines = 1)
    Text(
        routine.exercises.sortedBy { it.routineExercise.position }.joinToString(" · ") { it.exercise.name },
        style = MaterialTheme.typography.bodyMedium,
        color = colors.muted,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    MonoCaption(
        pluralStringResource(R.plurals.exercise_count, routine.exercises.size, routine.exercises.size) +
            " · " + pluralStringResource(R.plurals.set_count, sets, sets)
    )
}

/** Monday-to-Sunday dots: filled accent = trained, ring = today. */
@Composable
private fun WeekStrip(monday: LocalDate, today: LocalDate, trained: Set<LocalDate>, modifier: Modifier) {
    val colors = SbldbTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        weekdayLetters().forEachIndexed { i, letter ->
            val day = monday.plusDays(i.toLong())
            val done = day in trained
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(letter, style = SbldbType.label, color = if (day == today) colors.accent else colors.dim)
                Box(
                    Modifier
                        .size(22.dp)
                        .then(
                            when {
                                done -> Modifier.background(colors.accent, CircleShape)
                                day == today -> Modifier.border(2.dp, colors.accent, CircleShape)
                                day.isAfter(today) -> Modifier.border(1.dp, colors.line, CircleShape)
                                else -> Modifier.background(colors.empty, CircleShape)
                            }
                        )
                )
            }
        }
    }
}
