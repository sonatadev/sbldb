package com.github.sonatadev.sbldb.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.github.sonatadev.sbldb.domain.ExplanationLevel

val LocalExplanationLevel = staticCompositionLocalOf { ExplanationLevel.BASIC }

/** Picks the text matching the chosen explanation level, falling back to the other one. */
@Composable
@ReadOnlyComposable
fun explained(basic: String?, expert: String?): String? =
    if (LocalExplanationLevel.current == ExplanationLevel.EXPERT) expert ?: basic else basic ?: expert

@Composable
@ReadOnlyComposable
fun isExpert(): Boolean = LocalExplanationLevel.current == ExplanationLevel.EXPERT
