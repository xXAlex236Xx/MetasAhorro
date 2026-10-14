package com.alex.metasahorro.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alex.metasahorro.MainActivity
import com.alex.metasahorro.R
import com.alex.metasahorro.data.ReminderSettings
import com.alex.metasahorro.repository.ReminderPlan
import com.alex.metasahorro.repository.millisUntilNext
import com.alex.metasahorro.util.fmtMoney
import java.util.concurrent.TimeUnit

/**
 * Programa y muestra los recordatorios. Un trabajo periódico de WorkManager corre una vez al día cerca de
 * la hora elegida (Android puede retrasarlo unos minutos para ahorrar batería) y decide qué avisar con
 * `planReminders`.
 */
object Reminders {
    private const val CHANNEL_ID = "reminders"
    private const val WORK_NAME = "daily_reminder"
    private const val ID_DAILY = 1
    private const val ID_DEADLINE_BASE = 100

    /** Deja programado (o cancela) el aviso diario según los ajustes. Se llama al abrir la app y al cambiar los ajustes. */
    fun sync(context: Context, settings: ReminderSettings) {
        val work = WorkManager.getInstance(context)
        if (!settings.dailyEnabled && !settings.deadlineEnabled) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(millisUntilNext(settings.hour, System.currentTimeMillis()), TimeUnit.MILLISECONDS)
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Muestra las notificaciones del plan. No hace nada si el usuario no dio permiso de notificaciones. */
    fun show(context: Context, plan: ReminderPlan) {
        val manager = NotificationManagerCompat.from(context)
        if (plan.isEmpty || !manager.areNotificationsEnabled()) return
        ensureChannel(context)

        plan.streakAtRisk?.let { days ->
            post(
                context, ID_DAILY, context.getString(R.string.notif_streak_title),
                context.resources.getQuantityString(R.plurals.notif_streak_text, days, days)
            )
        }
        if (plan.askToSave) {
            post(context, ID_DAILY, context.getString(R.string.notif_save_title), context.getString(R.string.notif_save_text))
        }
        plan.deadlines.take(3).forEachIndexed { i, alert ->
            post(
                context, ID_DEADLINE_BASE + i,
                context.getString(R.string.notif_deadline_title, alert.metaName),
                context.resources.getQuantityString(
                    R.plurals.notif_deadline_text, alert.daysLeft, alert.daysLeft, fmtMoney(alert.missing)
                )
            )
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID, context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.notif_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun post(context: Context, id: Int, title: String, text: String) {
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // El permiso se revocó entre la comprobación y el envío: no hay nada que hacer.
        }
    }
}
