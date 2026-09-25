package com.github.sonatadev.sbldb.domain

/**
 * Ranks replacement exercises by how closely they load the same joint actions.
 * Ratings are 1–5 per joint action; missing actions count as 0.
 */
object ExerciseSwap {
    /** Shared loading minus half the mismatch, so a close twin beats a broader exercise. */
    fun similarity(current: Map<Int, Int>, candidate: Map<Int, Int>): Double {
        val actions = current.keys + candidate.keys
        val shared = actions.sumOf { minOf(current[it] ?: 0, candidate[it] ?: 0) }
        val mismatch = actions.sumOf { kotlin.math.abs((current[it] ?: 0) - (candidate[it] ?: 0)) }
        return shared - mismatch / 2.0
    }

    /**
     * Best replacements for [currentId], most similar first. Candidates must train at least one of the
     * current exercise's main actions (rated 4+, or its best-rated one) at 3 or more.
     */
    fun rank(currentId: Int, ratings: Map<Int, Map<Int, Int>>, limit: Int = 10): List<Int> {
        val current = ratings[currentId] ?: return emptyList()
        if (current.isEmpty()) return emptyList()
        val best = current.values.max()
        val main = current.filterValues { it >= 4 || it == best }.keys
        return ratings
            .filterKeys { it != currentId }
            .filterValues { candidate -> main.any { (candidate[it] ?: 0) >= 3 } }
            .map { (id, candidate) -> id to similarity(current, candidate) }
            .sortedWith(compareByDescending<Pair<Int, Double>> { it.second }.thenBy { it.first })
            .take(limit)
            .map { it.first }
    }
}
