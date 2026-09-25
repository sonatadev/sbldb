package com.github.sonatadev.sbldb.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.data.repository.WorkoutRepository
import com.github.sonatadev.sbldb.domain.MuscleGroupVolume
import com.github.sonatadev.sbldb.domain.VolumeCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class MonthStats(
    val sessions: Int = 0,
    val hardSets: Int = 0,
    /** Most trained groups this month; sets are weekly averages so they compare with the 10–20 zone. */
    val topGroups: List<MuscleGroupVolume> = emptyList(),
    /** Session name → count, most frequent first. */
    val byRoutine: List<Pair<String, Int>> = emptyList()
)

data class LogUiState(
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    val workoutsByDay: Map<LocalDate, List<WorkoutWithExercises>> = emptyMap(),
    val stats: MonthStats = MonthStats()
) {
    val selectedWorkouts: List<WorkoutWithExercises> get() = workoutsByDay[selected].orEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModel(
    workouts: WorkoutRepository,
    exercises: ExerciseRepository
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())

    private val monthData = month.flatMapLatest { m ->
        val from = m.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = m.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        combine(workouts.finishedBetween(from, to), workouts.volumeRows(from, to), exercises.muscleGroups) { list, rows, groups ->
            val byDay = list.groupBy { Instant.ofEpochMilli(it.workout.startedAt).atZone(zone).toLocalDate() }
            // Weeks elapsed so far in the current month, so early-month averages are not diluted
            val days = if (m == YearMonth.now()) LocalDate.now().dayOfMonth else m.lengthOfMonth()
            val weeks = (days / 7.0).coerceAtLeast(1.0)
            val stats = MonthStats(
                sessions = list.size,
                hardSets = rows.filter { VolumeCalculator.isHardSet(it.rir) }.map { it.setId }.distinct().size,
                topGroups = VolumeCalculator.calculate(rows, groups, zone)
                    .filter { it.sets > 0 }
                    .take(3)
                    .map { it.copy(sets = it.sets / weeks) },
                byRoutine = list.groupingBy { it.workout.name }.eachCount().toList().sortedByDescending { it.second }
            )
            m to (byDay to stats)
        }
    }

    val uiState: StateFlow<LogUiState> = combine(monthData, selected) { (m, data), day ->
        LogUiState(month = m, selected = day, workoutsByDay = data.first, stats = data.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogUiState())

    fun select(day: LocalDate) {
        selected.value = day
    }

    fun changeMonth(offset: Long) {
        val next = month.value.plusMonths(offset)
        month.value = next
        selected.value = if (next == YearMonth.now()) LocalDate.now() else next.atDay(1)
    }
}
