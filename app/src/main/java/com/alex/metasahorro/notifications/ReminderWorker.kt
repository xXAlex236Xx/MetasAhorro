package com.alex.metasahorro.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alex.metasahorro.data.AppDatabase
import com.alex.metasahorro.data.MovementKind
import com.alex.metasahorro.data.PoolState
import com.alex.metasahorro.data.PrefsStore
import com.alex.metasahorro.repository.planReminders
import kotlinx.coroutines.flow.first

/** Trabajo diario: lee la base de datos, arma el plan de avisos y lo muestra. Sin ajustes activos no hace nada. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = PrefsStore(applicationContext).reminders.first()
        if (!settings.dailyEnabled && !settings.deadlineEnabled) return Result.success()

        val dao = AppDatabase.getInstance(applicationContext).dao()
        val pool = dao.getPool() ?: PoolState()
        val plan = planReminders(
            nowMillis = System.currentTimeMillis(),
            depositMillis = dao.getMovements().filter { it.kind == MovementKind.DEPOSIT }.map { it.tsMillis },
            metas = dao.getMetas(),
            totalSaved = pool.cash + pool.digital,
            settings = settings
        )
        Reminders.show(applicationContext, plan)
        return Result.success()
    }
}
