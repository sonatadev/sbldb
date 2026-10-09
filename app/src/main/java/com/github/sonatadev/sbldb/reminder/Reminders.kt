package com.github.sonatadev.sbldb.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.github.sonatadev.sbldb.MainActivity
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.SbldbApplication
import com.github.sonatadev.sbldb.data.repository.Reminder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Reminders for routines planned in the calendar: once a day at the chosen time, if a routine is
 * planned for today and hasn't been done, a notification offers to start it.
 * The alarm is inexact (no special permission) and set again after each firing and after a reboot.
 */
object Reminders {
    private const val CHANNEL = "reminders"
    private const val NOTIFICATION_ID = 3
    private const val REQUEST = 41

    /** The next time the reminder rings after [now]. */
    fun nextAt(minuteOfDay: Int, now: ZonedDateTime): ZonedDateTime {
        val today = now.toLocalDate().atStartOfDay(now.zone).plusMinutes(minuteOfDay.toLong())
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** Sets (or clears) the alarm to match [reminder]. */
    fun schedule(context: Context, reminder: Reminder) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarms.cancel(pending)
        if (!reminder.enabled) return
        val at = nextAt(reminder.minuteOfDay, ZonedDateTime.now(ZoneId.systemDefault())).toInstant().toEpochMilli()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /** Posts today's reminder if a planned routine is still to do. */
    suspend fun notifyIfPlanned(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val container = (context.applicationContext as SbldbApplication).container
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val planned = container.routineRepository.plannedBetween(today, today).first()
        if (planned.isEmpty()) return
        val doneToday = container.workoutRepository.history.first()
            .filter { Instant.ofEpochMilli(it.workout.startedAt).atZone(zone).toLocalDate() == today }
            .mapNotNull { it.workout.routineId }.toSet()
        val routine = planned.firstOrNull { it.routineId !in doneToday } ?: return
        // Already training: no need to remind
        if (container.workoutRepository.activeWorkout.first() != null) return

        createChannel(context)
        val start = PendingIntent.getActivity(
            context, REQUEST,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_START_ROUTINE, routine.routineId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val open = PendingIntent.getActivity(
            context, REQUEST + 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.reminder_title, routine.name))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.widget_start), start)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }
}

/** Rings at the reminder time, and after a reboot or an app update, to set the alarm again. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = (context.applicationContext as SbldbApplication).container
        Thread {
            try {
                runBlocking {
                    val reminder = container.settingsRepository.reminder.first()
                    val action = intent.action
                    if (action == null && reminder.enabled) Reminders.notifyIfPlanned(context)
                    Reminders.schedule(context, reminder)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
