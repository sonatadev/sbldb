package com.github.sonatadev.sbldb

import com.github.sonatadev.sbldb.domain.AccentColor
import com.github.sonatadev.sbldb.domain.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppIconTest {
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun `every theme and accent has a launcher entry with its icon`() {
        val missing = ThemeMode.entries.flatMap { t -> AccentColor.entries.map { a -> AppIcon.aliasName(t, a) } }
            .filter { alias ->
                val name = alias.removePrefix("Launcher_")
                "android:name=\".$alias\"" !in manifest || !File("src/main/res/drawable-anydpi-v26/ic_launcher_$name.xml").exists()
            }
        assertTrue("Missing: $missing", missing.isEmpty())
    }

    @Test
    fun `exactly the default icon is enabled on install`() {
        val enabled = Regex("""android:name="\.(Launcher_\w+)"\s+android:enabled="true"""").findAll(manifest).map { it.groupValues[1] }.toList()
        assertEquals(listOf(AppIcon.aliasName(ThemeMode.SYSTEM, AccentColor.ORANGE)), enabled)
    }
}
