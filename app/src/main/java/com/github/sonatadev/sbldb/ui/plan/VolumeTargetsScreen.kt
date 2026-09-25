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
import com.github.sonatadev.sbldb.domain.VolumeTarget
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.InfoLink
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType

@Composable
fun VolumeTargetsScreen(
    onBack: () -> Unit,
    onOpenGlossary: (String) -> Unit,
    viewModel: VolumeTargetsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val targets by viewModel.targets.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(label = stringResource(R.string.targets_label), title = stringResource(R.string.volume_targets), navigation = { BackButton(onBack) })
        }
        item {
            Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                MonoCaption(stringResource(R.string.targets_hint), Modifier.weight(1f))
                InfoLink(stringResource(R.string.zone_legend), onClick = { onOpenGlossary("Volume") })
            }
        }
        item {
            Module(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f))
                    ModuleLabel(stringResource(R.string.target_min), Modifier.width(112.dp))
                    ModuleLabel(stringResource(R.string.target_max), Modifier.width(112.dp))
                }
                Column {
                    targets.forEachIndexed { i, (group, target) ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                        val custom = target != VolumeTarget.DEFAULT
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(group, style = MaterialTheme.typography.bodyLarge, color = if (custom) colors.accent else colors.ink, modifier = Modifier.weight(1f))
                            MiniStepper(target.minSets, target.minSets > VolumeTarget.LOWEST, target.minSets < target.maxSets,
                                onChange = { viewModel.set(group, it, target.maxSets) })
                            MiniStepper(target.maxSets, target.maxSets > target.minSets, target.maxSets < VolumeTarget.HIGHEST,
                                onChange = { viewModel.set(group, target.minSets, it) })
                        }
                    }
                }
            }
        }
        item {
            SecondaryButton(stringResource(R.string.targets_reset), onClick = viewModel::resetAll, color = colors.muted, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MiniStepper(value: Int, canDown: Boolean, canUp: Boolean, onChange: (Int) -> Unit) {
    val colors = SbldbTheme.colors
    Row(Modifier.width(112.dp), verticalAlignment = Alignment.CenterVertically) {
        StepGlyph("−", canDown) { onChange(value - 1) }
        Text("$value", style = SbldbType.monoLarge, color = colors.ink, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        StepGlyph("+", canUp) { onChange(value + 1) }
    }
}

@Composable
private fun StepGlyph(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = SbldbTheme.colors
    Box(
        Modifier
            .size(38.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(glyph, style = SbldbType.monoLarge, color = if (enabled) colors.ink else colors.empty) }
}
