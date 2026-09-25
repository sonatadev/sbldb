package com.github.sonatadev.sbldb.domain

/**
 * Rest between sets. Immutable: every action returns a new timer, so it is trivial to test and to
 * share between the screen and the workout notification.
 */
data class RestTimer(
    /** Wall-clock time the rest ends, or null when not resting. */
    val endsAt: Long? = null,
    /** Length of the current rest, for progress bars. */
    val totalSeconds: Int = 0
) {
    val isRunning: Boolean get() = endsAt != null

    fun remainingSeconds(now: Long): Int = endsAt?.let { ((it - now + 999) / 1000).toInt().coerceAtLeast(0) } ?: 0

    fun isFinished(now: Long): Boolean = endsAt != null && now >= endsAt

    /** 1 at the start of the rest, 0 when it is over. */
    fun progress(now: Long): Float =
        if (endsAt == null || totalSeconds == 0) 0f else (remainingSeconds(now).toFloat() / totalSeconds).coerceIn(0f, 1f)

    fun adjust(deltaSeconds: Int, now: Long): RestTimer {
        val end = endsAt ?: return this
        val newEnd = (end + deltaSeconds * 1000L).coerceAtLeast(now)
        return copy(endsAt = newEnd, totalSeconds = (totalSeconds + deltaSeconds).coerceAtLeast(1))
    }

    companion object {
        fun start(seconds: Int, now: Long) = RestTimer(now + seconds * 1000L, seconds)
        val Idle = RestTimer()
    }
}
