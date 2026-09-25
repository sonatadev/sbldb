package com.github.sonatadev.sbldb.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.RoutineWithExercises
import com.github.sonatadev.sbldb.data.entity.Workout
import com.github.sonatadev.sbldb.data.entity.WorkoutWithExercises
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.data.repository.RoutineRepository
import com.github.sonatadev.sbldb.data.repository.WorkoutRepository
import com.github.sonatadev.sbldb.domain.MuscleGroupVolume
import com.github.sonatadev.sbldb.domain.VolumeCalculator
import com.github.sonatadev.sbldb.domain.WeekRange
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val week: WeekRange = WeekRange.of(0),
    val activeWorkout: Workout? = null,
    val routines: List<RoutineWithExercises> = emptyList(),
    /** Routine done least recently (never-done routines first). */
    val nextRoutine: RoutineWithExercises? = null,
    val trainedDays: Set<LocalDate> = emptySet(),
    val sessionsThisWeek: Int = 0,
    val hardSetsThisWeek: Int = 0,
    /** Up to three muscles furthest below the 10-set zone this week, among those you train. */
    val lagging: List<MuscleGroupVolume> = emptyList(),
    val lastSession: WorkoutWithExercises? = null
)

class HomeViewModel(
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    exercises: ExerciseRepository
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val week = WeekRange.of(0)
    /** Muscles trained in the last four weeks decide which ones can show up as lagging. */
    private val lookbackStart = WeekRange.of(3).startMillis

    private val weekData = combine(
        workouts.finishedBetween(week.startMillis, week.endMillis),
        workouts.volumeRows(week.startMillis, week.endMillis),
        workouts.volumeRows(lookbackStart, week.endMillis),
        exercises.muscleGroups
    ) { weekWorkouts, weekRows, recentRows, groups ->
        val trainedGroups = recentRows.map { it.muscleGroup }.toSet().ifEmpty { MAJOR_GROUPS }
        val volume = VolumeCalculator.calculate(weekRows, groups.filter { it in trainedGroups }, zone)
        WeekData(
            trainedDays = weekWorkouts.map { Instant.ofEpochMilli(it.workout.startedAt).atZone(zone).toLocalDate() }.toSet(),
            sessions = weekWorkouts.size,
            hardSets = weekRows.filter { VolumeCalculator.isHardSet(it.rir) }.map { it.setId }.distinct().size,
            lagging = volume.filter { it.sets < VolumeCalculator.OPTIMAL_MIN_SETS }.sortedBy { it.sets }.take(3)
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        workouts.activeWorkout,
        routines.routines,
        workouts.history,
        weekData
    ) { active, routineList, history, data ->
        val lastDone = history.mapNotNull { w -> w.workout.routineId?.let { it to w.workout.startedAt } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.max() }
        HomeUiState(
            week = week,
            activeWorkout = active,
            routines = routineList,
            nextRoutine = routineList.filter { it.exercises.isNotEmpty() }
                .minByOrNull { lastDone[it.routine.routineId] ?: Long.MIN_VALUE },
            trainedDays = data.trainedDays,
            sessionsThisWeek = data.sessions,
            hardSetsThisWeek = data.hardSets,
            lagging = data.lagging,
            lastSession = history.firstOrNull()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun startEmpty(onReady: () -> Unit) = launch { workouts.startOrResume(); onReady() }

    fun startRoutine(routineId: Long, onReady: () -> Unit) = launch { workouts.startFromRoutine(routineId); onReady() }

    fun createRoutine(name: String, onCreated: (Long) -> Unit) = launch { onCreated(routines.create(name)) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private data class WeekData(
        val trainedDays: Set<LocalDate>,
        val sessions: Int,
        val hardSets: Int,
        val lagging: List<MuscleGroupVolume>
    )

    private companion object {
        val MAJOR_GROUPS = setOf("Chest", "Shoulders", "Lats", "Traps", "Biceps", "Triceps", "Quadriceps", "Hamstrings", "Glutes", "Calves")
    }
}
