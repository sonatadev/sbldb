package com.github.sonatadev.sbldb.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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

    /** Hash of the bundled YAML content last written to the database. */
    suspend fun contentHash(): String? = context.dataStore.data.first()[contentHashKey]

    suspend fun setContentHash(hash: String) {
        context.dataStore.edit { it[contentHashKey] = hash }
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
