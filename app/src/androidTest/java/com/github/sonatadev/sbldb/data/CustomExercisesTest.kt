package com.github.sonatadev.sbldb.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sonatadev.sbldb.data.content.ContentSource
import com.github.sonatadev.sbldb.data.entity.Role
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomExercisesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        settings = SettingsRepository(context)
        settings.setContentHash("")
        SeedData.sync(ContentSource.bundled(context), db, settings)
        Unit
    }

    @After
    fun tearDown() = db.close()

    private suspend fun action(joint: String, name: String) = db.userDataDAO().actionId(joint, name)!!

    private suspend fun groups(exerciseId: Int, role: Role) =
        db.exerciseDAO().getMusclesForExercise(exerciseId).first().filter { it.role == role }.map { it.muscle.muscleGroup }.toSet()

    @Test
    fun musclesAreDerivedFromTheRatedActions() = runBlocking {
        val id = CustomExercises.save(
            db,
            CustomExerciseInput("Cable Bayesian Curl Kneeling", "Cable", "D-handle", null, emptyList(),
                mapOf(action("Elbow", "Flexion") to 5, action("Shoulder", "Flexion") to 1))
        )
        assertTrue("Biceps" in groups(id, Role.PRIMARY))
        // A 1-rated action never makes its prime movers primary
        assertFalse("Shoulders" in groups(id, Role.PRIMARY))
        assertTrue(db.exerciseDAO().getExercise(id).first()!!.isCustom)
    }

    @Test
    fun lowRatingsOnlyGiveSecondaryMuscles() = runBlocking {
        val id = CustomExercises.save(
            db,
            CustomExerciseInput("Odd Curl", "Band", null, null, emptyList(), mapOf(action("Elbow", "Flexion") to 2))
        )
        assertTrue(groups(id, Role.PRIMARY).isEmpty())
        assertTrue("Biceps" in groups(id, Role.SECONDARY))
    }

    @Test
    fun syncNeverOverwritesACustomExerciseWithTheSameName() = runBlocking {
        // The user made "Toosa Curl" before the library shipped it
        val bundled = db.exerciseDAO().findByName("Toosa Curl")!!
        db.exerciseDAO().deleteExercise(bundled)
        val id = CustomExercises.save(
            db,
            CustomExerciseInput("Toosa Curl", "Band", null, "my version", emptyList(), mapOf(action("Elbow", "Flexion") to 4))
        )

        settings.setContentHash("")
        SeedData.sync(ContentSource.bundled(context), db, settings)

        val after = db.exerciseDAO().findByName("Toosa Curl")!!
        assertEquals(id, after.exerciseId)
        assertTrue(after.isCustom)
        assertEquals("Band", after.equipment)
        assertEquals("my version", after.note)
        assertEquals(listOf(4), db.jointActionDAO().getActionsForExercise(id).first().map { it.rating })
    }

    @Test
    fun usedCustomExerciseIsArchivedUnusedIsDeleted() = runBlocking {
        val input = CustomExerciseInput("Temp Press", "Machine", null, null, emptyList(), mapOf(action("Shoulder", "Flexion") to 4))
        val unused = CustomExercises.save(db, input)
        CustomExercises.remove(db, db.exerciseDAO().getExercise(unused).first()!!)
        assertNull(db.exerciseDAO().getExercise(unused).first())

        val used = CustomExercises.save(db, input)
        val workout = db.workoutDAO().insertWorkout(Workout(name = "W", startedAt = 1L, endedAt = 2L))
        val we = db.workoutDAO().insertWorkoutExercise(WorkoutExercise(workoutId = workout, exerciseId = used, position = 0))
        db.workoutDAO().insertSet(WorkoutSet(workoutExerciseId = we, position = 0, reps = 10, isCompleted = true))
        CustomExercises.remove(db, db.exerciseDAO().getExercise(used).first()!!)
        val archived = db.exerciseDAO().getExercise(used).first()
        assertNotNull(archived)
        assertTrue(archived!!.isArchived)
    }

    @Test
    fun editingHistoryAddsDoneSetsAndMovesTheWorkout() = runBlocking {
        val repo = WorkoutRepository(db)
        val workout = Workout(name = "Old", startedAt = 1_000_000L, endedAt = 2_000_000L)
        val workoutId = db.workoutDAO().insertWorkout(workout)
        repo.addExercise(workoutId, db.exerciseDAO().findId("Barbell Bench Press")!!)
        val sets = repo.workout(workoutId).first()!!.exercises.single().sets
        assertTrue(sets.single().isCompleted)

        repo.updateTimes(workout.copy(workoutId = workoutId), 5_000_000L, 3_600_000L)
        val moved = db.workoutDAO().findWorkout(workoutId)!!
        assertEquals(5_000_000L, moved.startedAt)
        assertEquals(8_600_000L, moved.endedAt)
    }
}
