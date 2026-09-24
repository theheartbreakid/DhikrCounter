package com.Crescent.DhikrCounter.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.MainActivity
import com.Crescent.DhikrCounter.R
import com.Crescent.DhikrCounter.core.update.model.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpdateReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? DhikrApplication ?: return
        val settingsManager = app.settingsManager
        val updateManager = app.updateManager

        // Respect frequency: if Never, don't check
        val frequency = settingsManager.updateFrequency
        if (frequency.equals(SettingsManager.UPDATE_FREQ_NEVER, ignoreCase = true)) {
            return
        }

        // Avoid checking if checked too recently
        val lastCheck = settingsManager.lastUpdateCheckTimestamp
        val now = System.currentTimeMillis()
        val minIntervalMs = when (frequency) {
            SettingsManager.UPDATE_FREQ_DAILY -> 20 * 3600 * 1000L // 20 hours leeway
            SettingsManager.UPDATE_FREQ_WEEKLY -> 6 * 24 * 3600 * 1000L // 6 days
            SettingsManager.UPDATE_FREQ_MONTHLY -> 27 * 24 * 3600 * 1000L // 27 days
            else -> 20 * 3600 * 1000L
        }

        if (now - lastCheck < minIntervalMs) {
            // Re-schedule next trigger
            UpdateScheduler.scheduleNextCheck(context, frequency)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = updateManager.checkForUpdates(isManual = false)
                if (state is UpdateState.UpdateAvailable) {
                    val info = state.info
                    val lastNotifiedId = settingsManager.notifiedUpdateId
                    if (info.releaseId != lastNotifiedId) {
                        settingsManager.notifiedUpdateId = info.releaseId
                        showUpdateNotification(context, info.remoteVersionName)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                UpdateScheduler.scheduleNextCheck(context, frequency)
            }
        }
    }

    private fun showUpdateNotification(context: Context, versionName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new Dhikr Counter releases"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_update_check", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Dhikr Counter update available")
            .setContentText("Version $versionName is ready to download.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "dhikr_app_updates"
        private const val NOTIFICATION_ID = 2002
    }
}

object UpdateScheduler {
    fun scheduleNextCheck(context: Context, frequency: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, UpdateReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (frequency.equals(SettingsManager.UPDATE_FREQ_NEVER, ignoreCase = true)) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val intervalMillis = when (frequency) {
            SettingsManager.UPDATE_FREQ_DAILY -> AlarmManager.INTERVAL_DAY
            SettingsManager.UPDATE_FREQ_WEEKLY -> AlarmManager.INTERVAL_DAY * 7L
            SettingsManager.UPDATE_FREQ_MONTHLY -> AlarmManager.INTERVAL_DAY * 30L
            else -> AlarmManager.INTERVAL_DAY
        }

        val triggerAtMillis = System.currentTimeMillis() + intervalMillis
        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC,
                triggerAtMillis,
                intervalMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelSchedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, UpdateReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
