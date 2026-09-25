package com.github.sonatadev.sbldb.ui.onboarding

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.github.sonatadev.sbldb.ui.components.PrimaryButton
import com.github.sonatadev.sbldb.ui.components.SecondaryButton
import com.github.sonatadev.sbldb.ui.components.SegmentedControl
import com.github.sonatadev.sbldb.ui.settings.SettingsViewModel
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import com.github.sonatadev.sbldb.ui.theme.SbldbType
import com.github.sonatadev.sbldb.ui.theme.color

private const val PAGES = 4

/** First launch: what the app is, explanation level, units and look, optional backup folder. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SbldbTheme.colors
    val context = LocalContext.current
    var page by rememberSaveable { mutableIntStateOf(0) }
    val levelLabels = mapOf(
        ExplanationLevel.BASIC to stringResource(R.string.level_beginner),
        ExplanationLevel.EXPERT to stringResource(R.string.level_expert)
    )
    val unitLabels = mapOf(WeightUnit.KG to stringResource(R.string.unit_kg), WeightUnit.LB to stringResource(R.string.unit_lb))
    val themeLabels = mapOf(
        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.DARK to stringResource(R.string.theme_dark)
    )
    BackHandler(enabled = page > 0) { page-- }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setBackupFolder(uri)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.ground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(PAGES) { i ->
                    Box(
                        Modifier
                            .size(if (i == page) 10.dp else 7.dp)
                            .background(if (i <= page) colors.accent else colors.empty, CircleShape)
                    )
                }
            }
            if (page < PAGES - 1) {
                Text(
                    stringResource(R.string.onboarding_skip).uppercase(),
                    style = SbldbType.mono,
                    color = colors.muted,
                    modifier = Modifier.clickable(onClick = onDone).padding(8.dp)
                )
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (page) {
                0 -> {
                    Text("sbl.db", style = SbldbType.hero(72), color = colors.accent)
                    Text(stringResource(R.string.onboarding_welcome), style = MaterialTheme.typography.headlineMedium, color = colors.ink)
                    Point("01", stringResource(R.string.onboarding_point_actions))
                    Point("02", stringResource(R.string.onboarding_point_volume))
                    Point("03", stringResource(R.string.onboarding_point_free))
                }
                1 -> {
                    Title(stringResource(R.string.onboarding_level_title))
                    SegmentedControl(
                        options = ExplanationLevel.entries,
                        selected = state.explanations,
                        label = { levelLabels.getValue(it) },
                        onSelect = viewModel::setExplanations
                    )
                    Module(Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(
                                if (state.explanations == ExplanationLevel.BASIC) R.string.onboarding_level_basic
                                else R.string.onboarding_level_expert
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.ink
                        )
                    }
                    MonoCaption(stringResource(R.string.onboarding_change_later))
                }
                2 -> {
                    Title(stringResource(R.string.onboarding_look_title))
                    ModuleLabel(stringResource(R.string.settings_units_short), color = colors.muted)
                    SegmentedControl(
                        options = WeightUnit.entries,
                        selected = state.weightUnit,
                        label = { unitLabels.getValue(it) },
                        onSelect = viewModel::setWeightUnit
                    )
                    ModuleLabel(stringResource(R.string.theme), color = colors.muted)
                    SegmentedControl(
                        options = ThemeMode.entries,
                        selected = state.themeMode,
                        label = { themeLabels.getValue(it) },
                        onSelect = viewModel::setThemeMode
                    )
                    ModuleLabel(stringResource(R.string.accent_color), color = colors.muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                else -> {
                    Title(stringResource(R.string.onboarding_backup_title))
                    Text(stringResource(R.string.onboarding_backup_text), style = MaterialTheme.typography.bodyLarge, color = colors.muted)
                    SecondaryButton(
                        stringResource(if (state.backupFolder == null) R.string.choose_folder else R.string.change_folder),
                        onClick = { pickFolder.launch(null) },
                        color = colors.accent,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.backupFolder != null) MonoCaption(stringResource(R.string.onboarding_backup_on), color = colors.accent)
                    MonoCaption(stringResource(R.string.onboarding_backup_later))
                }
            }
        }

        PrimaryButton(
            text = stringResource(if (page < PAGES - 1) R.string.onboarding_next else R.string.onboarding_start),
            onClick = { if (page < PAGES - 1) page++ else onDone() },
            accentDot = page == PAGES - 1,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, color = SbldbTheme.colors.ink)
}

@Composable
private fun Point(number: String, text: String) {
    val colors = SbldbTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Text(number, style = SbldbType.mono, color = colors.accent, modifier = Modifier.width(34.dp).padding(top = 3.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
    }
}
