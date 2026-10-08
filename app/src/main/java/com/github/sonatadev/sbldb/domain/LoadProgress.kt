package com.github.sonatadev.sbldb.domain

import com.github.sonatadev.sbldb.data.entity.SetHistoryRow

/** What a load chart follows, session by session. */
enum class LoadMetric {
    /** Best estimated 1RM of the session. */
    E1RM,

    /** Heaviest straight set of the session. */
    HEAVIEST,

    /** Weight × reps summed over the session's working sets. */
    VOLUME
}

/** One exercise's trend over a period: the series is oldest first, values in kg. */
data class ExerciseProgress(
    val exerciseId: Int,
    val name: String,
    val series: List<Pair<Long, Double>>,
    val lastSessionAt: Long
) {
    /** Relative change from the first to the last session, e.g. 0.05 for +5%; null with fewer than two sessions. */
    val change: Double? get() = LoadProgress.change(series)

    val trend: Trend get() = LoadProgress.trend(change)
}

enum class Trend { UP, FLAT, DOWN, NEW }

object LoadProgress {
    /** Changes within ±2.5% count as flat: day-to-day noise rather than progress. */
    const val FLAT_BAND = 0.025

    /** One point per session with a usable value, oldest first. */
    fun series(history: List<SetHistoryRow>, metric: LoadMetric): List<Pair<Long, Double>> =
        history
            .groupBy { it.workoutId }
            .mapNotNull { (_, sets) -> value(sets, metric)?.let { sets.first().startedAt to it } }
            .sortedBy { it.first }

    fun value(sets: List<SetHistoryRow>, metric: LoadMetric): Double? = when (metric) {
        LoadMetric.E1RM -> sets.mapNotNull { OneRepMax.epley(it.weightKg, it.reps) }.maxOrNull()
        LoadMetric.HEAVIEST -> sets.filter { it.isStraight && (it.reps ?: 0) > 0 }.mapNotNull { it.weightKg }.filter { it > 0 }.maxOrNull()
        LoadMetric.VOLUME -> sets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }.takeIf { it > 0 }
    }

    fun change(series: List<Pair<Long, Double>>): Double? {
        if (series.size < 2) return null
        val first = series.first().second
        return if (first > 0) (series.last().second - first) / first else null
    }

    fun trend(change: Double?): Trend = when {
        change == null -> Trend.NEW
        change > FLAT_BAND -> Trend.UP
        change < -FLAT_BAND -> Trend.DOWN
        else -> Trend.FLAT
    }

    /**
     * Trend of every exercise in [rows] (sets of one period, any order), most recently trained first.
     * Counterweight machines are left out: more weight there means less work, so the trend would read backwards.
     */
    fun of(rows: List<Pair<ExerciseRef, SetHistoryRow>>, metric: LoadMetric): List<ExerciseProgress> =
        rows
            .groupBy({ it.first }, { it.second })
            .filterKeys { !Progression.isAssisted(it.name) }
            .mapNotNull { (exercise, sets) ->
                val series = series(sets, metric)
                if (series.isEmpty()) null else ExerciseProgress(exercise.exerciseId, exercise.name, series, series.last().first)
            }
            .sortedByDescending { it.lastSessionAt }
}

data class ExerciseRef(val exerciseId: Int, val name: String)
