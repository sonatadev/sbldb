package com.github.sonatadev.sbldb.ui.volume

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.domain.MuscleGroupVolume
import com.github.sonatadev.sbldb.domain.VolumeBand
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.DotRow
import com.github.sonatadev.sbldb.ui.components.InfoLink
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.RoundButton
import com.github.sonatadev.sbldb.ui.components.StatusDot
import com.github.sonatadev.sbldb.ui.formatSets
import com.github.sonatadev.sbldb.ui.formatShortDate
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import java.time.temporal.IsoFields

@Composable
fun VolumeScreen(onBack: () -> Unit, onOpenMuscle: (String) -> Unit, onOpenGlossary: (String?) -> Unit, viewModel: VolumeViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val weekNumber = state.week.start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    val weekEnd = state.week.start.plusDays(6)

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.Bottom) {
                Box(Modifier.padding(end = 10.dp, bottom = 8.dp)) { BackButton(onBack) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModuleLabel(
                        stringResource(R.string.volume_label, formatShortDate(state.week.start), formatShortDate(weekEnd)),
                        color = colors.muted
                    )
                    Text("W%02d".format(weekNumber), style = SbldbType.hero(60), color = colors.accent)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    RoundButton("‹", stringResource(R.string.previous_week), onClick = viewModel::previousWeek)
                    RoundButton("›", stringResource(R.string.next_week), onClick = viewModel::nextWeek, enabled = state.weeksAgo > 0)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryModule(stringResource(R.string.in_zone), state.groups.count { it.band == VolumeBand.OPTIMAL }, colors.accent, Modifier.weight(1f))
                SummaryModule(stringResource(R.string.below_zone), state.groups.count { it.band == VolumeBand.LOW }, colors.ink, Modifier.weight(1f))
                SummaryModule(stringResource(R.string.above_zone), state.groups.count { it.band == VolumeBand.HIGH }, colors.ink, Modifier.weight(1f))
            }
        }
        item {
            Module(
                Modifier.fillMaxWidth(),
                label = stringResource(R.string.module_by_muscle),
                trailing = { InfoLink(stringResource(R.string.zone_legend), onClick = { onOpenGlossary("Zone 10–20") }) }
            ) {
                Column {
                    state.groups.forEach { group ->
                        MuscleRow(group, expanded = expanded == group.muscleGroup, onOpenMuscle = { onOpenMuscle(group.muscleGroup) }) {
                            expanded = if (expanded == group.muscleGroup) null else group.muscleGroup
                        }
                    }
                }
            }
        }
        item {
            MonoCaption(stringResource(R.string.volume_explainer_short), Modifier.padding(horizontal = 6.dp))
        }
    }
}

@Composable
private fun SummaryModule(label: String, count: Int, color: Color, modifier: Modifier) {
    Module(modifier.fillMaxHeight(), label = label) {
        Text("$count", style = SbldbType.hero(38), color = if (count == 0) SbldbTheme.colors.dim else color)
    }
}

@Composable
private fun MuscleRow(group: MuscleGroupVolume, expanded: Boolean, onOpenMuscle: () -> Unit, onClick: () -> Unit) {
    val colors = SbldbTheme.colors
    val inZone = group.band == VolumeBand.OPTIMAL
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(group.muscleGroup, style = MaterialTheme.typography.bodyLarge, color = colors.ink, maxLines = 1)
                MonoCaption(stringResource(R.string.frequency_short, group.frequency), color = colors.dim)
            }
            DotRow(group.sets, target = group.target)
            Text(
                formatSets(group.sets),
                style = SbldbType.mono.copy(fontSize = SbldbType.monoLarge.fontSize),
                color = if (inZone) colors.accent else colors.ink,
                textAlign = TextAlign.End,
                modifier = Modifier.width(34.dp)
            )
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (group.regions.isEmpty()) {
                    MonoCaption(stringResource(R.string.no_regions))
                }
                Text(
                    stringResource(R.string.about_muscle, group.muscleGroup) + "  ›",
                    style = SbldbType.mono,
                    color = colors.accent,
                    modifier = Modifier.clickable(onClick = onOpenMuscle).padding(vertical = 6.dp)
                )
                group.regions.forEach { region ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusDot(if (region.sets > 0) colors.accent else colors.empty)
                        Text(region.region, style = MaterialTheme.typography.bodyMedium, color = colors.muted, modifier = Modifier.weight(1f))
                        Text(formatSets(region.sets), style = SbldbType.mono, color = colors.ink)
                    }
                }
            }
        }
    }
}
