package com.github.sonatadev.sbldb

import android.app.Application
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.backup.BackupManager
import com.github.sonatadev.sbldb.data.content.ContentUpdater
import com.github.sonatadev.sbldb.data.repository.ExerciseRepository
import com.github.sonatadev.sbldb.data.repository.JointActionRepository
import com.github.sonatadev.sbldb.data.repository.RoutineRepository
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.data.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual dependency container, shared by all ViewModels. */
class AppContainer(application: Application) {
    val database = AppDatabase.getDatabase(application)
    val exerciseRepository = ExerciseRepository(database)
    val workoutRepository = WorkoutRepository(database)
    val settingsRepository = SettingsRepository(application)
    val jointActionRepository = JointActionRepository(database)
    val routineRepository = RoutineRepository(database)
    val contentUpdater = ContentUpdater(application, database, settingsRepository)
    val backupManager = BackupManager(application, database, settingsRepository)
}

class SbldbApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            // Start from local content right away, then look for newer content on GitHub
            container.contentUpdater.loadLocal()
            container.contentUpdater.refresh()
        }
    }
}
