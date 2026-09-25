package com.github.sonatadev.sbldb.data.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties

/** Downloads the content files from GitHub, using ETags so unchanged files cost almost nothing. */
object RemoteContent {

    sealed interface Fetch {
        data object Unchanged : Fetch
        data class Downloaded(val files: ContentFiles) : Fetch
        data class Failed(val reason: String) : Fetch
    }

    suspend fun fetch(context: Context): Fetch = withContext(Dispatchers.IO) {
        val cached = ContentSource.cached(context)
        val etags = loadEtags(context).takeIf { cached != null } ?: Properties()
        val newEtags = Properties()
        val result = LinkedHashMap<String, ByteArray>()
        var changed = cached == null
        try {
            for (name in ContentSource.ALL_FILES) {
                val connection = (URL(ContentSource.REMOTE_BASE + name).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    etags.getProperty(name)?.let { setRequestProperty("If-None-Match", it) }
                }
                try {
                    when (connection.responseCode) {
                        HttpURLConnection.HTTP_NOT_MODIFIED -> result[name] = requireNotNull(cached?.bytes(name))
                        HttpURLConnection.HTTP_OK -> {
                            result[name] = connection.inputStream.use { it.readBytes() }
                            changed = true
                        }
                        else -> return@withContext Fetch.Failed("HTTP ${connection.responseCode} for $name")
                    }
                    (connection.getHeaderField("ETag") ?: etags.getProperty(name))?.let { newEtags.setProperty(name, it) }
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: Exception) {
            return@withContext Fetch.Failed(e.message ?: e.javaClass.simpleName)
        }
        pendingEtags = newEtags
        if (changed) Fetch.Downloaded(ContentFiles(result)) else Fetch.Unchanged
    }

    /** ETags are only stored once the downloaded files have been validated and cached. */
    private var pendingEtags: Properties? = null

    fun commitEtags(context: Context) {
        pendingEtags?.let { props -> etagFile(context).apply { parentFile?.mkdirs() }.outputStream().use { props.store(it, null) } }
    }

    private fun etagFile(context: Context) = File(context.filesDir, "content/etags.properties")

    private fun loadEtags(context: Context) = Properties().apply {
        etagFile(context).takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
}
