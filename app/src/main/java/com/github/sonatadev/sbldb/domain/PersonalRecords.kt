package com.github.sonatadev.sbldb.domain

/** Personal bests for one exercise. Weights are in kg. */
data class Records(
    val bestE1rm: Double? = null,
    val heaviest: Double? = null,
    /** Load → most reps ever done with it. */
    val repsAtWeight: Map<Double, Int> = emptyMap()
) {
    val isEmpty: Boolean get() = heaviest == null && repsAtWeight.isEmpty()
}

enum class PrKind { E1RM, WEIGHT, REPS }

object PersonalRecords {
    fun of(sets: List<PastSet>): Records = sets.fold(Records()) { records, set -> add(records, set) }

    /** [records] updated with [set]. */
    fun add(records: Records, set: PastSet): Records {
        val reps = set.reps ?: return records
        if (reps <= 0) return records
        val weight = set.weightKg ?: 0.0
        val e1rm = OneRepMax.epley(set.weightKg, set.reps)
        return Records(
            bestE1rm = listOfNotNull(records.bestE1rm, e1rm).maxOrNull(),
            heaviest = if (weight > 0) maxOf(records.heaviest ?: 0.0, weight) else records.heaviest,
            repsAtWeight = records.repsAtWeight + (weight to maxOf(records.repsAtWeight[weight] ?: 0, reps))
        )
    }

    /**
     * Which records [set] beats. A first-ever set beats nothing: there is no record to break yet.
     * A rep record needs the same load done before.
     */
    fun beaten(set: PastSet, before: Records): Set<PrKind> {
        val reps = set.reps ?: return emptySet()
        if (reps <= 0 || before.isEmpty) return emptySet()
        val weight = set.weightKg ?: 0.0
        val kinds = mutableSetOf<PrKind>()
        val e1rm = OneRepMax.epley(set.weightKg, set.reps)
        if (e1rm != null && before.bestE1rm != null && e1rm > before.bestE1rm + 1e-9) kinds += PrKind.E1RM
        if (weight > 0 && before.heaviest != null && weight > before.heaviest + 1e-9) kinds += PrKind.WEIGHT
        before.repsAtWeight[weight]?.let { if (reps > it) kinds += PrKind.REPS }
        return kinds
    }

    /** For each set in order, the records it beats against history plus the earlier sets of the session. */
    fun beatenInSession(history: Records, session: List<PastSet>): List<Set<PrKind>> {
        var running = history
        return session.map { set -> beaten(set, running).also { running = add(running, set) } }
    }
}
