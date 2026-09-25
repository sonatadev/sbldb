package com.github.sonatadev.sbldb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.content.ContentUpdater
import com.github.sonatadev.sbldb.data.repository.ContentStatus
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.domain.AccentColor
import com.github.sonatadev.sbldb.domain.ExplanationLevel
import com.github.sonatadev.sbldb.domain.ThemeMode
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.ORANGE,
    val explanations: ExplanationLevel = ExplanationLevel.BASIC,
    val content: ContentStatus = ContentStatus(null, null, null),
    val checking: Boolean = false
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val contentUpdater: ContentUpdater
) : ViewModel() {
    private val checking = MutableStateFlow(false)

    private val preferences = combine(settings.weightUnit, settings.themeMode, settings.accentColor, settings.explanationLevel) { unit, mode, accent, level ->
        SettingsUiState(unit, mode, accent, level)
    }

    val uiState: StateFlow<SettingsUiState> =
        combine(preferences, settings.contentStatus, checking) { prefs, content, busy -> prefs.copy(content = content, checking = busy) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun checkForUpdates() {
        if (checking.value) return
        viewModelScope.launch {
            checking.value = true
            try { contentUpdater.refresh() } finally { checking.value = false }
        }
    }

    fun setWeightUnit(unit: WeightUnit) = launch { settings.setWeightUnit(unit) }

    fun setThemeMode(mode: ThemeMode) = launch { settings.setThemeMode(mode) }

    fun setAccent(accent: AccentColor) = launch { settings.setAccentColor(accent) }

    fun setExplanations(level: ExplanationLevel) = launch { settings.setExplanationLevel(level) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
