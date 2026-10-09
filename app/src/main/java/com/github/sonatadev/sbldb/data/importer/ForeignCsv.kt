package com.github.sonatadev.sbldb.data.importer

import com.github.sonatadev.sbldb.data.entity.SetType
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import kotlin.math.roundToInt

/** The app a workout log was exported from. */
enum class ForeignApp { STRONG, HEVY }

/** One set as read from the file, before exercises are matched. Weight is in the file's unit unless [weightIsKg]. */
data class ForeignSet(
    val weight: Double?,
    val reps: Int?,
    val seconds: Int?,
    val rir: Int?,
    val type: SetType
)

data class ForeignExercise(val name: String, val sets: List<ForeignSet>, val note: String?)

data class ForeignWorkout(
    val name: String,
    val startedAt: Long,
    val endedAt: Long,
    val notes: String?,
    val exercises: List<ForeignExercise>
)

data class ForeignLog(
    val app: ForeignApp,
    val workouts: List<ForeignWorkout>,
    /** True when the file says its weights are kg (Hevy); Strong doesn't say, so the user picks. */
    val weightIsKg: Boolean?
) {
    val exerciseNames: List<String> get() = workouts.flatMap { w -> w.exercises.map { it.name } }.distinct().sorted()
}

class ImportFormatException(message: String) : Exception(message)

/** Reads the CSV exports of Strong and Hevy. Independent of Android so it can be unit tested. */
object ForeignCsv {
    fun parse(text: String, zone: ZoneId = ZoneId.systemDefault()): ForeignLog {
        val clean = text.removePrefix("﻿")
        val firstLine = clean.lineSequence().firstOrNull().orEmpty()
        val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','
        val rows = rows(clean, delimiter)
        if (rows.size < 2) throw ImportFormatException("The file has no sets")
        val header = rows.first().map { it.trim().lowercase() }
        val body = rows.drop(1).filter { r -> r.any { it.isNotBlank() } }
        return when {
            "exercise_title" in header && "start_time" in header -> hevy(header, body, zone)
            "exercise name" in header && "date" in header -> strong(header, body, zone)
            else -> throw ImportFormatException("Not a Strong or Hevy export")
        }
    }

    // ---------------------------------------------------------------- Strong
    // Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes,Workout Notes,RPE

    private fun strong(header: List<String>, rows: List<List<String>>, zone: ZoneId): ForeignLog {
        val col = columns(header)
        val workouts = rows.groupBy { it[col.getValue("date")] + "|" + it.at(col["workout name"]) }.map { (_, sets) ->
            val first = sets.first()
            val start = parseDate(first[col.getValue("date")], STRONG_DATES).atZone(zone).toInstant().toEpochMilli()
            val minutes = strongDuration(first.at(col["duration"]))
            ForeignWorkout(
                name = first.at(col["workout name"]).ifBlank { "Workout" },
                startedAt = start,
                endedAt = start + minutes * 60_000L,
                notes = sets.map { it.at(col["workout notes"]) }.firstOrNull { it.isNotBlank() },
                exercises = sets.groupConsecutive { it[col.getValue("exercise name")] }.map { (name, list) ->
                    ForeignExercise(
                        name = name.trim(),
                        note = list.map { it.at(col["notes"]) }.firstOrNull { it.isNotBlank() },
                        sets = list.map { r ->
                            val order = r.at(col["set order"]).trim().uppercase()
                            ForeignSet(
                                weight = number(r.at(col["weight"]))?.takeIf { it > 0 },
                                reps = number(r.at(col["reps"]))?.roundToInt()?.takeIf { it > 0 },
                                seconds = number(r.at(col["seconds"]))?.roundToInt()?.takeIf { it > 0 },
                                rir = rirFromRpe(number(r.at(col["rpe"]))),
                                type = when (order) {
                                    "W" -> SetType.WARMUP
                                    "D" -> SetType.DROP
                                    "F" -> SetType.FAILURE
                                    else -> SetType.NORMAL
                                }
                            )
                        }
                    )
                }
            )
        }
        return ForeignLog(ForeignApp.STRONG, workouts.sortedBy { it.startedAt }, weightIsKg = null)
    }

    /** "1h 5m", "45m", "1h", "50s": whole minutes, at least one. */
    internal fun strongDuration(text: String): Long {
        val h = Regex("""(\d+)\s*h""").find(text)?.groupValues?.get(1)?.toLong() ?: 0
        val m = Regex("""(\d+)\s*m(?!s)""").find(text)?.groupValues?.get(1)?.toLong() ?: 0
        return (h * 60 + m).coerceAtLeast(1)
    }

