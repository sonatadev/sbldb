package com.github.sonatadev.sbldb.data.content

import android.content.Context
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** One complete set of the library's content files, from the APK or from GitHub. */
class ContentFiles(private val files: Map<String, ByteArray>) {
    fun open(name: String): InputStream = requireNotNull(files[name]) { "Missing content file $name" }.inputStream()

    /** Content format declared in content.json; the app only reads formats it knows. */
    val format: Int
        get() = files[ContentSource.MANIFEST]
            ?.let { Regex(""""format"\s*:\s*(\d+)""").find(String(it))?.groupValues?.get(1)?.toIntOrNull() } ?: 0

    /** Hash of the data files, used to skip database writes when nothing changed. */
    val hash: String
        get() {
            val digest = MessageDigest.getInstance("SHA-256")
            ContentSource.DATA_FILES.forEach { digest.update(requireNotNull(files[it])) }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

    fun bytes(name: String): ByteArray? = files[name]
}

object ContentSource {
    const val MANIFEST = "content.json"
    val DATA_FILES = listOf("muscles.yaml", "joint_actions.yaml", "exercises.yaml", "glossary.yaml")
    val ALL_FILES = listOf(MANIFEST) + DATA_FILES

    /** Highest content format this version of the app understands. */
    const val SUPPORTED_FORMAT = 1

    /** The public repository is the source of truth; the APK ships a copy for offline use. */
    const val REMOTE_BASE = "https://raw.githubusercontent.com/sonatadev/sbldb/main/app/src/main/assets/"

    fun bundled(context: Context): ContentFiles =
        ContentFiles(ALL_FILES.associateWith { name -> context.assets.open(name).use { it.readBytes() } })

    private fun cacheDir(context: Context) = File(context.filesDir, "content")

    /** Last content downloaded from GitHub and validated, if any. */
    fun cached(context: Context): ContentFiles? {
        val dir = cacheDir(context)
        val files = ALL_FILES.associateWith { File(dir, it) }
        if (files.values.any { !it.exists() }) return null
        return ContentFiles(files.mapValues { it.value.readBytes() })
    }

    fun saveCache(context: Context, content: ContentFiles) {
        val dir = cacheDir(context).apply { mkdirs() }
        ALL_FILES.forEach { name ->
            val tmp = File(dir, "$name.tmp")
            tmp.writeBytes(requireNotNull(content.bytes(name)))
            tmp.renameTo(File(dir, name))
        }
    }
}
