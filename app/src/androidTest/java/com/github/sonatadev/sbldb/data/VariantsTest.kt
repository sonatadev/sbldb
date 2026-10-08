package com.github.sonatadev.sbldb.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sonatadev.sbldb.data.content.ContentSource
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.Role
import com.github.sonatadev.sbldb.data.entity.Workout
import com.github.sonatadev.sbldb.data.entity.WorkoutExercise
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VariantsTest {
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

    private suspend fun id(name: String) = db.exerciseDAO().findId(name)!!

    private suspend fun groups(exerciseId: Int, role: Role) =
        db.exerciseDAO().getMusclesForExercise(exerciseId).first().filter { it.role == role }.map { it.muscle.muscleGroup }.toSet()

    @Test
    fun theDefaultAttachmentIsTheExerciseItself() = runBlocking {
        val pulldown = id("Lat Pulldown")
        assertEquals(pulldown, Variants.idFor(db, pulldown, "Wide Bar"))
    }

    @Test
    fun aLibraryVariantHasItsOwnMovement() = runBlocking {
        val vBar = Variants.idFor(db, id("Lat Pulldown"), "V-Bar")
        val row = db.exerciseDAO().findById(vBar)!!
        assertEquals("Lat Pulldown · V-Bar", row.name)
        assertTrue(row.hasOwnActions)
        val actions = db.userDataDAO().actionRatings(vBar).associate { "${it.joint} / ${it.name}" to it.rating }
        assertEquals(4, actions["Shoulder / Extension"])
        assertNull(actions["Shoulder / Adduction"])
    }

    @Test
    fun anyOtherAttachmentCopiesTheExercise() = runBlocking {
        val pushdown = id("Triceps Pushdown")
        val vBar = Variants.idFor(db, pushdown, "V-Bar")
        assertNotEquals(pushdown, vBar)
        assertEquals(vBar, Variants.idFor(db, pushdown, "V-Bar"))
        assertEquals(groups(pushdown, Role.PRIMARY), groups(vBar, Role.PRIMARY))
        assertFalse(db.exerciseDAO().findById(vBar)!!.hasOwnActions)
        // From a variant, the family is still the same exercise
        assertEquals(pushdown, Variants.idFor(db, vBar, "Rope"))
        // Variants stay out of the library list
        assertTrue(db.exerciseDAO().getAllExercises().first().none { it.isVariant })
    }

    @Test
    fun aMergedExerciseKeepsItsHistory() = runBlocking {
        // A database from before the merge: the old exercise with a logged workout
        db.close()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val old = db.exerciseDAO().insertExercise(Exercise(0, "Close-Grip Lat Pulldown", "Cable", "V-Bar")).toInt()
        val workout = db.workoutDAO().insertWorkout(Workout(name = "Pull", startedAt = 1000, endedAt = 2000))
        db.workoutDAO().insertWorkoutExercise(WorkoutExercise(workoutId = workout, exerciseId = old, position = 0))

        settings.setContentHash("")
        SeedData.sync(ContentSource.bundled(context), db, settings)

        val row = db.exerciseDAO().findById(old)!!
        assertEquals("Lat Pulldown · V-Bar", row.name)
        assertEquals(id("Lat Pulldown"), row.parentId)
        assertNull(db.exerciseDAO().findByName("Close-Grip Lat Pulldown"))
        // Backups and old names still find it
        assertEquals(old, Variants.resolve(db, "Close-Grip Lat Pulldown"))
    }

    @Test
    fun backupNamesResolveToVariants() = runBlocking {
        val made = Variants.resolve(db, "Triceps Pushdown · EZ Bar")!!
        assertEquals("EZ Bar", db.exerciseDAO().findById(made)!!.attachment)
        assertEquals(id("Triceps Pushdown"), Variants.resolve(db, "Tricep Pushdown (Rope Attachment)"))
        assertNull(Variants.resolve(db, "Not An Exercise"))
    }
}
