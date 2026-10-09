package com.github.sonatadev.sbldb.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.github.sonatadev.sbldb.MainActivity
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.SbldbApplication
import com.github.sonatadev.sbldb.domain.AccentColor
import com.github.sonatadev.sbldb.domain.NextRoutine
import com.github.sonatadev.sbldb.domain.ThemeMode
import com.github.sonatadev.sbldb.domain.WeekRange
import com.github.sonatadev.sbldb.ui.theme.color
import com.github.sonatadev.sbldb.ui.theme.readableOn
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle as DayStyle
import java.util.Locale

/** What the widget shows: today's week as dots, and the routine up next (or the workout in progress). */
private data class WidgetData(
    val week: List<Pair<String, Boolean>>,
    val todayIndex: Int,
    val sessions: Int,
    val nextId: Long?,
    val nextName: String?,
    val nextDetail: String?,
    val inProgress: String?,
    val theme: ThemeMode,
    val accent: AccentColor
)

/** Home-screen widget: the week at a glance and one tap to start the next routine. */
class SbldbWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent { Content(context, data) }
    }

    private suspend fun load(context: Context): WidgetData {
        val container = (context.applicationContext as SbldbApplication).container
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val week = WeekRange.of(0)
        val history = container.workoutRepository.history.first()
        val routines = container.routineRepository.routines.first()
        val trained = history.filter { it.workout.startedAt in week.startMillis until week.endMillis }
            .map { Instant.ofEpochMilli(it.workout.startedAt).atZone(zone).toLocalDate() }.toSet()
        val next = NextRoutine.pick(routines, history, container.routineRepository.plannedBetween(today, today).first(), today, zone)
        val active = container.workoutRepository.activeWorkout.first()
        val sets = next?.routine?.exercises?.sumOf { it.routineExercise.sets } ?: 0
        return WidgetData(
            week = DayOfWeek.entries.map { d ->
                d.getDisplayName(DayStyle.NARROW, Locale.getDefault()).uppercase() to (week.start.plusDays(d.ordinal.toLong()) in trained)
            },
            todayIndex = today.dayOfWeek.ordinal,
            sessions = trained.size,
            nextId = next?.routine?.routine?.routineId,
            nextName = next?.routine?.routine?.name,
            nextDetail = next?.let { context.getString(R.string.widget_detail, it.routine.exercises.size, sets) },
            inProgress = active?.name,
            theme = container.settingsRepository.themeMode.first(),
            accent = container.settingsRepository.accentColor.first()
        )
    }

    @Composable
    private fun Content(context: Context, data: WidgetData) {
        // The app's theme: fixed light or dark, or following the phone
        fun pick(light: Color, dark: Color): ColorProvider = when (data.theme) {
            ThemeMode.LIGHT -> ColorProvider(light)
            ThemeMode.DARK -> ColorProvider(dark)
            ThemeMode.SYSTEM -> ColorProvider(day = light, night = dark)
        }
        val ground = pick(Color(0xFFFFFFFF), Color(0xFF0D0D0D))
        val ink = pick(Color(0xFF0A0A0A), Color(0xFFFFFFFF))
        val muted = pick(Color(0xFF6B6B6B), Color(0xFF8A8A8A))
        val empty = pick(Color(0xFFD6D6D1), Color(0xFF2A2A2A))
        val accent = pick(data.accent.color(false), data.accent.color(true))
        val onAccent = pick(readableOn(data.accent.color(false)), readableOn(data.accent.color(true)))
        val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = muted)

        val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val start = when {
            data.inProgress != null -> Intent(open).putExtra(MainActivity.EXTRA_OPEN_WORKOUT, true)
            data.nextId != null -> Intent(open).putExtra(MainActivity.EXTRA_START_ROUTINE, data.nextId)
            else -> open
        }

        Column(
            GlanceModifier.fillMaxSize().background(ground).cornerRadius(22.dp).padding(14.dp).clickable(actionStartActivity(open))
        ) {
            Text(context.getString(R.string.widget_week, data.sessions).uppercase(), style = mono)
            Spacer(GlanceModifier.height(8.dp))
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                data.week.forEachIndexed { i, (letter, trained) ->
                    Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(letter, style = mono.copy(color = if (i == data.todayIndex) accent else muted))
                        Spacer(GlanceModifier.height(4.dp))
                        Image(
                            ImageProvider(if (trained || i != data.todayIndex) R.drawable.widget_dot else R.drawable.widget_ring),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (trained || i == data.todayIndex) accent else empty),
                            modifier = GlanceModifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Text(
                        (if (data.inProgress != null) context.getString(R.string.widget_in_progress) else context.getString(R.string.widget_next)).uppercase(),
                        style = mono
                    )
                    Text(
                        data.inProgress ?: data.nextName ?: context.getString(R.string.widget_no_routine),
                        style = TextStyle(color = ink, fontSize = 18.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                    if (data.inProgress == null) data.nextDetail?.let { Text(it.uppercase(), style = mono, maxLines = 1) }
                }
                Spacer(GlanceModifier.width(10.dp))
                Box(
                    GlanceModifier.background(accent).cornerRadius(20.dp).padding(horizontal = 16.dp, vertical = 10.dp).clickable(actionStartActivity(start)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        context.getString(if (data.inProgress != null) R.string.widget_resume else R.string.widget_start),
                        style = TextStyle(color = onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    companion object {
        /** Redraws every widget, after a workout or a routine changed. */
        suspend fun refresh(context: Context) = runCatching { SbldbWidget().updateAll(context) }
    }
}

class SbldbWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SbldbWidget()
}
