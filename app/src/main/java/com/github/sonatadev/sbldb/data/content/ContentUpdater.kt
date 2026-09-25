package com.github.sonatadev.sbldb.data.content

import android.content.Context
import com.github.sonatadev.sbldb.data.AppDatabase
import com.github.sonatadev.sbldb.data.SeedData
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the library content current: starts from the best local copy (last GitHub download if it
 * is newer than the installed APK, else the APK's own copy), then checks GitHub and applies new content only after it passes validation.
 */
class ContentUpdater(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository
) {
    private val mutex = Mutex()

    suspend fun loadLocal() = mutex.withLock {
        ContentSource.dropCacheIfOlderThanApk(context)
        val cached = ContentSource.cached(context)?.takeIf { ContentValidator.check(it) is ContentValidator.Result.Valid }
        SeedData.sync(cached ?: ContentSource.bundled(context), db, settings)
        settings.setContentStatus(source = if (cached != null) SOURCE_GITHUB else SOURCE_APP, checkedAt = null, error = null)
    }

    suspend fun refresh() = mutex.withLock {
        val now = System.currentTimeMillis()
        when (val fetch = RemoteContent.fetch(context)) {
            is RemoteContent.Fetch.Unchanged -> settings.setContentStatus(SOURCE_GITHUB.takeIf { ContentSource.cached(context) != null }, now, null)
            is RemoteContent.Fetch.Failed -> settings.setContentStatus(null, now, "GitHub unreachable (${fetch.reason})")
            is RemoteContent.Fetch.Downloaded -> when (val check = ContentValidator.check(fetch.files)) {
                is ContentValidator.Result.Valid -> {
                    ContentSource.saveCache(context, fetch.files)
                    RemoteContent.commitEtags(context)
                    SeedData.sync(fetch.files, db, settings)
                    settings.setContentStatus(SOURCE_GITHUB, now, null)
                }
                is ContentValidator.Result.Invalid ->
                    settings.setContentStatus(null, now, "Update ignored: ${check.problems.first()}")
            }
        }
    }

    companion object {
        const val SOURCE_GITHUB = "GitHub"
        const val SOURCE_APP = "App"
    }
}
