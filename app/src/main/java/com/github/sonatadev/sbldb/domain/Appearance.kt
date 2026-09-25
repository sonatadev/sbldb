package com.github.sonatadev.sbldb.domain

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Accent choices offered in Settings; the actual colors live in the UI theme. */
enum class AccentColor(val label: String) {
    ORANGE("Orange"),
    RED("Red"),
    YELLOW("Yellow"),
    GREEN("Green"),
    CYAN("Cyan"),
    BLUE("Blue"),
    VIOLET("Violet")
}

/** Which version of the explanations to show. */
enum class ExplanationLevel { BASIC, EXPERT }
