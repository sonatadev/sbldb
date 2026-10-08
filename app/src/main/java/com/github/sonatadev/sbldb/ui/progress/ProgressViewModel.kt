package com.github.sonatadev.sbldb.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.domain.ExerciseProgress
import com.github.sonatadev.sbldb.domain.ExerciseRef
import com.github.sonatadev.sbldb.domain.LoadMetric
import com.github.sonatadev.sbldb.domain.LoadProgress
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import java.time.ZonedDateTime

/** How far back the overview looks. */
enum class ProgressPeriod(val months: Long) { MONTH(1), QUARTER(3), YEAR(12) }

data class ProgressUiState(
    val period: ProgressPeriod = ProgressPeriod.QUARTER,
    val metric: LoadMetric = LoadMetric.E1RM,
    val exercises: List<ExerciseProgress> = emptyList(),
    val unit: WeightUnit = WeightUnit.KG,
    val loaded: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(exercises: ExerciseRepository, settings: SettingsRepository) : ViewModel() {
    private val period = MutableStateFlow(ProgressPeriod.QUARTER)
    private val metric = MutableStateFlow(LoadMetric.E1RM)

    val uiState: StateFlow<ProgressUiState> = combine(period, metric) { p, m -> p to m }
        .flatMapLatest { (p, m) ->
            val from = ZonedDateTime.now(ZoneId.systemDefault()).minusMonths(p.months).toInstant().toEpochMilli()
            exercises.allSetsSince(from).map { rows ->
                ProgressUiState(p, m, LoadProgress.of(rows.map { ExerciseRef(it.exerciseId, it.name, it.isTimed) to it.set }, m), loaded = true)
            }
        }
        .combine(settings.weightUnit) { state, unit -> state.copy(unit = unit) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun setPeriod(value: ProgressPeriod) {
        period.value = value
    }

    fun setMetric(value: LoadMetric) {
        metric.value = value
    }
}
