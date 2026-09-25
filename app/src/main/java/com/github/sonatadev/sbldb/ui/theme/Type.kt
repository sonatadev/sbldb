package com.github.sonatadev.sbldb.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.github.sonatadev.sbldb.R

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold)
)

val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium)
)

/** Dot-matrix display face, for hero numbers only (timer, week, e1RM). */
val Doto = FontFamily(Font(R.font.doto_extrabold, FontWeight.ExtraBold))

private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Geist),
    displayMedium = base.displayMedium.copy(fontFamily = Geist),
    displaySmall = base.displaySmall.copy(fontFamily = Geist),
    headlineLarge = base.headlineLarge.copy(fontFamily = Geist, fontWeight = FontWeight.Medium, letterSpacing = (-0.02).em),
    headlineMedium = base.headlineMedium.copy(fontFamily = Geist, fontWeight = FontWeight.Medium, letterSpacing = (-0.02).em),
    headlineSmall = base.headlineSmall.copy(fontFamily = Geist, fontWeight = FontWeight.Medium, letterSpacing = (-0.02).em),
    titleLarge = base.titleLarge.copy(fontFamily = Geist, fontWeight = FontWeight.Medium, letterSpacing = (-0.01).em),
    titleMedium = base.titleMedium.copy(fontFamily = Geist, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = Geist, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = Geist, letterSpacing = 0.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = Geist, letterSpacing = 0.sp),
    bodySmall = base.bodySmall.copy(fontFamily = Geist, letterSpacing = 0.sp),
    labelLarge = base.labelLarge.copy(fontFamily = Geist),
    labelMedium = base.labelMedium.copy(fontFamily = GeistMono),
    labelSmall = base.labelSmall.copy(fontFamily = GeistMono)
)

/** Text styles outside the Material scale. */
object SbldbType {
    fun hero(size: Int) = TextStyle(fontFamily = Doto, fontWeight = FontWeight.ExtraBold, fontSize = size.sp, lineHeight = (size * 0.95).sp)

    /** Module labels and metadata: "03 · NOW". Callers pass uppercase text. */
    val label = TextStyle(fontFamily = GeistMono, fontSize = 10.sp, letterSpacing = 0.12.em, lineHeight = 14.sp)

    val mono = TextStyle(fontFamily = GeistMono, fontSize = 12.sp, letterSpacing = 0.04.em, lineHeight = 16.sp)

    val monoLarge = TextStyle(fontFamily = GeistMono, fontSize = 15.sp, lineHeight = 20.sp)
}
