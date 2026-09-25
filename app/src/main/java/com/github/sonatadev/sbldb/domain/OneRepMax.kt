package com.github.sonatadev.sbldb.domain

object OneRepMax {
    /** Above this many reps the estimate becomes unreliable, so no e1RM is computed. */
    const val MAX_REPS = 12

    /** Estimated one-rep max with the Epley formula, or null if the set is not suitable. */
    fun epley(weightKg: Double?, reps: Int?): Double? {
        if (weightKg == null || reps == null || weightKg <= 0 || reps !in 1..MAX_REPS) return null
        return if (reps == 1) weightKg else weightKg * (1 + reps / 30.0)
    }
}
