package com.github.sonatadev.sbldb.ui

import com.github.sonatadev.sbldb.domain.AccentColor
import com.github.sonatadev.sbldb.ui.theme.contrastRatio
import com.github.sonatadev.sbldb.ui.theme.sbldbColors
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentColorTest {
    private val cases = AccentColor.entries.flatMap { accent -> listOf(false, true).map { dark -> sbldbColors(dark, accent) to accent } }

    @Test
    fun `accent stands out from the ground and from modules`() {
        for ((c, accent) in cases) {
            val theme = if (c.isDark) "dark" else "light"
            assertTrue("$accent on $theme ground: ${contrastRatio(c.accent, c.ground)}", contrastRatio(c.accent, c.ground) >= 3f)
            assertTrue("$accent on $theme module: ${contrastRatio(c.accent, c.module)}", contrastRatio(c.accent, c.module) >= 3f)
        }
    }

    @Test
    fun `text on accent is readable`() {
        for ((c, accent) in cases) {
            assertTrue("$accent: ${contrastRatio(c.onAccent, c.accent)}", contrastRatio(c.onAccent, c.accent) >= 4.5f)
        }
    }
}
