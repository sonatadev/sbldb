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
import com.github.sonatadev.sbldb.data.dao.UserDataDAO
import com.github.sonatadev.sbldb.data.dao.WorkoutDAO
import com.github.sonatadev.sbldb.data.entity.Exercise
import com.github.sonatadev.sbldb.data.entity.ExerciseMuscle
import com.github.sonatadev.sbldb.data.entity.BodyEntry
import com.github.sonatadev.sbldb.data.entity.ExerciseJointAction
import com.github.sonatadev.sbldb.data.entity.ExerciseNote
import com.github.sonatadev.sbldb.data.entity.MuscleTarget
import com.github.sonatadev.sbldb.data.entity.GlossaryTerm
import com.github.sonatadev.sbldb.data.entity.JointAction
import com.github.sonatadev.sbldb.data.entity.JointActionMuscle
import com.github.sonatadev.sbldb.data.entity.Muscle
import com.github.sonatadev.sbldb.data.entity.PlannedWorkout
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
        Routine::class, RoutineExercise::class, GlossaryTerm::class,
        ExerciseNote::class, BodyEntry::class, MuscleTarget::class, PlannedWorkout::class
    ],
    version = 10
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDAO(): ExerciseDAO
    abstract fun muscleDAO(): MuscleDAO
    abstract fun exerciseMuscleDAO(): ExerciseMuscleDAO
    abstract fun workoutDAO(): WorkoutDAO
    abstract fun jointActionDAO(): JointActionDAO
    abstract fun routineDAO(): RoutineDAO
    abstract fun glossaryDAO(): GlossaryDAO
    abstract fun userDataDAO(): UserDataDAO

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN note TEXT")
            }
        }

        /** Set types, notes, rest times, routine frequency, custom exercises, body entries, volume targets. */
        /** Routine slots can remember the joint action they were picked for. */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_exercises ADD COLUMN jointActionId INTEGER")
            }
        }

        /** Routines planned on calendar days. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS planned_workouts (plannedId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "date INTEGER NOT NULL, routineId INTEGER NOT NULL, " +
                        "FOREIGN KEY(routineId) REFERENCES routines(routineId) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_planned_workouts_date ON planned_workouts (date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_planned_workouts_routineId ON planned_workouts (routineId)")
            }
        }

        /** Joint-action animations, filled by the next content sync. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE joint_actions ADD COLUMN animation TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sets ADD COLUMN setType TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("UPDATE workout_sets SET setType = 'WARMUP' WHERE isWarmup = 1")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN note TEXT")
                db.execSQL("ALTER TABLE routine_exercises ADD COLUMN restSeconds INTEGER NOT NULL DEFAULT 120")
                db.execSQL("ALTER TABLE routines ADD COLUMN timesPerWeek INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE exercises ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `exercise_notes` (`exerciseId` INTEGER NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`exerciseId`), " +
                        "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`exerciseId`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `body_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, " +
                        "`weightKg` REAL, `waistCm` REAL, `chestCm` REAL, `armCm` REAL, `thighCm` REAL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_body_entries_date` ON `body_entries` (`date`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `muscle_targets` (`muscleGroup` TEXT NOT NULL, `minSets` INTEGER NOT NULL, " +
                        "`maxSets` INTEGER NOT NULL, PRIMARY KEY(`muscleGroup`))"
                )
            }
        }

        /** Searchable alternative names for exercises. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN aliases TEXT")
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    // Versions 1–2 only ever existed on development devices
                    .fallbackToDestructiveMigrationFrom(true, 1, 2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
