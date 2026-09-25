package com.github.sonatadev.sbldb.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.sonatadev.sbldb.data.dao.ExerciseDAO
import com.github.sonatadev.sbldb.data.dao.ExerciseMuscleDAO
import com.github.sonatadev.sbldb.data.dao.GlossaryDAO
import com.github.sonatadev.sbldb.data.dao.JointActionDAO
import com.github.sonatadev.sbldb.data.dao.MuscleDAO
import com.github.sonatadev.sbldb.data.dao.RoutineDAO
import com.github.sonatadev.sbldb.data.dao.WorkoutDAO
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.ExerciseMuscle
import com.github.sonatadev.sbldb.data.entity.ExerciseJointAction
import com.github.sonatadev.sbldb.data.entity.GlossaryTerm
import com.github.sonatadev.sbldb.data.entity.JointAction
import com.github.sonatadev.sbldb.data.entity.JointActionMuscle
import com.github.sonatadev.sbldb.data.entity.Muscle
import com.github.sonatadev.sbldb.data.entity.Routine
import com.github.sonatadev.sbldb.data.entity.RoutineExercise
import com.github.sonatadev.sbldb.data.entity.Workout
import com.github.sonatadev.sbldb.data.entity.WorkoutExercise
import com.github.sonatadev.sbldb.data.entity.WorkoutSet

@Database(
    entities = [
        Exercise::class, Muscle::class, ExerciseMuscle::class,
        Workout::class, WorkoutExercise::class, WorkoutSet::class,
        JointAction::class, JointActionMuscle::class, ExerciseJointAction::class,
        Routine::class, RoutineExercise::class, GlossaryTerm::class
    ],
    version = 5
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDAO(): ExerciseDAO
    abstract fun muscleDAO(): MuscleDAO
    abstract fun exerciseMuscleDAO(): ExerciseMuscleDAO
    abstract fun workoutDAO(): WorkoutDAO
    abstract fun jointActionDAO(): JointActionDAO
    abstract fun routineDAO(): RoutineDAO
    abstract fun glossaryDAO(): GlossaryDAO

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN note TEXT")
            }
        }

        /** Beginner and expert explanations, plus the glossary. Content is filled by SeedData. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("whatBasic", "whatExpert", "whyBasic", "whyExpert", "feelBasic", "feelExpert")
                    .forEach { db.execSQL("ALTER TABLE joint_actions ADD COLUMN $it TEXT") }
                db.execSQL("ALTER TABLE joint_action_muscles ADD COLUMN noteBasic TEXT")
                db.execSQL("ALTER TABLE joint_action_muscles ADD COLUMN noteExpert TEXT")
                db.execSQL("ALTER TABLE muscles ADD COLUMN infoBasic TEXT")
                db.execSQL("ALTER TABLE muscles ADD COLUMN infoExpert TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS glossary (term TEXT NOT NULL, basic TEXT NOT NULL, expert TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(term))"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "sbldb_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    // Versions 1–2 only ever existed on development devices
                    .fallbackToDestructiveMigrationFrom(true, 1, 2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
