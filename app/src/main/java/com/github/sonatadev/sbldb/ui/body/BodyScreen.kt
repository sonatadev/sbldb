package com.github.sonatadev.sbldb.ui.body

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.data.entity.BodyEntry
import com.github.sonatadev.sbldb.domain.WeightUnit
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.BackButton
import com.github.sonatadev.sbldb.ui.components.CompactNumberField
import com.github.sonatadev.sbldb.ui.components.InfoLink
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.PrimaryButton
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.TrendChart
import com.github.sonatadev.sbldb.ui.formatShortDate
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun BodyScreen(
    onBack: () -> Unit,
    onOpenGlossary: (String) -> Unit,
    viewModel: BodyViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    val focus = LocalFocusManager.current
    if (!state.loaded) return
    val unit = state.unit
    var weight by rememberSaveable { mutableStateOf("") }
    var waist by rememberSaveable { mutableStateOf("") }
    var chest by rememberSaveable { mutableStateOf("") }
    var arm by rememberSaveable { mutableStateOf("") }
    var thigh by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(label = stringResource(R.string.body_label), title = stringResource(R.string.body), navigation = { BackButton(onBack) })
        }
        item {
            Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_body_today)) {
                Field(stringResource(R.string.body_weight), unit.label, weight) { weight = it }
                MonoCaption(stringResource(R.string.body_measurements_optional), Modifier.padding(top = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(stringResource(R.string.body_waist), unit.lengthLabel, waist, Modifier.weight(1f)) { waist = it }
                    Field(stringResource(R.string.body_chest), unit.lengthLabel, chest, Modifier.weight(1f)) { chest = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(stringResource(R.string.body_arm), unit.lengthLabel, arm, Modifier.weight(1f)) { arm = it }
                    Field(stringResource(R.string.body_thigh), unit.lengthLabel, thigh, Modifier.weight(1f)) { thigh = it }
                }
                PrimaryButton(
                    stringResource(R.string.save),
                    onClick = {
                        viewModel.save(weight, waist, chest, arm, thigh)
                        weight = ""; waist = ""; chest = ""; arm = ""; thigh = ""
                        focus.clearFocus()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
        }
        state.latestWeight?.let { latest ->
            item {
                Module(
                    Modifier.fillMaxWidth(),
                    label = stringResource(R.string.module_body_weight),
                    trailing = { InfoLink(stringResource(R.string.body_rate), onClick = { onOpenGlossary("Weight trend") }) }
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(unit.format(latest.weightKg!!), style = SbldbType.hero(52), color = colors.accent)
                        Text(" " + unit.label, style = SbldbType.monoLarge, color = colors.muted, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    state.rate?.let { rate ->
                        val sign = if (rate.kgPerWeek >= 0) "+" else "−"
                        Text(
                            stringResource(
                                R.string.body_rate_value,
                                sign + unit.format(abs(rate.kgPerWeek)) + " " + unit.label,
                                sign + "%.2f".format(abs(rate.percentPerWeek))
                            ),
                            style = SbldbType.mono,
                            color = colors.ink
                        )
                    } ?: MonoCaption(stringResource(R.string.body_rate_need_more))
                    if (state.trend.size >= 2) {
                        TrendChart(
                            state.trend,
                            Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp),
                            format = unit::format,
                            unitLabel = unit.label
                        )
                        MonoCaption(stringResource(R.string.body_trend_hint))
                    }
                }
            }
        }
        val measurements = listOf(
            R.string.body_waist to state.measurement { it.waistCm },
            R.string.body_chest to state.measurement { it.chestCm },
            R.string.body_arm to state.measurement { it.armCm },
            R.string.body_thigh to state.measurement { it.thighCm }
        ).filter { it.second != null }
        if (measurements.isNotEmpty()) {
            item {
                Module(Modifier.fillMaxWidth(), label = stringResource(R.string.module_body_measurements)) {
                    measurements.forEach { (label, value) ->
                        val (latest, change) = value!!
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, color = colors.ink, modifier = Modifier.weight(1f))
                            change?.let {
                                val sign = if (it >= 0) "+" else "−"
                                MonoCaption("$sign${unit.formatLength(abs(it))}  ", color = colors.muted)
                            }
                            Text("${unit.formatLength(latest)} ${unit.lengthLabel}", style = SbldbType.monoLarge, color = colors.accent)
                        }
                    }
                    MonoCaption(stringResource(R.string.body_change_hint))
                }
            }
        }
        if (state.entries.isNotEmpty()) {
            item { ModuleLabel(stringResource(R.string.module_body_entries), color = colors.muted, modifier = Modifier.padding(start = 6.dp, top = 6.dp)) }
            items(state.entries.take(30), key = { it.id }) { entry -> EntryRow(entry, unit, onDelete = { viewModel.delete(entry) }) }
        }
    }
}

@Composable
private fun Field(label: String, unitLabel: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    Column(modifier) {
        ModuleLabel("$label ($unitLabel)", color = SbldbTheme.colors.muted)
        CompactNumberField(value = value, onValueChange = onChange, decimal = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun EntryRow(entry: BodyEntry, unit: WeightUnit, onDelete: () -> Unit) {
    val colors = SbldbTheme.colors
    val parts = buildList {
        entry.weightKg?.let { add("${unit.format(it)} ${unit.label}") }
        entry.waistCm?.let { add("W ${unit.formatLength(it)}") }
        entry.chestCm?.let { add("C ${unit.formatLength(it)}") }
        entry.armCm?.let { add("A ${unit.formatLength(it)}") }
        entry.thighCm?.let { add("T ${unit.formatLength(it)}") }
    }
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            MonoCaption(formatShortDate(LocalDate.ofEpochDay(entry.date)), Modifier.padding(end = 12.dp))
            Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.delete), onClick = onDelete),
                contentAlignment = Alignment.Center
            ) { Text("×", style = SbldbType.monoLarge, color = colors.dim) }
        }
    }
}
