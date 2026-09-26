package com.github.sonatadev.sbldb.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.CustomExerciseInput
import com.github.sonatadev.sbldb.data.CustomExercises
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields

class BackupException(message: String) : Exception(message)

data class BackupSummary(val workouts: Int, val sets: Int, val routines: Int, val customExercises: Int, val bodyEntries: Int)

/**
 * Exports and restores everything that belongs to the user: workouts, routines, custom exercises,
 * notes, body entries, volume targets and settings. Exercises are referenced by name so a backup
 * stays valid whatever the library looks like when it is restored.
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository
) {
    private val dao = db.userDataDAO()

    // ------------------------------------------------------------------ export

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val names = dao.exerciseNames().associate { it.exerciseId to it.name }
        val routinesById = dao.allRoutines().associateBy { it.routine.routineId }
        val root = JSONObject()
            .put("backupVersion", VERSION)
            .put("app", "sbl.db")
            .put("exportedAt", System.currentTimeMillis())
            .put("settings", settings.snapshot())

        root.put("customExercises", JSONArray().apply {
            dao.customExercises().forEach { e ->
                put(JSONObject()
                    .put("name", e.name).put("equipment", e.equipment).putOpt("attachment", e.attachment)
                    .putOpt("note", e.note).put("aliases", JSONArray(e.aliasList)).put("archived", e.isArchived)
                    .put("actions", JSONArray().apply {
                        dao.actionRatings(e.exerciseId).forEach { put(JSONObject().put("joint", it.joint).put("name", it.name).put("rating", it.rating)) }
                    }))
            }
        })
        val actionsById = db.jointActionDAO().allActions().associateBy { it.jointActionId }
        root.put("routines", JSONArray().apply {
            routinesById.values.forEach { r ->
                put(JSONObject()
                    .put("name", r.routine.name).put("position", r.routine.position).put("timesPerWeek", r.routine.timesPerWeek)
                    .put("exercises", JSONArray().apply {
                        r.exercises.sortedBy { it.routineExercise.position }.forEach { entry ->
                            val p = entry.routineExercise
                            put(JSONObject()
                                .put("exercise", entry.exercise.name).put("position", p.position).put("sets", p.sets)
                                .put("repMin", p.repMin).put("repMax", p.repMax).putOpt("targetRir", p.targetRir).put("restSeconds", p.restSeconds)
                                .putOpt("movement", p.jointActionId?.let { actionsById[it] }?.let { JSONObject().put("joint", it.joint).put("name", it.name) }))
                        }
                    }))
            }
        })
        // Planned days refer to routines by their index in the list above (names can repeat)
        val routineIndex = routinesById.keys.withIndex().associate { (i, id) -> id to i }
        root.put("planned", JSONArray().apply {
            db.routineDAO().allPlanned().forEach { p ->
                routineIndex[p.routineId]?.let { put(JSONObject().put("date", p.date).put("routine", it)) }
            }
        })
        root.put("workouts", JSONArray().apply {
            dao.allWorkouts().forEach { w ->
                put(JSONObject()
                    .put("name", w.workout.name).put("startedAt", w.workout.startedAt).putOpt("endedAt", w.workout.endedAt)
                    .putOpt("notes", w.workout.notes).putOpt("routine", w.workout.routineId?.let { routinesById[it]?.routine?.name })
                    .put("exercises", JSONArray().apply {
                        w.exercises.sortedBy { it.workoutExercise.position }.forEach { ex ->
                            put(JSONObject()
                                .put("exercise", ex.exercise.name).put("position", ex.workoutExercise.position).putOpt("note", ex.workoutExercise.note)
                                .put("sets", JSONArray().apply {
                                    ex.sets.sortedBy { it.position }.forEach { s ->
                                        put(JSONObject()
                                            .put("position", s.position).putOpt("weightKg", s.weightKg).putOpt("reps", s.reps).putOpt("rir", s.rir)
                                            .put("type", s.setType.name).put("completed", s.isCompleted))
                                    }
                                }))
                        }
                    }))
            }
        })
        root.put("exerciseNotes", JSONArray().apply {
            dao.allExerciseNotes().forEach { n -> names[n.exerciseId]?.let { put(JSONObject().put("exercise", it).put("text", n.text)) } }
        })
        root.put("body", JSONArray().apply {
            dao.allBodyEntries().forEach { b ->
                put(JSONObject().put("date", LocalDate.ofEpochDay(b.date).toString()).putOpt("weightKg", b.weightKg)
                    .putOpt("waistCm", b.waistCm).putOpt("chestCm", b.chestCm).putOpt("armCm", b.armCm).putOpt("thighCm", b.thighCm))
            }
        })
        root.put("volumeTargets", JSONArray().apply {
            dao.allTargets().forEach { put(JSONObject().put("muscleGroup", it.muscleGroup).put("minSets", it.minSets).put("maxSets", it.maxSets)) }
        })
        root.toString(2)
    }

    /** One row per set, for spreadsheets. */
    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        buildString {
            appendLine("date,time,workout,exercise,set,type,weight_kg,reps,rir,completed")
            dao.allWorkouts().forEach { w ->
                val at = Instant.ofEpochMilli(w.workout.startedAt).atZone(zone)
                w.exercises.sortedBy { it.workoutExercise.position }.forEach { ex ->
                    ex.sets.sortedBy { it.position }.forEachIndexed { i, s ->
                        appendLine(
                            listOf(
                                at.toLocalDate().toString(), at.toLocalTime().withNano(0).toString(), w.workout.name, ex.exercise.name,
                                (i + 1).toString(), s.setType.name.lowercase(), s.weightKg?.toString().orEmpty(),
                                s.reps?.toString().orEmpty(), s.rir?.toString().orEmpty(), s.isCompleted.toString()
                            ).joinToString(",") { csv(it) }
                        )
                    }
                }
            }
        }
    }

    private fun csv(value: String) = if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    // ------------------------------------------------------------------ import

    /** Parses and checks a backup without touching the database. */
    suspend fun inspect(json: String): Pair<JSONObject, BackupSummary> = withContext(Dispatchers.IO) {
        val root = try { JSONObject(json) } catch (e: Exception) { throw BackupException("This is not an sbl.db backup file") }
        val version = root.optInt("backupVersion", -1)
        if (version !in 1..VERSION) throw BackupException("Backup version $version is not supported by this app version")

        val library = dao.exerciseNames().map { it.name }.toSet() - dao.customExercises().map { it.name }.toSet()
        val custom = root.optJSONArray("customExercises").objects().map { it.getString("name") }.toSet()
        val known = library + custom
        val referenced = root.optJSONArray("workouts").objects().flatMap { w -> w.optJSONArray("exercises").objects().map { it.getString("exercise") } } +
            root.optJSONArray("routines").objects().flatMap { r -> r.optJSONArray("exercises").objects().map { it.getString("exercise") } }
        val unknown = referenced.filter { it !in known }.distinct()
        if (unknown.isNotEmpty()) throw BackupException("Unknown exercises in backup: ${unknown.take(3).joinToString()}")
        root.optJSONArray("customExercises").objects().forEach { e ->
            e.optJSONArray("actions").objects().forEach { a ->
                if (dao.actionId(a.getString("joint"), a.getString("name")) == null) {
                    throw BackupException("Unknown joint action ${a.getString("joint")} / ${a.getString("name")}")
                }
            }
        }
        val workouts = root.optJSONArray("workouts").objects()
        root to BackupSummary(
            workouts = workouts.size,
            sets = workouts.sumOf { w -> w.optJSONArray("exercises").objects().sumOf { it.optJSONArray("sets")?.length() ?: 0 } },
            routines = root.optJSONArray("routines")?.length() ?: 0,
            customExercises = custom.size,
            bodyEntries = root.optJSONArray("body")?.length() ?: 0
        )
    }

    /** Replaces all user data with the backup's content. The library itself is not touched. */
    suspend fun restore(json: String) = withContext(Dispatchers.IO) {
        val (root, _) = inspect(json)
        db.withTransaction {
            dao.deleteWorkouts(); dao.deleteRoutines(); dao.deleteExerciseNotes(); dao.deleteBodyEntries(); dao.deleteTargets()
            dao.deleteCustomExercises()

            root.optJSONArray("customExercises").objects().forEach { e ->
                val ratings = e.optJSONArray("actions").objects().associate { a ->
                    requireNotNull(dao.actionId(a.getString("joint"), a.getString("name"))) to a.getInt("rating")
                }
                val id = CustomExercises.save(
                    db,
                    CustomExerciseInput(
                        e.getString("name"), e.optString("equipment", "Other"), e.optStringOrNull("attachment"),
                        e.optStringOrNull("note"), e.optJSONArray("aliases").strings(), ratings
                    )
                )
                if (e.optBoolean("archived")) db.exerciseDAO().findByName(e.getString("name"))?.let { db.exerciseDAO().updateExercise(it.copy(isArchived = true)) }
                check(id > 0)
            }
            val ids = dao.exerciseNames().associate { it.name to it.exerciseId }

            val routineIds = HashMap<String, Long>()
            val routinesInOrder = mutableListOf<Long>()
            root.optJSONArray("routines").objects().forEach { r ->
                val routineId = db.routineDAO().insert(Routine(name = r.getString("name"), position = r.optInt("position"), timesPerWeek = r.optInt("timesPerWeek", 1)))
                routineIds.putIfAbsent(r.getString("name"), routineId)
                routinesInOrder += routineId
                r.optJSONArray("exercises").objects().forEach { x ->
                    db.routineDAO().insertExercise(
                        RoutineExercise(
                            routineId = routineId, exerciseId = ids.getValue(x.getString("exercise")), position = x.optInt("position"),
                            sets = x.optInt("sets", 3), repMin = x.optInt("repMin", 8), repMax = x.optInt("repMax", 12),
                            targetRir = x.optIntOrNull("targetRir"), restSeconds = x.optInt("restSeconds", 120),
                            jointActionId = x.optJSONObject("movement")?.let { m -> dao.actionId(m.getString("joint"), m.getString("name")) }
                        )
                    )
                }
            }

            root.optJSONArray("planned").objects().forEach { p ->
                routinesInOrder.getOrNull(p.getInt("routine"))?.let { db.routineDAO().insertPlanned(PlannedWorkout(date = p.getLong("date"), routineId = it)) }
            }

            val workoutDao = db.workoutDAO()
            root.optJSONArray("workouts").objects().forEach { w ->
                val workoutId = workoutDao.insertWorkout(
                    Workout(
                        name = w.getString("name"), startedAt = w.getLong("startedAt"), endedAt = w.optLongOrNull("endedAt"),
                        notes = w.optStringOrNull("notes"), routineId = w.optStringOrNull("routine")?.let { routineIds[it] }
                    )
                )
                w.optJSONArray("exercises").objects().forEach { x ->
                    val weId = workoutDao.insertWorkoutExercise(
                        WorkoutExercise(workoutId = workoutId, exerciseId = ids.getValue(x.getString("exercise")), position = x.optInt("position"), note = x.optStringOrNull("note"))
                    )
                    x.optJSONArray("sets").objects().forEach { s ->
                        val type = runCatching { SetType.valueOf(s.optString("type", "NORMAL")) }.getOrDefault(SetType.NORMAL)
                        workoutDao.insertSet(
                            WorkoutSet(
                                workoutExerciseId = weId, position = s.optInt("position"), weightKg = s.optDoubleOrNull("weightKg"),
                                reps = s.optIntOrNull("reps"), rir = s.optIntOrNull("rir"), isWarmup = type == SetType.WARMUP,
                                isCompleted = s.optBoolean("completed"), setType = type
                            )
                        )
                    }
                }
            }
            root.optJSONArray("exerciseNotes").objects().forEach { n ->
                ids[n.getString("exercise")]?.let { dao.upsertExerciseNote(ExerciseNote(it, n.getString("text"))) }
            }
            root.optJSONArray("body").objects().forEach { b ->
                dao.insertBodyEntry(
                    BodyEntry(
                        date = LocalDate.parse(b.getString("date")).toEpochDay(), weightKg = b.optDoubleOrNull("weightKg"),
                        waistCm = b.optDoubleOrNull("waistCm"), chestCm = b.optDoubleOrNull("chestCm"),
                        armCm = b.optDoubleOrNull("armCm"), thighCm = b.optDoubleOrNull("thighCm")
                    )
                )
            }
            root.optJSONArray("volumeTargets").objects().forEach { t ->
                dao.upsertTarget(MuscleTarget(t.getString("muscleGroup"), t.getInt("minSets"), t.getInt("maxSets")))
            }
        }
        root.optJSONObject("settings")?.let { settings.restore(it) }
    }

    // ------------------------------------------------------------------ files

    suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: throw BackupException("Could not write the file")
    }

    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } ?: throw BackupException("Could not read the file")
    }

    /**
     * Writes the latest backup to the folder the user picked, plus one dated copy per week (the
     * last [WEEKLY_COPIES] are kept). Returns false when no folder is set or it is unreachable.
     */
    suspend fun autoBackup(): Boolean = withContext(Dispatchers.IO) {
        val treeUri = settings.backupFolder() ?: return@withContext false
        val folder = DocumentFile.fromTreeUri(context, Uri.parse(treeUri))?.takeIf { it.canWrite() } ?: return@withContext false
        val json = exportJson()
        val week = LocalDate.now().let { "%d-W%02d".format(it.get(IsoFields.WEEK_BASED_YEAR), it.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)) }
        runCatching {
            listOf(LATEST, "$WEEKLY_PREFIX$week").forEach { name ->
                val file = folder.findFile("$name.json") ?: folder.createFile("application/json", name) ?: throw BackupException("Could not create $name")
                write(file.uri, json)
            }
            folder.listFiles()
                .filter { it.name?.startsWith(WEEKLY_PREFIX) == true }
                .sortedByDescending { it.name }
                .drop(WEEKLY_COPIES)
                .forEach { it.delete() }
            settings.setLastBackup(System.currentTimeMillis())
        }.isSuccess
    }

    companion object {
        const val VERSION = 1
        private const val LATEST = "sbldb-backup-latest"
        private const val WEEKLY_PREFIX = "sbldb-backup-week-"
        private const val WEEKLY_COPIES = 4
    }
}

private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else List(length()) { getJSONObject(it) }
private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { getString(it) }
private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key).takeIf { has(key) }
private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null
private fun JSONObject.optLongOrNull(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
private fun JSONObject.optDoubleOrNull(key: String): Double? = if (has(key) && !isNull(key)) getDouble(key) else null
