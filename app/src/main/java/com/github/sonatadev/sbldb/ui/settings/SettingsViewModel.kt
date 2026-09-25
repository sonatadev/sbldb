package com.github.sonatadev.sbldb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.github.sonatadev.sbldb.data.backup.BackupException
import com.github.sonatadev.sbldb.data.backup.BackupManager
import com.github.sonatadev.sbldb.data.backup.BackupSummary
import com.github.sonatadev.sbldb.data.content.ContentUpdater
import com.github.sonatadev.sbldb.data.repository.ContentStatus
import com.github.sonatadev.sbldb.data.repository.SettingsRepository
import com.github.sonatadev.sbldb.domain.AccentColor
import com.github.sonatadev.sbldb.domain.ExplanationLevel
import com.github.sonatadev.sbldb.domain.ThemeMode
import com.github.sonatadev.sbldb.domain.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.ui.UiText

data class SettingsUiState(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.ORANGE,
    val explanations: ExplanationLevel = ExplanationLevel.BASIC,
    val content: ContentStatus = ContentStatus(null, null, null),
    val checking: Boolean = false,
    val restSeconds: Int = 120,
    val backupFolder: String? = null,
    val lastBackup: Long? = null
)

/** A backup picked for import, waiting for the user to confirm. */
data class PendingRestore(val json: String, val summary: BackupSummary)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val contentUpdater: ContentUpdater,
    private val backups: BackupManager
) : ViewModel() {
    private val checking = MutableStateFlow(false)

    /** Short result of the last data action, shown under the buttons. */
    val dataMessage = MutableStateFlow<UiText?>(null)
    val pendingRestore = MutableStateFlow<PendingRestore?>(null)

    private val preferences = combine(settings.weightUnit, settings.themeMode, settings.accentColor, settings.explanationLevel) { unit, mode, accent, level ->
        SettingsUiState(unit, mode, accent, level)
    }

    val uiState: StateFlow<SettingsUiState> =
        combine(preferences, settings.contentStatus, checking, settings.backupStatus, settings.restSeconds) { prefs, content, busy, backup, rest ->
            prefs.copy(content = content, checking = busy, backupFolder = backup.first, lastBackup = backup.second, restSeconds = rest)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun exportJson(uri: Uri) = dataAction { backups.write(uri, backups.exportJson()); UiText.Res(R.string.msg_backup_saved) }

    fun exportCsv(uri: Uri) = dataAction { backups.write(uri, backups.exportCsv()); UiText.Res(R.string.msg_csv_saved) }

    fun inspect(uri: Uri) = dataAction {
        val json = backups.read(uri)
        pendingRestore.value = PendingRestore(json, backups.inspect(json).second)
        null
    }

    fun confirmRestore() {
        val pending = pendingRestore.value ?: return
        pendingRestore.value = null
        dataAction { backups.restore(pending.json); UiText.Res(R.string.msg_restored, listOf(pending.summary.workouts)) }
    }

    fun cancelRestore() {
        pendingRestore.value = null
    }

    fun setBackupFolder(uri: Uri?) = dataAction {
        settings.setBackupFolder(uri?.toString())
        UiText.Res(
            when {
                uri == null -> R.string.msg_backup_off
                backups.autoBackup() -> R.string.msg_backup_folder_ok
                else -> R.string.msg_backup_folder_failed
            }
        )
    }

    private fun dataAction(block: suspend () -> UiText?) {
        viewModelScope.launch {
            dataMessage.value = try {
                block()
            } catch (e: BackupException) {
                e.message?.let(UiText::Raw)
            } catch (e: Exception) {
                UiText.Res(R.string.msg_error, listOf(e.message.orEmpty()))
            }
        }
    }

    fun checkForUpdates() {
        if (checking.value) return
        viewModelScope.launch {
            checking.value = true
            try { contentUpdater.refresh() } finally { checking.value = false }
        }
    }

    fun setWeightUnit(unit: WeightUnit) = launch { settings.setWeightUnit(unit) }

    fun setThemeMode(mode: ThemeMode) = launch { settings.setThemeMode(mode) }

    fun setAccent(accent: AccentColor) = launch { settings.setAccentColor(accent) }

    fun setExplanations(level: ExplanationLevel) = launch { settings.setExplanationLevel(level) }

    fun setRestSeconds(seconds: Int) = launch { settings.setRestSeconds(seconds) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
