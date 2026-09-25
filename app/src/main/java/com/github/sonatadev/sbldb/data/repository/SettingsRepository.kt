package com.github.sonatadev.sbldb.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.sonatadev.sbldb.domain.AccentColor
import com.github.sonatadev.sbldb.domain.ExplanationLevel
import com.github.sonatadev.sbldb.domain.ThemeMode
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val weightUnitKey = stringPreferencesKey("weight_unit")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val accentKey = stringPreferencesKey("accent_color")
    private val explanationKey = stringPreferencesKey("explanation_level")

    val weightUnit: Flow<WeightUnit> = enumFlow(weightUnitKey, WeightUnit.KG)
    val themeMode: Flow<ThemeMode> = enumFlow(themeModeKey, ThemeMode.SYSTEM)
    val accentColor: Flow<AccentColor> = enumFlow(accentKey, AccentColor.ORANGE)
    val explanationLevel: Flow<ExplanationLevel> = enumFlow(explanationKey, ExplanationLevel.BASIC)

    private val contentHashKey = stringPreferencesKey("content_hash")
    private val contentSourceKey = stringPreferencesKey("content_source")
    private val contentCheckedKey = longPreferencesKey("content_checked_at")
    private val contentErrorKey = stringPreferencesKey("content_error")

    /** Where the library content in use came from, when it was last checked, and the last problem. */
    val contentStatus: Flow<ContentStatus> = context.dataStore.data.map { prefs ->
        ContentStatus(prefs[contentSourceKey], prefs[contentCheckedKey], prefs[contentErrorKey])
    }.distinctUntilChanged()

    suspend fun setContentStatus(source: String?, checkedAt: Long?, error: String?) {
        context.dataStore.edit { prefs ->
            source?.let { prefs[contentSourceKey] = it }
            checkedAt?.let { prefs[contentCheckedKey] = it }
            if (error == null) prefs.remove(contentErrorKey) else prefs[contentErrorKey] = error
        }
    }

    /** Hash of the bundled YAML content last written to the database. */
    suspend fun contentHash(): String? = context.dataStore.data.first()[contentHashKey]

    suspend fun setContentHash(hash: String) {
        context.dataStore.edit { it[contentHashKey] = hash }
    }

    private val backupFolderKey = stringPreferencesKey("backup_folder")
    private val lastBackupKey = longPreferencesKey("last_backup_at")
    private val restSecondsKey = intPreferencesKey("rest_seconds")

    val backupStatus: Flow<Pair<String?, Long?>> = context.dataStore.data.map { it[backupFolderKey] to it[lastBackupKey] }.distinctUntilChanged()

    /** Default rest between sets when the routine does not set one. */
    val restSeconds: Flow<Int> = context.dataStore.data.map { it[restSecondsKey] ?: 120 }.distinctUntilChanged()

    suspend fun backupFolder(): String? = context.dataStore.data.first()[backupFolderKey]

    suspend fun setBackupFolder(uri: String?) {
        context.dataStore.edit { if (uri == null) it.remove(backupFolderKey) else it[backupFolderKey] = uri }
    }

    suspend fun setLastBackup(millis: Long) {
        context.dataStore.edit { it[lastBackupKey] = millis }
    }

    suspend fun setRestSeconds(seconds: Int) {
        context.dataStore.edit { it[restSecondsKey] = seconds.coerceIn(15, 600) }
    }

    /** User preferences for backups (never device-specific values like the backup folder). */
    suspend fun snapshot(): org.json.JSONObject {
        val prefs = context.dataStore.data.first()
        return org.json.JSONObject().apply {
            listOf(weightUnitKey, themeModeKey, accentKey, explanationKey).forEach { key -> prefs[key]?.let { put(key.name, it) } }
            prefs[restSecondsKey]?.let { put(restSecondsKey.name, it) }
        }
    }

    suspend fun restore(json: org.json.JSONObject) {
        context.dataStore.edit { prefs ->
            listOf(weightUnitKey, themeModeKey, accentKey, explanationKey).forEach { key ->
                if (json.has(key.name)) prefs[key] = json.getString(key.name)
            }
            if (json.has(restSecondsKey.name)) prefs[restSecondsKey] = json.getInt(restSecondsKey.name)
        }
    }

    suspend fun setWeightUnit(unit: WeightUnit) = set(weightUnitKey, unit)

    suspend fun setThemeMode(mode: ThemeMode) = set(themeModeKey, mode)

    suspend fun setAccentColor(accent: AccentColor) = set(accentKey, accent)

    suspend fun setExplanationLevel(level: ExplanationLevel) = set(explanationKey, level)

    private inline fun <reified E : Enum<E>> enumFlow(key: Preferences.Key<String>, default: E): Flow<E> =
        context.dataStore.data
            .map { prefs -> prefs[key]?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default }
            .distinctUntilChanged()

    private suspend fun set(key: Preferences.Key<String>, value: Enum<*>) {
        context.dataStore.edit { it[key] = value.name }
    }
}

data class ContentStatus(val source: String?, val checkedAt: Long?, val error: String?)
