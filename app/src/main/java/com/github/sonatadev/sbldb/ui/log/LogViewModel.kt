package com.github.sonatadev.sbldb.ui.log

import kotlinx.coroutines.launch
import com.github.sonatadev.sbldb.data.repository.RoutineRepository
import com.github.sonatadev.sbldb.data.entity.RoutineWithExercises
import com.github.sonatadev.sbldb.data.entity.PlannedRoutine
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
    val plannedByDay: Map<LocalDate, List<PlannedRoutine>> = emptyMap(),
    val stats: MonthStats = MonthStats(),
    /** Routines that can be planned (the ones with exercises). */
    val routines: List<RoutineWithExercises> = emptyList(),
    val hasActiveWorkout: Boolean = false
) {
    val selectedWorkouts: List<WorkoutWithExercises> get() = workoutsByDay[selected].orEmpty()
    val selectedPlanned: List<PlannedRoutine> get() = plannedByDay[selected].orEmpty()
    val canPlanSelected: Boolean get() = !selected.isBefore(LocalDate.now())
}

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModel(
    private val workouts: WorkoutRepository,
    exercises: ExerciseRepository,
    private val routines: RoutineRepository
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())

    private val monthData = month.flatMapLatest { m ->
        val from = m.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = m.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        combine(
            workouts.finishedBetween(from, to),
            workouts.volumeRows(from, to),
            exercises.muscleGroups,
            exercises.volumeTargets,
            routines.plannedBetween(m.atDay(1), m.atEndOfMonth())
        ) { list, rows, groups, targets, planned ->
            val byDay = list.groupBy { Instant.ofEpochMilli(it.workout.startedAt).atZone(zone).toLocalDate() }
            // Weeks elapsed so far in the current month, so early-month averages are not diluted
            val days = if (m == YearMonth.now()) LocalDate.now().dayOfMonth else m.lengthOfMonth()
            val weeks = (days / 7.0).coerceAtLeast(1.0)
            val stats = MonthStats(
                sessions = list.size,
                hardSets = rows.filter { VolumeCalculator.isHardSet(it.rir) }.map { it.setId }.distinct().size,
                topGroups = VolumeCalculator.calculate(rows, groups, zone, targets)
                    .filter { it.sets > 0 }
                    .take(3)
                    .map { it.copy(sets = it.sets / weeks) },
                byRoutine = list.groupingBy { it.workout.name }.eachCount().toList().sortedByDescending { it.second }
            )
            MonthData(m, byDay, planned.groupBy { LocalDate.ofEpochDay(it.date) }, stats)
        }
    }

    val uiState: StateFlow<LogUiState> = combine(monthData, selected, routines.routines, workouts.activeWorkout) { data, day, routineList, active ->
        LogUiState(
            month = data.month,
            selected = day,
            workoutsByDay = data.workouts,
            plannedByDay = data.planned,
            stats = data.stats,
            routines = routineList.filter { it.exercises.isNotEmpty() },
            hasActiveWorkout = active != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogUiState())

    fun select(day: LocalDate) {
        selected.value = day
    }

    fun plan(routineId: Long) {
        val day = selected.value
        viewModelScope.launch { routines.plan(day, routineId) }
    }

    fun unplan(planned: PlannedRoutine) {
        viewModelScope.launch { routines.unplan(planned.plannedId) }
    }

    fun start(planned: PlannedRoutine, onReady: () -> Unit) {
        viewModelScope.launch {
            workouts.startFromRoutine(planned.routineId)
            onReady()
        }
    }

    fun changeMonth(offset: Long) {
        // Looking back is unlimited; planning ahead goes up to a year
        val next = month.value.plusMonths(offset).coerceAtMost(YearMonth.now().plusMonths(MAX_MONTHS_AHEAD))
        month.value = next
        selected.value = if (next == YearMonth.now()) LocalDate.now() else next.atDay(1)
    }

    private data class MonthData(
        val month: YearMonth,
        val workouts: Map<LocalDate, List<WorkoutWithExercises>>,
        val planned: Map<LocalDate, List<PlannedRoutine>>,
        val stats: MonthStats
    )

    companion object {
        const val MAX_MONTHS_AHEAD = 12L
    }
}
