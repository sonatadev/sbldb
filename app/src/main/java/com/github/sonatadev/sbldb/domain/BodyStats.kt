package com.github.sonatadev.sbldb.domain

/** A body-weight reading on an epoch day. */
data class WeightPoint(val day: Long, val kg: Double)

/** Weekly change in body weight, from a least-squares line through recent readings. */
data class WeightRate(val kgPerWeek: Double, val percentPerWeek: Double)

object BodyStats {
    /** Readings older than this many days before the latest one are ignored for the rate. */
    const val RATE_WINDOW_DAYS = 28
    /** Needs readings spread over at least this many days to say anything. */
    const val MIN_SPAN_DAYS = 7

    fun rate(points: List<WeightPoint>): WeightRate? {
        if (points.isEmpty()) return null
        val latest = points.maxOf { it.day }
        val recent = points.filter { it.day >= latest - RATE_WINDOW_DAYS }
        if (recent.size < 3 || latest - recent.minOf { it.day } < MIN_SPAN_DAYS) return null
        val meanX = recent.map { it.day.toDouble() }.average()
        val meanY = recent.map { it.kg }.average()
        val sxx = recent.sumOf { (it.day - meanX) * (it.day - meanX) }
        if (sxx == 0.0) return null
        val slopePerDay = recent.sumOf { (it.day - meanX) * (it.kg - meanY) } / sxx
        val perWeek = slopePerDay * 7
        return WeightRate(perWeek, perWeek / meanY * 100)
    }

    /** Average of each reading and the ones in the 6 days before it, to smooth daily water swings. */
    fun rollingAverage(points: List<WeightPoint>, days: Int = 7): List<WeightPoint> {
        val sorted = points.sortedBy { it.day }
        return sorted.map { p ->
            val window = sorted.filter { it.day in (p.day - days + 1)..p.day }
            WeightPoint(p.day, window.map { it.kg }.average())
        }
    }
}
