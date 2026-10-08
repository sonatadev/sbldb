package com.github.sonatadev.sbldb.ui

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sonatadev.sbldb.MainActivity
import com.github.sonatadev.sbldb.SbldbApplication
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.entity.Routine
import com.github.sonatadev.sbldb.data.entity.RoutineExercise
import com.github.sonatadev.sbldb.data.entity.Workout
import com.github.sonatadev.sbldb.data.entity.WorkoutExercise
import com.github.sonatadev.sbldb.data.entity.WorkoutSet
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Not a check: opens the main screens on example data and saves a picture of each, so the UI can be
 * looked at without a phone. CI pulls them from the device (files/screens) and keeps them as an artifact.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTour {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val out = File(context.getExternalFilesDir(null), "screens").apply { mkdirs() }

    @Test
    fun tour() {
        seed()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            shot("01-home")
            scrollTo("Start empty workout", ignoreCase = true)
            shot("02-home-bottom")

            tab("Log")
            shot("03-log")
            open("Sets per muscle, week by week")
            shot("04-volume")
            scrollTo("By muscle", ignoreCase = true)
            shot("05-volume-muscles")
            back()

            open("How the load of every exercise is moving")
            shot("06-progress")
            back()

            open("Body weight, rate of change and measurements")
            shot("07-body")
            back()

            tab("Actions")
            shot("08-actions")

            tab("Settings")
            shot("09-settings")

            tab("Home")
            compose.onAllNodesWithText("Start Upper A").onFirst().performClick()
            compose.waitForIdle()
            shot("10-workout")
            compose.onAllNodesWithText("Wide bar", substring = true, ignoreCase = true).onFirst().performClick()
            compose.waitForIdle()
            shot("11-attachment-picker")
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(400)
        // The whole screen, dialogs included (they live in a window of their own)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tab(label: String) {
        compose.onAllNodesWithText(label.uppercase()).onFirst().performClick()
        compose.waitForIdle()
    }

    private fun scrollTo(text: String, ignoreCase: Boolean = false) {
        runCatching {
            compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(text, substring = true, ignoreCase = ignoreCase))
        }
        compose.waitForIdle()
    }

    private fun open(text: String) {
        scrollTo(text)
        compose.onAllNodesWithText(text).onFirst().performClick()
        compose.waitForIdle()
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {}
        androidx.test.espresso.Espresso.pressBack()
        compose.waitForIdle()
    }

    /** Eight weeks of an upper/lower split with steady progress, finished workouts only. */
    private fun seed() = runBlocking {
        // No permission dialog over the screens
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val app = ApplicationProvider.getApplicationContext<SbldbApplication>()
        val container = app.container
        container.contentUpdater.loadLocal()
        container.settingsRepository.setOnboarded()
        container.needsOnboarding.value = false
        val db: AppDatabase = container.database
        if (db.workoutDAO().countWorkouts() > 0) return@runBlocking

        suspend fun id(name: String) = requireNotNull(db.exerciseDAO().findId(name)) { "No exercise $name" }
        val upper = listOf("Lat Pulldown" to 55.0, "Barbell Bench Press" to 70.0, "Seated Cable Row" to 60.0, "Incline Dumbbell Press" to 26.0, "Cable Lateral Raise" to 8.0, "Triceps Pushdown" to 25.0)
        val lower = listOf("Barbell Back Squat" to 90.0, "Romanian Deadlift" to 80.0, "Leg Extension" to 50.0, "Lying Leg Curl" to 40.0)

        suspend fun routine(name: String, position: Int, exercises: List<Pair<String, Double>>): Long {
            val routineId = db.routineDAO().insert(Routine(name = name, position = position, timesPerWeek = 2))
            exercises.forEachIndexed { i, (ex, _) -> db.routineDAO().insertExercise(RoutineExercise(routineId = routineId, exerciseId = id(ex), position = i)) }
            return routineId
        }
        val upperId = routine("Upper A", 0, upper)
        val lowerId = routine("Lower A", 1, lower)

        val zone = ZoneId.systemDefault()
        val thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        for (week in 8 downTo 0) {
            val monday = thisMonday.minusWeeks(week.toLong())
            listOf(0L to (upperId to upper), 2L to (lowerId to lower), 4L to (upperId to upper)).forEach { (offset, plan) ->
                val day = monday.plusDays(offset)
                if (!day.isBefore(LocalDate.now())) return@forEach
                val start = day.atTime(18, 30).atZone(zone).toInstant().toEpochMilli()
                val workoutId = db.workoutDAO().insertWorkout(
                    Workout(name = if (plan.first == upperId) "Upper A" else "Lower A", startedAt = start, endedAt = start + 62 * 60_000L, routineId = plan.first)
                )
                plan.second.forEachIndexed { i, (ex, base) ->
                    val weId = db.workoutDAO().insertWorkoutExercise(WorkoutExercise(workoutId = workoutId, exerciseId = id(ex), position = i))
                    val load = base * (1 + 0.012 * (8 - week))
                    repeat(3) { s ->
                        db.workoutDAO().insertSet(
                            WorkoutSet(workoutExerciseId = weId, position = s, weightKg = Math.round(load * 2) / 2.0, reps = 10 - s, rir = 2 - minOf(s, 1), isCompleted = true)
                        )
                    }
                }
            }
        }
    }
}
