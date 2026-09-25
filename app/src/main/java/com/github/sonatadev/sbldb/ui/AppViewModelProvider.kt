package com.github.sonatadev.sbldb.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.sonatadev.sbldb.AppContainer
import com.github.sonatadev.sbldb.SbldbApplication
import com.github.sonatadev.sbldb.ui.exercises.ExerciseDetailViewModel
import com.github.sonatadev.sbldb.ui.exercises.ExerciseListViewModel
import com.github.sonatadev.sbldb.ui.settings.SettingsViewModel
import com.github.sonatadev.sbldb.ui.volume.VolumeViewModel
import com.github.sonatadev.sbldb.ui.workout.ActiveWorkoutViewModel
import com.github.sonatadev.sbldb.ui.workout.WorkoutDetailViewModel
import com.github.sonatadev.sbldb.ui.exercises.CustomExerciseViewModel
import com.github.sonatadev.sbldb.ui.plan.WeeklyPlanViewModel
import com.github.sonatadev.sbldb.ui.plan.VolumeTargetsViewModel
import com.github.sonatadev.sbldb.ui.actions.ActionDetailViewModel
import com.github.sonatadev.sbldb.ui.actions.ActionsViewModel
import com.github.sonatadev.sbldb.ui.log.LogViewModel
import com.github.sonatadev.sbldb.ui.glossary.GlossaryViewModel
import com.github.sonatadev.sbldb.ui.home.HomeViewModel
import com.github.sonatadev.sbldb.ui.muscles.MuscleDetailViewModel
import com.github.sonatadev.sbldb.ui.routines.RoutineEditorViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { WeeklyPlanViewModel(container().routineRepository, container().exerciseRepository) }
        initializer { VolumeTargetsViewModel(container().exerciseRepository) }
        initializer { CustomExerciseViewModel(createSavedStateHandle(), container().database) }
        initializer { HomeViewModel(container().workoutRepository, container().routineRepository, container().exerciseRepository) }
        initializer { RoutineEditorViewModel(createSavedStateHandle(), container().routineRepository, container().exerciseRepository) }
        initializer { ActionsViewModel(container().jointActionRepository, container().settingsRepository, container().contentUpdater) }
        initializer { MuscleDetailViewModel(createSavedStateHandle(), container().jointActionRepository) }
        initializer { GlossaryViewModel(createSavedStateHandle(), container().jointActionRepository) }
        initializer { ActionDetailViewModel(createSavedStateHandle(), container().jointActionRepository) }
        initializer { LogViewModel(container().workoutRepository, container().exerciseRepository) }
        initializer {
            ActiveWorkoutViewModel(
                container().workoutRepository,
                container().exerciseRepository,
                container().routineRepository,
                container().settingsRepository,
                container().backupManager,
                container().workoutSession
            )
        }
        initializer {
            WorkoutDetailViewModel(
                createSavedStateHandle(),
                container().workoutRepository,
                container().settingsRepository
            )
        }
        initializer {
            ExerciseListViewModel(
                createSavedStateHandle(),
                container().exerciseRepository,
                container().jointActionRepository,
                container().workoutRepository,
                container().routineRepository
            )
        }
        initializer {
            ExerciseDetailViewModel(
                createSavedStateHandle(),
                container().exerciseRepository,
                container().jointActionRepository,
                container().settingsRepository
            )
        }
        initializer {
            VolumeViewModel(container().workoutRepository, container().exerciseRepository)
        }
        initializer { SettingsViewModel(container().settingsRepository, container().contentUpdater, container().backupManager) }
    }

    private fun CreationExtras.container(): AppContainer =
        (this[APPLICATION_KEY] as SbldbApplication).container
}
