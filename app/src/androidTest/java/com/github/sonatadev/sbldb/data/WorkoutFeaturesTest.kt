package com.github.sonatadev.sbldb.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sonatadev.sbldb.data.content.ContentSource
import com.github.sonatadev.sbldb.data.entity.SetType
import com.github.sonatadev.sbldb.data.entity.Workout
import com.github.sonatadev.sbldb.data.entity.WorkoutExercise
import com.github.sonatadev.sbldb.data.entity.WorkoutSet
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutFeaturesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private lateinit var repo: WorkoutRepository

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val settings = SettingsRepository(context)
        settings.setContentHash("")
        SeedData.sync(ContentSource.bundled(context), db, settings)
        repo = WorkoutRepository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun swapSuggestsTheSupportedTwinAndKeepsTheSets() = runBlocking {
        val barbellRow = db.exerciseDAO().findId("Barbell Row")!!
        val names = repo.swapCandidates(barbellRow).map { it.name }
        assertTrue(names.toString(), names.take(5).any { it.contains("Seal Row") || it.contains("Chest-Supported") })
        assertFalse(names.contains("Barbell Row"))

        val workout = db.workoutDAO().insertWorkout(Workout(name = "W", startedAt = 1L))
        val we = db.workoutDAO().insertWorkoutExercise(WorkoutExercise(workoutId = workout, exerciseId = barbellRow, position = 0))
        db.workoutDAO().insertSet(WorkoutSet(workoutExerciseId = we, position = 0, weightKg = 60.0, reps = 8))
        val target = db.exerciseDAO().findId(names.first())!!
        repo.swapExercise(repo.workout(workout).first()!!.exercises.single().workoutExercise, target)
        val after = repo.workout(workout).first()!!.exercises.single()
        assertEquals(target, after.exercise.exerciseId)
        assertEquals(60.0, after.sets.single().weightKg!!, 0.0)
    }

    @Test
    fun setTypeKeepsTheWarmupFlagInSync() = runBlocking {
        val workout = db.workoutDAO().insertWorkout(Workout(name = "W", startedAt = 1L))
        val we = db.workoutDAO().insertWorkoutExercise(WorkoutExercise(workoutId = workout, exerciseId = db.exerciseDAO().findId("Barbell Row")!!, position = 0))
        val setId = db.workoutDAO().insertSet(WorkoutSet(workoutExerciseId = we, position = 0))
        suspend fun set() = repo.workout(workout).first()!!.exercises.single().sets.single { it.setId == setId }

        repo.updateSetType(setId, SetType.WARMUP)
        assertTrue(set().isWarmup)
        repo.updateSetType(setId, SetType.DROP)
        assertEquals(SetType.DROP, set().setType)
        assertFalse(set().isWarmup)
    }

    @Test
    fun prefillFillsOnlyTheGivenSets() = runBlocking {
        val workout = db.workoutDAO().insertWorkout(Workout(name = "W", startedAt = 1L))
        val we = db.workoutDAO().insertWorkoutExercise(WorkoutExercise(workoutId = workout, exerciseId = db.exerciseDAO().findId("Barbell Row")!!, position = 0))
        val a = db.workoutDAO().insertSet(WorkoutSet(workoutExerciseId = we, position = 0, weightKg = 50.0, reps = 10, isCompleted = true))
        val b = db.workoutDAO().insertSet(WorkoutSet(workoutExerciseId = we, position = 1))
        repo.prefill(listOf(b), 62.5, 8)
        val sets = repo.workout(workout).first()!!.exercises.single().sets.associateBy { it.setId }
        assertEquals(50.0, sets.getValue(a).weightKg!!, 0.0)
        assertEquals(62.5, sets.getValue(b).weightKg!!, 0.0)
        assertEquals(8, sets.getValue(b).reps)
    }
}
