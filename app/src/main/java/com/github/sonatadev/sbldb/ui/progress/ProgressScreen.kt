package com.github.sonatadev.sbldb.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.domain.ExerciseProgress
import com.github.sonatadev.sbldb.domain.LoadMetric
import com.github.sonatadev.sbldb.domain.Trend
import com.github.sonatadev.sbldb.domain.WeightUnit
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleRow
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SegmentedControl
import com.github.sonatadev.sbldb.ui.components.Sparkline
import com.github.sonatadev.sbldb.ui.shortName
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(onBack: () -> Unit, onOpenExercise: (Int) -> Unit, viewModel: ProgressViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    val periodNames = mapOf(
        ProgressPeriod.MONTH to stringResource(R.string.period_month),
        ProgressPeriod.QUARTER to stringResource(R.string.period_quarter),
        ProgressPeriod.YEAR to stringResource(R.string.period_year)
    )
    val metricNames = LoadMetric.entries.associateWith { stringResource(it.shortName) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(
                label = stringResource(R.string.progress_label),
                title = stringResource(R.string.progress_title),
                navigation = { BackButton(onBack) }
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentedControl(ProgressPeriod.entries, state.period, label = { periodNames.getValue(it) }, onSelect = viewModel::setPeriod)
                SegmentedControl(LoadMetric.entries, state.metric, label = { metricNames.getValue(it) }, onSelect = viewModel::setMetric)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TrendCount(stringResource(R.string.trend_up), state.exercises.count { it.trend == Trend.UP }, colors.accent, Modifier.weight(1f))
                TrendCount(stringResource(R.string.trend_flat), state.exercises.count { it.trend == Trend.FLAT }, colors.ink, Modifier.weight(1f))
                TrendCount(stringResource(R.string.trend_down), state.exercises.count { it.trend == Trend.DOWN }, colors.ink, Modifier.weight(1f))
            }
        }
        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_by_exercise)) {
                if (state.loaded && state.exercises.isEmpty()) {
                    Text(stringResource(R.string.progress_empty), style = MaterialTheme.typography.bodyLarge, color = colors.muted)
                }
                Column {
                    state.exercises.forEach { exercise ->
                        ExerciseTrendRow(exercise, state.unit, onClick = { onOpenExercise(exercise.exerciseId) })
                    }
                }
            }
        }
        item {
            MonoCaption(stringResource(R.string.progress_hint), Modifier.padding(horizontal = 6.dp))
        }
    }
}

@Composable
private fun TrendCount(label: String, count: Int, color: Color, modifier: Modifier) {
    Module(modifier.fillMaxHeight(), label = label) {
        Text("$count", style = SbldbType.hero(38), color = if (count == 0) SbldbTheme.colors.dim else color)
    }
}

@Composable
private fun ExerciseTrendRow(exercise: ExerciseProgress, unit: WeightUnit, onClick: () -> Unit) {
    val colors = SbldbTheme.colors
    ModuleRow(onClick = onClick) {
        Column(Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.bodyLarge, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MonoCaption(
                pluralStringResource(R.plurals.session_count, exercise.series.size, exercise.series.size) +
                    " · " + unit.formatRounded(exercise.series.last().second) + " " + unit.label,
                color = colors.dim
            )
        }
        Sparkline(exercise.series, Modifier.size(width = 64.dp, height = 28.dp))
        Text(
            changeLabel(exercise),
            style = SbldbType.mono,
            color = if (exercise.trend == Trend.UP) colors.accent else if (exercise.trend == Trend.NEW) colors.dim else colors.ink,
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun changeLabel(exercise: ExerciseProgress): String {
    val change = exercise.change ?: return stringResource(R.string.trend_new)
    val percent = (change * 100).roundToInt()
    return when {
        percent > 0 -> "+$percent%"
        percent < 0 -> "−${abs(percent)}%"
        else -> "0%"
    }
}
