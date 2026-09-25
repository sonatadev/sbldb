package com.github.sonatadev.sbldb.domain

/** What a routine slot should prescribe, taken from sets actually done. */
data class SlotPlan(val sets: Int, val repMin: Int, val repMax: Int, val targetRir: Int?)

object RoutinePlan {
    /** A range narrower than this is widened around what was done (10, 10, 10 → 8–12). */
    private const val MIN_SPAN = 4

    /**
     * Turns the working sets of one exercise into a plan: same number of sets, the rep range that
     * was done (widened when too narrow) and the typical effort as the target RIR.
     */
    fun fromSets(sets: List<PastSet>): SlotPlan? {
        val done = sets.filter { (it.reps ?: 0) > 0 }
        if (done.isEmpty()) return null
        var low = done.minOf { it.reps!! }
        var high = done.maxOf { it.reps!! }
        if (high - low < MIN_SPAN) {
            val missing = MIN_SPAN - (high - low)
            low = (low - missing / 2).coerceAtLeast(1)
            high = low + MIN_SPAN
        }
        val rirs = done.mapNotNull { it.rir }.sorted()
        val rir = if (rirs.isEmpty()) null else rirs[(rirs.size - 1) / 2].coerceIn(0, 5)
        return SlotPlan(done.size, low, high, rir)
    }
}
