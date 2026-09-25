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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.ConfirmDialog
import com.github.sonatadev.sbldb.ui.components.EffortMeter
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.formatDate
import com.github.sonatadev.sbldb.ui.formatDuration
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit,
    onOpenExercise: (Int) -> Unit,
    viewModel: WorkoutDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoading, state.workout == null) {
        if (!state.isLoading && state.workout == null) onBack()
    }
    val item = state.workout ?: return
    val workout = item.workout

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            val duration = workout.endedAt?.let { " · " + formatDuration(it - workout.startedAt) }.orEmpty()
            ScreenHeader(
                label = formatDate(workout.startedAt) + duration,
                title = workout.name,
                navigation = { BackButton(onBack) }
            )
        }
        items(item.exercises.sortedBy { it.workoutExercise.position }, key = { it.workoutExercise.workoutExerciseId }) { exercise ->
            Module(Modifier.fillMaxWidth(), label = "%02d".format(item.exercises.indexOf(exercise) + 1)) {
                Text(
                    exercise.exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink,
                    modifier = Modifier.clickable { onOpenExercise(exercise.exercise.exerciseId) }
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
                                    if (set.weightKg != null) append("${state.unit.format(set.weightKg)} ${state.unit.label} ")
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
        item {
            SecondaryButton(
                stringResource(R.string.delete_workout),
                onClick = { showDelete = true },
                color = colors.muted,
                modifier = Modifier.fillMaxWidth()
            )
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
}
