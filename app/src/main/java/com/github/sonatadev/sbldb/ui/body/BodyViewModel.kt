package com.github.sonatadev.sbldb.ui.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.BodyEntry
import com.github.sonatadev.sbldb.data.repository.BodyRepository
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.domain.BodyStats
import com.github.sonatadev.sbldb.domain.WeightPoint
import com.github.sonatadev.sbldb.domain.WeightRate
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BodyUiState(
    val loaded: Boolean = false,
    val unit: WeightUnit = WeightUnit.KG,
    /** Newest first. */
    val entries: List<BodyEntry> = emptyList(),
    /** 7-day average weight, oldest first, as (epoch millis, kg) for the chart. */
    val trend: List<Pair<Long, Double>> = emptyList(),
    val rate: WeightRate? = null
) {
    val latestWeight: BodyEntry? get() = entries.firstOrNull { it.weightKg != null }

    /** Most recent value of a measurement and how much it changed since the first one. */
    fun measurement(pick: (BodyEntry) -> Double?): Pair<Double, Double?>? {
        val withValue = entries.filter { pick(it) != null }
        val latest = withValue.firstOrNull()?.let(pick) ?: return null
        val first = withValue.lastOrNull()?.let(pick)
        return latest to first?.takeIf { withValue.size > 1 }?.let { latest - it }
    }
}

class BodyViewModel(private val body: BodyRepository, settings: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<BodyUiState> = combine(body.entries, settings.weightUnit) { entries, unit ->
        val points = entries.mapNotNull { e -> e.weightKg?.let { WeightPoint(e.date, it) } }
        BodyUiState(
            loaded = true,
            unit = unit,
            entries = entries,
            trend = BodyStats.rollingAverage(points).map { it.day * MILLIS_PER_DAY to it.kg },
            rate = BodyStats.rate(points)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyUiState())

    /** Values are in the display unit; blank fields are left out. */
    fun save(weight: String, waist: String, chest: String, arm: String, thigh: String) {
        val unit = uiState.value.unit
        fun length(text: String) = text.replace(',', '.').toDoubleOrNull()?.let(unit::toCm)
        val entry = BodyEntry(
            date = LocalDate.now().toEpochDay(),
            weightKg = weight.replace(',', '.').toDoubleOrNull()?.let(unit::toKg),
            waistCm = length(waist),
            chestCm = length(chest),
            armCm = length(arm),
            thighCm = length(thigh)
        )
        if (listOf(entry.weightKg, entry.waistCm, entry.chestCm, entry.armCm, entry.thighCm).all { it == null }) return
        viewModelScope.launch { body.save(entry) }
    }

    fun delete(entry: BodyEntry) {
        viewModelScope.launch { body.delete(entry) }
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
