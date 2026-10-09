package com.github.sonatadev.sbldb.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sonatadev.sbldb.SbldbApplication
import com.github.sonatadev.sbldb.ui.importer.ImportScreen
import com.github.sonatadev.sbldb.ui.importer.ImportViewModel
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The import preview on a small Strong export, saved next to the tour's pictures. */
@RunWith(AndroidJUnit4::class)
class ImportShot {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun preview() {
        val app = ApplicationProvider.getApplicationContext<SbldbApplication>()
        runBlocking { app.container.contentUpdater.loadLocal() }
        val csv = File(app.cacheDir, "strong.csv").apply {
            writeText(
                """
                Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes,Workout Notes,RPE
                2026-08-03 18:30:00,Push,1h 5m,Bench Press (Barbell),1,80,8,0,0,,,8
                2026-08-03 18:30:00,Push,1h 5m,Incline Bench Press (Dumbbell),1,26,10,0,0,,,
                2026-08-03 18:30:00,Push,1h 5m,Lateral Raise (Dumbbell),1,10,12,0,0,,,
                2026-08-05 18:30:00,Pull,58m,Lat Pulldown (Cable),1,55,10,0,0,,,
                2026-08-05 18:30:00,Pull,58m,Seated Cable Row - V Grip (Cable),1,60,10,0,0,,,
                2026-08-05 18:30:00,Pull,58m,Tire Flip,1,0,5,0,0,,,
                2026-09-28 18:30:00,Legs,1h,Squat (Barbell),1,100,5,0,0,,,
                2026-09-28 18:30:00,Legs,1h,Plank,1,0,0,0,60,,,
                """.trimIndent()
            )
        }
        val viewModel = ImportViewModel(SavedStateHandle(mapOf("uri" to Uri.fromFile(csv).toString())), app, app.container.database, app.container.settingsRepository)
        compose.setContent { SbldbTheme { ImportScreen(onBack = {}, viewModel = viewModel) } }
        compose.waitUntil(5_000) { !viewModel.uiState.value.loading }
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val out = File(app.getExternalFilesDir(null), "screens").apply { mkdirs() }
        File(out, "21-import.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
