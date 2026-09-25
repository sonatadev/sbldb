package com.github.sonatadev.sbldb.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object CalendarGrid {
    /** The month as Monday-first weeks of 7 cells; cells outside the month are null. */
    fun weeks(month: YearMonth): List<List<LocalDate?>> {
        val leading = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
        val cells = List<LocalDate?>(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        val padded = cells + List((7 - cells.size % 7) % 7) { null }
        return padded.chunked(7)
    }
}
