package com.github.sonatadev.sbldb.ui.volume

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.data.repository.WorkoutRepository
import com.github.sonatadev.sbldb.domain.MuscleGroupVolume
import com.github.sonatadev.sbldb.domain.VolumeCalculator
import com.github.sonatadev.sbldb.domain.VolumeHistory
import com.github.sonatadev.sbldb.domain.WeekRange
import com.github.sonatadev.sbldb.domain.WeeklyVolume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class VolumeUiState(
    val weeksAgo: Int = 0,
    val week: WeekRange = WeekRange.of(0),
    val groups: List<MuscleGroupVolume> = emptyList(),
    /** The selected week and the ones before it, for the charts. */
    val history: WeeklyVolume = WeeklyVolume()
)

@OptIn(ExperimentalCoroutinesApi::class)
class VolumeViewModel(
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository
) : ViewModel() {
    private val weeksAgo = MutableStateFlow(0)

    val uiState: StateFlow<VolumeUiState> = weeksAgo
        .flatMapLatest { ago ->
            val weeks = VolumeHistory.weeksEndingAt(ago)
            val week = weeks.last()
            combine(
                workoutRepository.volumeRows(weeks.first().startMillis, week.endMillis),
                exerciseRepository.muscleGroups,
                exerciseRepository.volumeTargets
            ) { rows, groups, targets ->
                val thisWeek = rows.filter { it.startedAt >= week.startMillis }
                VolumeUiState(ago, week, VolumeCalculator.calculate(thisWeek, groups, targets = targets), VolumeHistory.of(rows, weeks))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VolumeUiState())

    fun previousWeek() {
        weeksAgo.value += 1
    }

    /** Jumps to a week of the chart: [index] counts from the oldest one shown. */
    fun selectWeek(index: Int) {
        weeksAgo.value = (weeksAgo.value + VolumeHistory.WEEKS - 1 - index).coerceAtLeast(0)
    }

    fun nextWeek() {
        if (weeksAgo.value > 0) weeksAgo.value -= 1
    }
}
