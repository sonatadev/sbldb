package com.github.sonatadev.sbldb

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sonatadev.sbldb.domain.AccentColor
import com.github.sonatadev.sbldb.domain.ExplanationLevel
import com.github.sonatadev.sbldb.domain.ThemeMode
import com.github.sonatadev.sbldb.ui.LocalExplanationLevel
import com.github.sonatadev.sbldb.ui.navigation.SbldbApp
import com.github.sonatadev.sbldb.ui.theme.SbldbTheme

class MainActivity : ComponentActivity() {
    /** Set when the app is opened from the workout notification. */
    private val openWorkoutRequests = kotlinx.coroutines.flow.MutableStateFlow(0)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_WORKOUT, false)) openWorkoutRequests.value++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.getBooleanExtra(EXTRA_OPEN_WORKOUT, false) == true) openWorkoutRequests.value++
        val settings = (application as SbldbApplication).container.settingsRepository
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val accent by settings.accentColor.collectAsStateWithLifecycle(AccentColor.ORANGE)
            val explanations by settings.explanationLevel.collectAsStateWithLifecycle(ExplanationLevel.BASIC)
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Status and navigation bar icons follow the app theme, not only the system one
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            SbldbTheme(themeMode = themeMode, accent = accent) {
                CompositionLocalProvider(LocalExplanationLevel provides explanations) {
                    val openWorkout by openWorkoutRequests.collectAsStateWithLifecycle()
                    SbldbApp(openWorkoutRequest = openWorkout)
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_WORKOUT = "open_workout"
    }
}
