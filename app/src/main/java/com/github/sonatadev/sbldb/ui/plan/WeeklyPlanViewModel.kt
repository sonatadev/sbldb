package com.github.sonatadev.sbldb.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.entity.Routine
import com.github.sonatadev.sbldb.data.entity.RoutineWithExercises
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.data.repository.RoutineRepository
import com.github.sonatadev.sbldb.domain.VolumeBand
import com.github.sonatadev.sbldb.domain.VolumeTarget
import com.github.sonatadev.sbldb.ui.routines.planFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanRow(val group: String, val sets: Double, val target: VolumeTarget) {
    val band: VolumeBand get() = target.band(sets)
}

data class WeeklyPlanUiState(
    val loaded: Boolean = false,
    val routines: List<RoutineWithExercises> = emptyList(),
    /** Every muscle group; out-of-zone ones first so gaps stand out. */
    val rows: List<PlanRow> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyPlanViewModel(
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository
) : ViewModel() {
    val uiState: StateFlow<WeeklyPlanUiState> =
        combine(routines.routines, exercises.muscleGroups, exercises.volumeTargets) { r, g, t -> Triple(r, g, t) }
            .mapLatest { (routineList, groups, targets) ->
                val planned = planFor(routineList, exercises).toMap()
                val rows = (groups + planned.keys).distinct()
                    .map { PlanRow(it, planned[it] ?: 0.0, targets[it] ?: VolumeTarget.DEFAULT) }
                    .sortedWith(compareBy<PlanRow> { it.band == VolumeBand.OPTIMAL }.thenByDescending { it.sets }.thenBy { it.group })
                WeeklyPlanUiState(loaded = true, routines = routineList, rows = rows)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyPlanUiState())

    fun setTimesPerWeek(routine: Routine, times: Int) {
        viewModelScope.launch { routines.setTimesPerWeek(routine, times) }
    }
}
