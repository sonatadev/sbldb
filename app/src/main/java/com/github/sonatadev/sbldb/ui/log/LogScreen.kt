package com.github.sonatadev.sbldb.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import com.github.sonatadev.sbldb.domain.CalendarGrid
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.DotRow
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.RoundButton
import com.github.sonatadev.sbldb.ui.components.StatusDot
import com.github.sonatadev.sbldb.ui.formatDuration
import com.github.sonatadev.sbldb.ui.formatSets
import com.github.sonatadev.sbldb.ui.formatTime
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
private val dayFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

@Composable
fun LogScreen(
    onOpenWorkout: (Long) -> Unit,
    onOpenVolume: () -> Unit,
    onOpenPlan: () -> Unit,
    viewModel: LogViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModuleLabel(stringResource(R.string.log_label, state.month.year), color = colors.muted)
                    Text(state.month.format(monthFormatter).uppercase(), style = SbldbType.hero(60), color = colors.accent)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    RoundButton("‹", stringResource(R.string.previous_month), onClick = { viewModel.changeMonth(-1) })
                    RoundButton("›", stringResource(R.string.next_month), onClick = { viewModel.changeMonth(1) }, enabled = state.month < YearMonth.now())
                }
            }
        }
        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_calendar)) {
                Calendar(state.month, state.selected, state.workoutsByDay.mapValues { it.value.size }, viewModel::select)
            }
        }
        item {
            ModuleLabel(state.selected.format(dayFormatter), color = colors.muted, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
        }
        if (state.selectedWorkouts.isEmpty()) {
            item { MonoCaption(stringResource(R.string.rest_day), Modifier.padding(horizontal = 6.dp)) }
        }
        items(state.selectedWorkouts, key = { it.workout.workoutId }) { workout ->
            DayWorkout(workout, onClick = { onOpenWorkout(workout.workout.workoutId) })
        }
        item {
            val stats = state.stats
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_month_stats)) {
                Row(Modifier.fillMaxWidth()) {
                    Stat(stringResource(R.string.sessions), "${stats.sessions}", Modifier.weight(1f))
                    Stat(stringResource(R.string.hard_sets), "${stats.hardSets}", Modifier.weight(1f))
                }
                if (stats.topGroups.isNotEmpty()) {
                    ModuleLabel(stringResource(R.string.top_muscles), color = colors.muted, modifier = Modifier.padding(top = 6.dp))
                    stats.topGroups.forEach { group ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(group.muscleGroup, style = MaterialTheme.typography.bodyLarge, color = colors.ink, modifier = Modifier.weight(1f), maxLines = 1)
                            DotRow(group.sets, target = group.target)
                            Text(formatSets(group.sets), style = SbldbType.mono, color = colors.ink, textAlign = TextAlign.End, modifier = Modifier.width(32.dp))
                        }
                    }
                    MonoCaption(stringResource(R.string.avg_per_week))
                }
                if (stats.byRoutine.isNotEmpty()) {
                    ModuleLabel(stringResource(R.string.by_session), color = colors.muted, modifier = Modifier.padding(top = 6.dp))
                    stats.byRoutine.forEach { (name, count) ->
                        Row {
                            Text(name, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("×$count", style = SbldbType.mono, color = colors.muted)
                        }
                    }
                }
            }
        }
        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_weekly_volume), onClick = onOpenVolume) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.weekly_volume_cta), style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
                    Text("›", style = SbldbType.monoLarge, color = colors.dim)
                }
            }
        }
        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.weekly_plan), onClick = onOpenPlan) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.plan_cta), style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
                    Text("›", style = SbldbType.monoLarge, color = colors.dim)
                }
            }
        }
    }
}

@Composable
private fun Calendar(month: YearMonth, selected: LocalDate, counts: Map<LocalDate, Int>, onSelect: (LocalDate) -> Unit) {
    val colors = SbldbTheme.colors
    val today = LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, style = SbldbType.label, color = colors.dim, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        CalendarGrid.weeks(month).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val isSelected = day == selected
                            val future = day.isAfter(today)
                            val trained = (counts[day] ?: 0) > 0
                            Column(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .then(if (trained) Modifier.background(colors.accent) else Modifier)
                                    .then(
                                        when {
                                            isSelected -> Modifier.border(2.dp, colors.ink, CircleShape)
                                            day == today -> Modifier.border(1.5.dp, colors.accent, CircleShape)
                                            else -> Modifier
                                        }
                                    )
                                    .clickable(enabled = !future, role = Role.Button) { onSelect(day) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "${day.dayOfMonth}",
                                    style = SbldbType.mono,
                                    color = when {
                                        trained -> colors.onAccent
                                        future -> colors.empty
                                        day == today -> colors.accent
                                        else -> colors.ink
                                    }
                                )
                                // A second workout on the same day shows as small dots under the number
                                if ((counts[day] ?: 0) > 1) {
                                    Row(Modifier.height(6.dp).padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        repeat((counts[day] ?: 0).coerceAtMost(3)) { StatusDot(colors.onAccent, size = 4.dp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayWorkout(item: WorkoutWithExercises, onClick: () -> Unit) {
    val colors = SbldbTheme.colors
    val workout = item.workout
    val sets = item.exercises.sumOf { it.sets.size }
    Module(Modifier.fillMaxWidth(), label = formatTime(workout.startedAt), onClick = onClick) {
        Text(workout.name, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            item.exercises.sortedBy { it.workoutExercise.position }.joinToString(" · ") { it.exercise.name },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val duration = workout.endedAt?.let { " · " + formatDuration(it - workout.startedAt) }.orEmpty()
        MonoCaption(pluralStringResource(R.plurals.set_count, sets, sets) + duration)
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ModuleLabel(label, color = SbldbTheme.colors.muted)
        Text(value, style = SbldbType.hero(36), color = SbldbTheme.colors.accent)
    }
}
