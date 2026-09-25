package com.github.sonatadev.sbldb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.domain.AccentColor
import com.github.sonatadev.sbldb.domain.ExplanationLevel
import com.github.sonatadev.sbldb.domain.ThemeMode
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.ORANGE,
    val explanations: ExplanationLevel = ExplanationLevel.BASIC
)

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        combine(settings.weightUnit, settings.themeMode, settings.accentColor, settings.explanationLevel, ::SettingsUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setWeightUnit(unit: WeightUnit) = launch { settings.setWeightUnit(unit) }

    fun setThemeMode(mode: ThemeMode) = launch { settings.setThemeMode(mode) }

    fun setAccent(accent: AccentColor) = launch { settings.setAccentColor(accent) }

    fun setExplanations(level: ExplanationLevel) = launch { settings.setExplanationLevel(level) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