    // ---------------------------------------------------------------- Hevy
    // title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,set_index,set_type,
    // weight_kg (or weight_lbs),reps,distance_km,duration_seconds,rpe

    private fun hevy(header: List<String>, rows: List<List<String>>, zone: ZoneId): ForeignLog {
        val col = columns(header)
        val kg = "weight_kg" in col
        val weightCol = col["weight_kg"] ?: col["weight_lbs"]
        val workouts = rows.groupBy { it[col.getValue("start_time")] + "|" + it.at(col["title"]) }.map { (_, sets) ->
            val first = sets.first()
            val start = parseDate(first[col.getValue("start_time")], HEVY_DATES)
            val end = first.at(col["end_time"]).takeIf { it.isNotBlank() }?.let { parseDate(it, HEVY_DATES) }
            // Hevy's times are wall-clock times without a zone: read them in the phone's zone
            val startMillis = start.atZone(zone).toInstant().toEpochMilli()
            ForeignWorkout(
                name = first.at(col["title"]).ifBlank { "Workout" },
                startedAt = startMillis,
                endedAt = end?.atZone(zone)?.toInstant()?.toEpochMilli()?.takeIf { it > startMillis } ?: (startMillis + 60_000L),
                notes = sets.map { it.at(col["description"]) }.firstOrNull { it.isNotBlank() },
                exercises = sets.groupConsecutive { it[col.getValue("exercise_title")] }.map { (name, list) ->
                    ForeignExercise(
                        name = name.trim(),
                        note = list.map { it.at(col["exercise_notes"]) }.firstOrNull { it.isNotBlank() },
                        sets = list.map { r ->
                            ForeignSet(
                                weight = number(r.at(weightCol))?.takeIf { it > 0 },
                                reps = number(r.at(col["reps"]))?.roundToInt()?.takeIf { it > 0 },
                                seconds = number(r.at(col["duration_seconds"]))?.roundToInt()?.takeIf { it > 0 },
                                rir = rirFromRpe(number(r.at(col["rpe"]))),
                                type = when (r.at(col["set_type"]).trim().lowercase()) {
                                    "warmup" -> SetType.WARMUP
                                    "dropset" -> SetType.DROP
                                    "failure" -> SetType.FAILURE
                                    else -> SetType.NORMAL
                                }
                            )
                        }
                    )
                }
            )
        }
        return ForeignLog(ForeignApp.HEVY, workouts.sortedBy { it.startedAt }, weightIsKg = kg)
    }

    // ---------------------------------------------------------------- helpers

    /** RPE 10 = no reps left; RPE 8 = 2 left. Outside 5–10 it isn't a usable RPE. */
    internal fun rirFromRpe(rpe: Double?): Int? = rpe?.takeIf { it in 5.0..10.0 }?.let { (10 - it).roundToInt() }

    private val STRONG_DATES = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss")
    private val HEVY_DATES = listOf("d MMM yyyy, HH:mm", "d MMM yyyy HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "dd/MM/yyyy HH:mm")

    private fun parseDate(text: String, patterns: List<String>): LocalDateTime {
        val trimmed = text.trim()
        for (p in patterns) {
            val f = DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(p).toFormatter(Locale.ENGLISH)
            runCatching { return LocalDateTime.parse(trimmed, f) }
        }
        runCatching { return LocalDateTime.parse(trimmed.removeSuffix("Z"), DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        throw ImportFormatException("Unreadable date '$trimmed'")
    }

    /** Accepts "82.5" and "82,5". */
    private fun number(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    private fun columns(header: List<String>): Map<String, Int> = header.withIndex().associate { (i, h) -> h to i }

    private fun List<String>.at(index: Int?): String = index?.let { getOrNull(it) }.orEmpty()

    /** Runs of equal keys, in order: the same exercise twice in a workout stays two entries. */
    private fun <T> List<T>.groupConsecutive(key: (T) -> String): List<Pair<String, List<T>>> {
        val out = mutableListOf<Pair<String, MutableList<T>>>()
        forEach { item -> val k = key(item); if (out.lastOrNull()?.first == k) out.last().second += item else out += k to mutableListOf(item) }
        return out
    }

    /** RFC 4180 rows: quoted fields may hold the delimiter, quotes ("") and line breaks. */
    internal fun rows(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == delimiter -> { row += field.toString(); field.clear() }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    row += field.toString(); field.clear(); rows += row; row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row += field.toString(); rows += row }
        return rows
    }
}
