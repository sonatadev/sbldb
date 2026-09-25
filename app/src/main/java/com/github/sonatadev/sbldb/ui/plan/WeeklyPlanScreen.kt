package com.github.sonatadev.sbldb.ui.plan

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.domain.VolumeBand
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.DotRow
import com.github.sonatadev.sbldb.ui.components.InfoLink
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.formatSets
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

@Composable
fun WeeklyPlanScreen(
    onBack: () -> Unit,
    onEditRoutine: (Long) -> Unit,
    onOpenTargets: () -> Unit,
    onOpenGlossary: (String) -> Unit,
    viewModel: WeeklyPlanViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    if (!state.loaded) return
    val inZone = state.rows.count { it.band == VolumeBand.OPTIMAL }
    val below = state.rows.count { it.band == VolumeBand.LOW }
    val above = state.rows.count { it.band == VolumeBand.HIGH }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(label = stringResource(R.string.plan_label), title = stringResource(R.string.weekly_plan), navigation = { BackButton(onBack) })
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Count(stringResource(R.string.in_zone), inZone, highlight = true, modifier = Modifier.weight(1f))
                Count(stringResource(R.string.below_zone), below, modifier = Modifier.weight(1f))
                Count(stringResource(R.string.above_zone), above, modifier = Modifier.weight(1f))
            }
        }
        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_plan_routines)) {
                if (state.routines.isEmpty()) {
                    Text(stringResource(R.string.plan_no_routines), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                }
                state.routines.forEachIndexed { i, r ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            r.routine.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                            modifier = Modifier.weight(1f).clickable { onEditRoutine(r.routine.routineId) }.padding(vertical = 8.dp)
                        )
                        Glyph("−", enabled = r.routine.timesPerWeek > 1) { viewModel.setTimesPerWeek(r.routine, r.routine.timesPerWeek - 1) }
                        Text("×${r.routine.timesPerWeek}", style = SbldbType.monoLarge, color = colors.accent, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                        Glyph("+", enabled = r.routine.timesPerWeek < 7) { viewModel.setTimesPerWeek(r.routine, r.routine.timesPerWeek + 1) }
                    }
                }
            }
        }
        item {
            Module(
                Modifier.fillMaxWidth(),
                label = stringResource(R.string.module_plan_muscles),
                trailing = {
                    Text(
                        stringResource(R.string.targets).uppercase() + "  ›",
                        style = SbldbType.mono,
                        color = colors.accent,
                        modifier = Modifier.clickable(onClick = onOpenTargets).padding(vertical = 4.dp)
                    )
                }
            ) {
                state.rows.forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(row.group, style = MaterialTheme.typography.bodyLarge, color = colors.ink, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            MonoCaption(row.target.label, color = if (row.band == VolumeBand.OPTIMAL) colors.muted else colors.accent)
                        }
                        DotRow(row.sets, target = row.target)
                        Text(
                            formatSets(row.sets),
                            style = SbldbType.mono,
                            color = if (row.band == VolumeBand.OPTIMAL) colors.accent else colors.ink,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonoCaption(stringResource(R.string.plan_hint), Modifier.weight(1f))
                    InfoLink(stringResource(R.string.zone_legend), onClick = { onOpenGlossary("Volume") })
                }
            }
        }
    }
}

@Composable
private fun Count(label: String, value: Int, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val colors = SbldbTheme.colors
    Module(modifier, label = label) {
        Text("$value", style = SbldbType.hero(40), color = if (highlight) colors.accent else colors.ink)
    }
}

@Composable
private fun Glyph(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = SbldbTheme.colors
    Box(
        Modifier
            .size(36.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(glyph, style = SbldbType.monoLarge, color = if (enabled) colors.ink else colors.empty) }
}
