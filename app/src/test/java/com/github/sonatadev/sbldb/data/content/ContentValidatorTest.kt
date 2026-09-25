package com.github.sonatadev.sbldb.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Downloaded content is only applied when it passes these checks. */
class ContentValidatorTest {
    private fun bundled(overrides: Map<String, String> = emptyMap()) = ContentFiles(
        ContentSource.ALL_FILES.associateWith { name ->
            overrides[name]?.toByteArray() ?: File("src/main/assets/$name").readBytes()
        }
    )

    @Test
    fun `bundled content is valid and declares a supported format`() {
        val files = bundled()
        assertEquals(1, files.format)
        val result = ContentValidator.check(files)
        assertTrue(result.toString(), result is ContentValidator.Result.Valid)
    }

    @Test
    fun `an exercise pointing to an unknown action is rejected`() {
        val broken = File("src/main/assets/exercises.yaml").readText()
            .replaceFirst("Shoulder / Horizontal Adduction", "Shoulder / Made Up Action")
        val result = ContentValidator.check(bundled(mapOf("exercises.yaml" to broken)))
        assertTrue(result is ContentValidator.Result.Invalid)
        assertTrue((result as ContentValidator.Result.Invalid).problems.any { "Made Up Action" in it })
    }

    @Test
    fun `malformed yaml is rejected instead of crashing`() {
        val result = ContentValidator.check(bundled(mapOf("glossary.yaml" to "- term: Broken\n  basic: a: b")))
        assertTrue(result is ContentValidator.Result.Invalid)
    }

    @Test
    fun `content in a newer format is ignored`() {
        val result = ContentValidator.check(bundled(mapOf("content.json" to """{"format": 99}""")))
        assertTrue(result is ContentValidator.Result.Invalid)
    }

    @Test
    fun `the hash changes with the content`() {
        val edited = File("src/main/assets/glossary.yaml").readText() + "\n"
        assertTrue(bundled().hash != bundled(mapOf("glossary.yaml" to edited)).hash)
    }
}
