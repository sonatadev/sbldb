package com.github.sonatadev.sbldb.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sonatadev.sbldb.data.content.ContentSource
import com.github.sonatadev.sbldb.data.importer.ExerciseMatcher
import com.github.sonatadev.sbldb.data.importer.ForeignCsv
import com.github.sonatadev.sbldb.data.importer.ForeignImporter
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.data.repository.WorkoutRepository
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForeignImportTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase

    private val strong = """
        Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes,Workout Notes,RPE
        2026-09-01 18:30:00,Push,1h,Bench Press (Barbell),1,200,8,0,0,,,8
        2026-09-01 18:30:00,Push,1h,Plank,1,0,0,0,45,,,
        2026-09-01 18:30:00,Push,1h,Tire Flip,1,0,5,0,0,,,
    """.trimIndent()

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val settings = SettingsRepository(context)
        settings.setContentHash("")
        SeedData.sync(ContentSource.bundled(context), db, settings)
        Unit
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun importsMatchedExercisesOnceInKg() = runBlocking {
        val log = ForeignCsv.parse(strong)
        val library = ForeignImporter.candidates(db)
        val mapping = log.exerciseNames.associateWith { ExerciseMatcher.sure(it, library)?.exerciseId }

        val first = ForeignImporter.import(db, log, mapping, WeightUnit.LB)
        assertEquals(1, first.workouts)
        assertEquals(2, first.sets) // the tire flip is left out

        val workout = WorkoutRepository(db).history.first().single()
        val bench = workout.exercises.single { it.exercise.name == "Barbell Bench Press" }.sets.single()
        assertEquals(90.7, bench.weightKg!!, 0.05)
        assertEquals(2, bench.rir)
        assertEquals(45, workout.exercises.single { it.exercise.name == "Plank" }.sets.single().reps)

        // The same file again adds nothing
        val again = ForeignImporter.import(db, log, mapping, WeightUnit.LB)
        assertEquals(0, again.workouts)
        assertEquals(1, again.skipped)
    }
}
