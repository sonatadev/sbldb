package com.github.sonatadev.sbldb.data.importer

/** A library exercise as the matcher sees it. */
data class MatchCandidate(val exerciseId: Int, val name: String, val equipment: String, val aliases: List<String>)

data class Match(val exerciseId: Int, val name: String, val score: Double)

/**
 * Finds the library exercise for a name from another app, e.g. "Bench Press (Barbell)" →
 * "Barbell Bench Press". Names are compared as sets of words (equipment included), after
 * normalising plurals, abbreviations and the equipment's spelling.
 */
object ExerciseMatcher {
    /** From this score up the match is taken without asking... */
    const val SURE = 0.65

    /** ...as long as the runner-up is at least this far behind. */
    const val MARGIN = 0.1

    fun best(name: String, candidates: List<MatchCandidate>, limit: Int = 5): List<Match> {
        val query = tokens(name)
        if (query.isEmpty()) return emptyList()
        return candidates
            .map { c ->
                val score = (listOf(c.name) + c.aliases).maxOf { label ->
                    val words = tokens(label) + tokens(c.equipment)
                    if (tokens(label) == query) 1.0 else jaccard(query, words).coerceAtMost(0.99)
                }
                Match(c.exerciseId, c.name, score)
            }
            .filter { it.score > 0.3 }
            .sortedByDescending { it.score }
            .take(limit)
    }

    /** The match to use without asking, if there is one clear enough. */
    fun sure(name: String, candidates: List<MatchCandidate>): Match? {
        val (first, second) = best(name, candidates, 2).let { it.getOrNull(0) to it.getOrNull(1) }
        return first?.takeIf { it.score >= SURE && (second == null || first.score - second.score >= MARGIN) }
    }

    private fun jaccard(a: Set<String>, b: Set<String>): Double = a.intersect(b).size.toDouble() / a.union(b).size

    private val SYNONYMS = mapOf(
        "db" to "dumbbell", "dumbbells" to "dumbbell", "bb" to "barbell", "ez" to "ezbar", "bicep" to "biceps", "tricep" to "triceps",
        "pulldowns" to "pulldown", "pushdowns" to "pushdown", "pullups" to "pullup", "chinups" to "chinup",
        "pull" to "pull", "lats" to "lat", "flye" to "fly", "flyes" to "fly", "flies" to "fly", "raises" to "raise",
        "curls" to "curl", "rows" to "row", "presses" to "press", "extensions" to "extension", "squats" to "squat",
        "lunges" to "lunge", "deadlifts" to "deadlift", "dips" to "dip", "shrugs" to "shrug", "crunches" to "crunch",
        "machine" to "machine", "cable" to "cable", "bodyweight" to "bodyweight", "smith" to "smith", "weighted" to "bodyweight",
        "assisted" to "assisted", "seated" to "seated", "standing" to "standing", "incline" to "incline", "decline" to "decline"
    )

    /** Words that say nothing about the exercise. */
    private val STOP = setOf("the", "a", "with", "on", "and", "of", "to", "other")

    internal fun tokens(text: String): Set<String> =
        text.lowercase()
            .replace("e-z", "ez").replace("pull-up", "pullup").replace("pull up", "pullup").replace("chin-up", "chinup").replace("chin up", "chinup")
            .replace("push-up", "pushup").replace("push up", "pushup").replace("t-bar", "tbar").replace("ez-bar", "ezbar").replace("ez bar", "ezbar")
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotEmpty() && it !in STOP }
            .map { SYNONYMS[it] ?: it }
            .toSet()
}
