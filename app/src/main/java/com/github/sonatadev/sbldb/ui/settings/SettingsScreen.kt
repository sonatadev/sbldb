package com.github.sonatadev.sbldb.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.domain.AccentColor
import com.github.sonatadev.sbldb.domain.ExplanationLevel
import com.github.sonatadev.sbldb.domain.ThemeMode
import com.github.sonatadev.sbldb.domain.WeightUnit
import com.github.sonatadev.sbldb.ui.AppViewModelProvider
import com.github.sonatadev.sbldb.ui.components.AccentSwatch
import com.github.sonatadev.sbldb.ui.components.Module
import com.github.sonatadev.sbldb.ui.components.ModuleLabel
import com.github.sonatadev.sbldb.ui.components.MonoCaption
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.formatDate
import com.github.sonatadev.sbldb.ui.formatTime
import com.github.sonatadev.sbldb.ui.components.ScreenHeader
import com.github.sonatadev.sbldb.ui.components.SegmentedControl
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.color

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onOpenGlossary: () -> Unit, viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    val themeLabels = mapOf(
        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.DARK to stringResource(R.string.theme_dark)
    )
    val levelLabels = mapOf(
        ExplanationLevel.BASIC to stringResource(R.string.level_beginner),
        ExplanationLevel.EXPERT to stringResource(R.string.level_expert)
    )
    val unitLabels = mapOf(
        WeightUnit.KG to stringResource(R.string.unit_kg),
        WeightUnit.LB to stringResource(R.string.unit_lb)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScreenHeader(label = stringResource(R.string.settings_label), title = stringResource(R.string.tab_settings))

        Module(Modifier.fillMaxWidth(), label = stringResource(R.string.settings_appearance)) {
            ModuleLabel(stringResource(R.string.theme), color = colors.muted)
            SegmentedControl(
                options = ThemeMode.entries,
                selected = state.themeMode,
                label = { themeLabels.getValue(it) },
                onSelect = viewModel::setThemeMode
            )
            ModuleLabel(stringResource(R.string.accent_color), color = colors.muted, modifier = Modifier.padding(top = 6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AccentColor.entries.forEach { accent ->
                    AccentSwatch(
                        color = accent.color(colors.isDark),
                        selected = accent == state.accent,
                        description = accent.label,
                        onClick = { viewModel.setAccent(accent) }
                    )
                }
            }
        }

        Module(Modifier.fillMaxWidth(), label = stringResource(R.string.settings_explanations)) {
            SegmentedControl(
                options = ExplanationLevel.entries,
                selected = state.explanations,
                label = { levelLabels.getValue(it) },
                onSelect = viewModel::setExplanations
            )
            Text(
                stringResource(if (state.explanations == ExplanationLevel.BASIC) R.string.explanations_basic_hint else R.string.explanations_expert_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted
            )
            Text(
                stringResource(R.string.open_glossary) + "  ›",
                style = MaterialTheme.typography.titleMedium,
                color = colors.accent,
                modifier = Modifier.clickable(onClick = onOpenGlossary).padding(vertical = 6.dp)
            )
        }

        Module(Modifier.fillMaxWidth(), label = stringResource(R.string.settings_units)) {
            SegmentedControl(
                options = WeightUnit.entries,
                selected = state.weightUnit,
                label = { unitLabels.getValue(it) },
                onSelect = viewModel::setWeightUnit
            )
        }

        Module(Modifier.fillMaxWidth(), label = stringResource(R.string.settings_library)) {
            val content = state.content
            Text(
                stringResource(
                    if (content.source == "GitHub") R.string.library_source_github else R.string.library_source_app
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink
            )
            content.checkedAt?.let {
                MonoCaption(stringResource(R.string.library_checked, formatDate(it) + " " + formatTime(it)))
            }
            content.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.accent) }
            SecondaryButton(
                stringResource(if (state.checking) R.string.library_checking else R.string.library_check),
                onClick = viewModel::checkForUpdates,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Module(Modifier.fillMaxWidth(), label = stringResource(R.string.settings_about)) {
            Text(stringResource(R.string.about), style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        }
    }
}
