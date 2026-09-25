package com.github.sonatadev.sbldb.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.domain.VolumeTarget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VolumeTargetsViewModel(private val exercises: ExerciseRepository) : ViewModel() {
    /** Every muscle group with its current target, alphabetical. */
    val targets: StateFlow<List<Pair<String, VolumeTarget>>> =
        combine(exercises.muscleGroups, exercises.volumeTargets) { groups, targets ->
            groups.map { it to (targets[it] ?: VolumeTarget.DEFAULT) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun set(group: String, minSets: Int, maxSets: Int) {
        viewModelScope.launch { exercises.setVolumeTarget(group, VolumeTarget.of(minSets, maxSets)) }
    }

    fun resetAll() {
        viewModelScope.launch { exercises.resetVolumeTargets() }
    }
}
