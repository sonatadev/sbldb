package com.github.sonatadev.sbldb

import com.github.sonatadev.sbldb.R
import android.app.Application
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.backup.BackupManager
import com.github.sonatadev.sbldb.data.content.ContentUpdater
import com.github.sonatadev.sbldb.session.WorkoutSession
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.data.repository.JointActionRepository
import com.github.sonatadev.sbldb.data.repository.BodyRepository
import com.github.sonatadev.sbldb.data.repository.RoutineRepository
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.data.repository.WorkoutRepository
import com.github.sonatadev.sbldb.reminder.Reminders
import com.github.sonatadev.sbldb.widget.SbldbWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Manual dependency container, shared by all ViewModels. */
class AppContainer(application: Application) {
    val database = AppDatabase.getDatabase(application)
    val exerciseRepository = ExerciseRepository(database)
    val workoutRepository = WorkoutRepository(database) { part ->
        application.getString(
            when (part) {
                WorkoutRepository.PartOfDay.MORNING -> R.string.workout_morning
                WorkoutRepository.PartOfDay.AFTERNOON -> R.string.workout_afternoon
                WorkoutRepository.PartOfDay.EVENING -> R.string.workout_evening
            }
        )
    }
    val settingsRepository = SettingsRepository(application)
    val jointActionRepository = JointActionRepository(database)
    val routineRepository = RoutineRepository(database)
    val bodyRepository = BodyRepository(database)
    val contentUpdater = ContentUpdater(application, database, settingsRepository)
    val backupManager = BackupManager(application, database, settingsRepository)
    val workoutSession = WorkoutSession()

    /** Whether to show onboarding; null while it is being decided. */
    val needsOnboarding = kotlinx.coroutines.flow.MutableStateFlow<Boolean?>(null)

    /** People upgrading with workouts already logged skip onboarding. */
    suspend fun decideOnboarding() {
        val done = settingsRepository.onboarded() ?: (database.workoutDAO().countWorkouts() > 0).also { existing ->
            if (existing) settingsRepository.setOnboarded()
        }
        needsOnboarding.value = !done
    }

    suspend fun finishOnboarding() {
        settingsRepository.setOnboarded()
        needsOnboarding.value = false
    }
}

@OptIn(FlowPreview::class)
class SbldbApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { container.decideOnboarding() }
        appScope.launch {
            // The launcher icon follows the theme and accent picked in Settings
            combine(container.settingsRepository.themeMode, container.settingsRepository.accentColor) { theme, accent -> theme to accent }
                .distinctUntilChanged()
                .collect { (theme, accent) -> runCatching { AppIcon.apply(this@SbldbApplication, theme, accent) } }
        }
        appScope.launch {
            // The reminder alarm follows its setting (and is set again at every app start)
            container.settingsRepository.reminder.collect { runCatching { Reminders.schedule(this@SbldbApplication, it) } }
        }
        appScope.launch {
            // The widget redraws whenever what it shows changes
            combine(
                container.workoutRepository.history,
                container.workoutRepository.activeWorkout,
                container.routineRepository.routines,
                container.settingsRepository.themeMode,
                container.settingsRepository.accentColor
            ) { _, active, _, _, _ -> active?.workoutId }
                .debounce(500)
                .collect { SbldbWidget.refresh(this@SbldbApplication) }
        }
        appScope.launch {
            // Start from local content right away, then look for newer content on GitHub
            container.contentUpdater.loadLocal()
            container.contentUpdater.refresh()
        }
    }
}
