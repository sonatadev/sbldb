package com.github.sonatadev.sbldb.data.backup

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.CustomExerciseInput
import com.github.sonatadev.sbldb.data.CustomExercises
import com.github.sonatadev.sbldb.data.SeedData
import com.github.sonatadev.sbldb.data.content.ContentSource
import com.github.sonatadev.sbldb.data.entity.BodyEntry
import com.github.sonatadev.sbldb.data.entity.ExerciseNote
import com.github.sonatadev.sbldb.data.entity.MuscleTarget
import com.github.sonatadev.sbldb.data.entity.PlannedWorkout
import com.github.sonatadev.sbldb.data.entity.Routine
import com.github.sonatadev.sbldb.data.entity.RoutineExercise
import com.github.sonatadev.sbldb.data.entity.SetType
import com.github.sonatadev.sbldb.data.entity.Workout
import com.github.sonatadev.sbldb.data.entity.WorkoutExercise
import com.github.sonatadev.sbldb.data.entity.WorkoutSet
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private lateinit var backups: BackupManager

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val settings = SettingsRepository(context)
        settings.setContentHash("") // force the library to be written into the fresh database
        SeedData.sync(ContentSource.bundled(context), db, settings)
        backups = BackupManager(context, db, settings)

        val bench = db.exerciseDAO().findId("Barbell Bench Press")!!
        val custom = CustomExercises.save(
            db,
            CustomExerciseInput("My Hammer Strength Press", "Machine", null, "seat 4", listOf("HS press"),
                mapOf(db.userDataDAO().actionId("Shoulder", "Horizontal Adduction")!! to 5))
        )
        val routine = db.routineDAO().insert(Routine(name = "Upper", position = 0, timesPerWeek = 2))
        db.routineDAO().insertExercise(RoutineExercise(routineId = routine, exerciseId = custom, position = 0, sets = 4, repMin = 6, repMax = 8, restSeconds = 150, jointActionId = db.userDataDAO().actionId("Shoulder", "Horizontal Adduction")))
        val workout = db.workoutDAO().insertWorkout(Workout(name = "Upper", startedAt = 1_000_000L, endedAt = 4_000_000L, routineId = routine))
        val we = db.workoutDAO().insertWorkoutExercise(WorkoutExercise(workoutId = workout, exerciseId = bench, position = 0, note = "felt strong"))
        db.workoutDAO().insertSet(WorkoutSet(workoutExerciseId = we, position = 0, weightKg = 40.0, reps = 10, isWarmup = true, isCompleted = true, setType = SetType.WARMUP))
        db.workoutDAO().insertSet(WorkoutSet(workoutExerciseId = we, position = 1, weightKg = 80.0, reps = 8, rir = 2, isCompleted = true, setType = SetType.MYO))
        db.userDataDAO().upsertExerciseNote(ExerciseNote(bench, "grip 81 cm"))
        db.userDataDAO().insertBodyEntry(BodyEntry(date = 20000, weightKg = 80.5, waistCm = 82.0))
        db.userDataDAO().upsertTarget(MuscleTarget("Shoulders", 12, 22))
        db.routineDAO().insertPlanned(PlannedWorkout(date = 20500, routineId = routine))
        Unit
    }

    @After
    fun tearDown() = db.close()

    private fun comparable(json: String) = JSONObject(json).apply { remove("exportedAt") }.toString()

    @Test
    fun exportThenRestoreGivesBackTheSameData() = runBlocking {
        val first = backups.exportJson()
        val (_, summary) = backups.inspect(first)
        assertEquals(1, summary.workouts)
        assertEquals(2, summary.sets)
        assertEquals(1, summary.customExercises)

        backups.restore(first)
        assertEquals(comparable(first), comparable(backups.exportJson()))
    }

    @Test
    fun csvHasOneRowPerSet() = runBlocking {
        val lines = backups.exportCsv().trim().lines()
        assertEquals("date,time,workout,exercise,set,type,weight_kg,reps,rir,completed", lines.first())
        assertEquals(3, lines.size)
        assertTrue(lines[2].contains("myo"))
    }

    @Test
    fun brokenFilesAreRejectedWithoutTouchingData() = runBlocking {
        val before = comparable(backups.exportJson())
        listOf(
            "not json at all",
            """{"backupVersion": 99}""",
            """{"backupVersion": 1, "workouts": [{"name": "X", "startedAt": 1, "exercises": [{"exercise": "Nonexistent Lift", "sets": []}]}]}"""
        ).forEach { bad ->
            try {
                backups.restore(bad)
                fail("Expected $bad to be rejected")
            } catch (expected: BackupException) {
            }
        }
        assertEquals(before, comparable(backups.exportJson()))
    }
}
