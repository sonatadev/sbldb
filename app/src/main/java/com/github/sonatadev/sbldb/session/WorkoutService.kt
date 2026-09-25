package com.github.sonatadev.sbldb.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.github.sonatadev.sbldb.MainActivity
import com.github.sonatadev.sbldb.R
import com.github.sonatadev.sbldb.SbldbApplication
import com.github.sonatadev.sbldb.data.entity.Workout
import com.github.sonatadev.sbldb.domain.RestTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Keeps the workout in progress visible on the lock screen and makes the end of a rest audible
 * even when the app is in the background. Runs only while a workout is open.
 */
class WorkoutService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var restAlarm: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels(this)
        val container = (application as SbldbApplication).container
        ServiceCompat.startForeground(
            this, ONGOING_ID, buildOngoing(null, RestTimer.Idle, null),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        scope.launch {
            combine(container.workoutRepository.activeWorkout, container.workoutSession.rest, container.workoutSession.nextUp) { w, r, n -> Triple(w, r, n) }
                .collect { (workout, rest, next) ->
                    if (workout == null) {
                        stopSelf()
                        return@collect
                    }
                    getSystemService(NotificationManager::class.java).notify(ONGOING_ID, buildOngoing(workout, rest, next))
                    scheduleRestAlarm(rest, next)
                }
        }
    }

    private fun scheduleRestAlarm(rest: RestTimer, next: String?) {
        restAlarm?.cancel()
        val end = rest.endsAt ?: return
        restAlarm = scope.launch {
            delay((end - System.currentTimeMillis()).coerceAtLeast(0))
            vibrate()
            val alert = NotificationCompat.Builder(this@WorkoutService, CHANNEL_REST)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.rest_over))
                .setContentText(next ?: getString(R.string.rest_over_hint))
                .setContentIntent(openApp())
                .setAutoCancel(true)
                .setTimeoutAfter(60_000)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            getSystemService(NotificationManager::class.java).notify(REST_ID, alert)
            (application as SbldbApplication).container.workoutSession.skipRest()
        }
    }

    private fun buildOngoing(workout: Workout?, rest: RestTimer, next: String?): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_WORKOUT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setContentIntent(openApp())
            .setContentTitle(workout?.name ?: getString(R.string.app_name))
        if (rest.isRunning) {
            // Native countdown: stays accurate without waking the app every second
            builder.setUsesChronometer(true).setChronometerCountDown(true).setWhen(rest.endsAt!!)
                .setContentText(getString(R.string.resting) + (next?.let { " · $it" } ?: ""))
        } else if (workout != null) {
            builder.setUsesChronometer(true).setWhen(workout.startedAt).setContentText(getString(R.string.workout_in_progress))
        }
        return builder.build()
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(MainActivity.EXTRA_OPEN_WORKOUT, true),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
        val pattern = longArrayOf(0, 300, 150, 300)
        if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        else @Suppress("DEPRECATION") vibrator.vibrate(pattern, -1)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_WORKOUT = "workout"
        private const val CHANNEL_REST = "rest"
        private const val ONGOING_ID = 1
        private const val REST_ID = 2

        fun createChannels(context: Context) {
            // Channels exist from Android 8; older versions use the builder's own priority
            if (Build.VERSION.SDK_INT < 26) return
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_WORKOUT, context.getString(R.string.channel_workout), NotificationManager.IMPORTANCE_LOW)
            )
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_REST, context.getString(R.string.channel_rest), NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(true)
                }
            )
        }

        /** Starts the service; ignored when Android does not allow it (e.g. app in background). */
        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, WorkoutService::class.java)) }
        }
    }
}
