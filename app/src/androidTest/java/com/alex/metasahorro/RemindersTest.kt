package com.alex.metasahorro

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.alex.metasahorro.data.ReminderSettings
import com.alex.metasahorro.notifications.Reminders
import com.alex.metasahorro.repository.DeadlineAlert
import com.alex.metasahorro.repository.ReminderPlan
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprueba en el emulador lo que los tests unitarios no pueden: que las notificaciones de verdad se publican
 * (con el permiso concedido) y que el aviso diario se programa y se cancela en WorkManager.
 */
@RunWith(AndroidJUnit4::class)
class RemindersTest {

    @get:Rule
    val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private val workName = "daily_reminder"

    @Before
    fun setUp() {
        manager.cancelAll()
        WorkManager.getInstance(context).cancelUniqueWork(workName).result.get()
    }

    @After
    fun tearDown() {
        manager.cancelAll()
        WorkManager.getInstance(context).cancelUniqueWork(workName).result.get()
    }

    private fun titles(): Map<Int, String?> =
        manager.activeNotifications.associate { it.id to it.notification.extras.getString(Notification.EXTRA_TITLE) }

    /** El sistema publica las notificaciones de forma asíncrona: se espera hasta 5 s a que aparezcan las esperadas. */
    private fun waitForTitles(vararg ids: Int): Map<Int, String?> {
        repeat(50) {
            val shown = titles()
            if (ids.all { it in shown }) return shown
            Thread.sleep(100)
        }
        return titles()
    }

    @Test
    fun show_publicaElAvisoDeRachaYElDeMetaPorVencer() {
        val plan = ReminderPlan(
            streakAtRisk = 3, askToSave = false,
            deadlines = listOf(DeadlineAlert("Notebook", daysLeft = 2, missing = 500_000.0))
        )

        Reminders.show(context, plan)

        val shown = waitForTitles(1, 100)
        assertEquals(context.getString(R.string.notif_streak_title), shown[1])
        assertEquals(context.getString(R.string.notif_deadline_title, "Notebook"), shown[100])
    }

    @Test
    fun show_conUnPlanVacioNoPublicaNada() {
        Reminders.show(context, ReminderPlan(streakAtRisk = null, askToSave = false, deadlines = emptyList()))
        assertTrue(manager.activeNotifications.isEmpty())
    }

    @Test
    fun sync_programaElAvisoDiarioYLoCancelaAlApagarlo() {
        val work = WorkManager.getInstance(context)

        Reminders.sync(context, ReminderSettings(dailyEnabled = true, hour = 21, deadlineEnabled = false))
        val scheduled = work.getWorkInfosForUniqueWork(workName).get()
        assertEquals(1, scheduled.size)
        assertEquals(WorkInfo.State.ENQUEUED, scheduled.single().state)

        Reminders.sync(context, ReminderSettings(dailyEnabled = false, deadlineEnabled = false))
        val cancelled = work.getWorkInfosForUniqueWork(workName).get()
        assertTrue(cancelled.all { it.state == WorkInfo.State.CANCELLED })
    }
}
