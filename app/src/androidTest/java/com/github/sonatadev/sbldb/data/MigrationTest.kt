package com.github.sonatadev.sbldb.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Logged workouts must survive schema upgrades now that the app is used for real. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate3To5KeepsWorkouts() {
        helper.createDatabase(dbName, 3).apply {
            execSQL("INSERT INTO exercises (exerciseId, name, equipment, attachment) VALUES (1, 'Barbell Row', 'Barbell', NULL)")
            execSQL("INSERT INTO workouts (workoutId, name, startedAt, endedAt, notes, routineId) VALUES (1, 'Upper', 1000, 2000, NULL, NULL)")
            execSQL("INSERT INTO workout_exercises (workoutExerciseId, workoutId, exerciseId, position) VALUES (1, 1, 1, 0)")
            execSQL("INSERT INTO workout_sets (setId, workoutExerciseId, position, weightKg, reps, rir, isWarmup, isCompleted) VALUES (1, 1, 0, 80.0, 8, 2, 0, 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 5, true, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)

        db.query("SELECT name, note FROM exercises WHERE exerciseId = 1").use { c ->
            c.moveToFirst()
            assertEquals("Barbell Row", c.getString(0))
            assertNull(c.getString(1))
        }
        db.query("SELECT COUNT(*) FROM glossary").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.query("SELECT weightKg, reps FROM workout_sets WHERE setId = 1").use { c ->
            c.moveToFirst()
            assertEquals(80.0, c.getDouble(0), 0.0)
            assertEquals(8, c.getInt(1))
        }
    }
}
