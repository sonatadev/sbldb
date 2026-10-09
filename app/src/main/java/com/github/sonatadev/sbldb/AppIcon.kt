package com.github.sonatadev.sbldb

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.github.sonatadev.sbldb.domain.AccentColor
import com.github.sonatadev.sbldb.domain.ThemeMode

/**
 * The launcher icon follows the app's theme: one activity-alias per accent × theme mode in the
 * manifest, and only the matching one enabled. With the system theme the icon's own colors follow
 * the phone's dark mode (values-night), so it needs no switching when the phone changes.
 */
object AppIcon {
    fun aliasName(theme: ThemeMode, accent: AccentColor) = "Launcher_${accent.name.lowercase()}_${theme.name.lowercase()}"

    /** Enables the alias for [theme] and [accent] and disables the others; does nothing if it's already the one. */
    fun apply(context: Context, theme: ThemeMode, accent: AccentColor) {
        val pm = context.packageManager
        val wanted = component(context, aliasName(theme, accent))
        if (pm.getComponentEnabledSetting(wanted) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return
        // Enable the new one first, so there is never a moment without a launcher entry
        pm.setComponentEnabledSetting(wanted, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        ThemeMode.entries.forEach { t ->
            AccentColor.entries.forEach { a ->
                val other = component(context, aliasName(t, a))
                if (other != wanted) pm.setComponentEnabledSetting(other, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
    }

    private fun component(context: Context, alias: String) = ComponentName(context.packageName, "${context.packageName}.$alias")
}
