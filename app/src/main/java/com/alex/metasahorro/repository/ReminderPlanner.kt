package com.alex.metasahorro.repository

import com.alex.metasahorro.data.Meta
import com.alex.metasahorro.data.ReminderSettings
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** Cuántos días antes del vencimiento se empieza a avisar. */
const val DEADLINE_WARNING_DAYS = 3

/** Una meta con fecha límite a pocos días de vencer. */
data class DeadlineAlert(val metaName: String, val daysLeft: Int, val missing: Double)

/**
 * Qué avisar hoy: `streakAtRisk` es la racha que se perdería si hoy no se registra nada (null si no aplica),
 * `askToSave` invita a ahorrar cuando no hay racha, y `deadlines` lista las metas por vencer.
 */
data class ReminderPlan(val streakAtRisk: Int?, val askToSave: Boolean, val deadlines: List<DeadlineAlert>) {
    val isEmpty get() = streakAtRisk == null && !askToSave && deadlines.isEmpty()
}

/** Milisegundos hasta la próxima vez que el reloj marque `hour`:00 (siempre en el futuro; si ya pasó hoy, es mañana). */
fun millisUntilNext(hour: Int, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    var target = now.toLocalDate().atTime(hour.coerceIn(0, 23), 0).atZone(zone)
    if (!target.isAfter(now)) target = now.toLocalDate().plusDays(1).atTime(hour.coerceIn(0, 23), 0).atZone(zone)
    return target.toInstant().toEpochMilli() - nowMillis
}

/** Decide qué avisos corresponden ahora según los ajustes del usuario. Pura: no toca notificaciones. */
fun planReminders(
    nowMillis: Long,
    depositMillis: List<Long>,
    metas: List<Meta>,
    totalSaved: Double,
    settings: ReminderSettings,
    zone: ZoneId = ZoneId.systemDefault()
): ReminderPlan {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val depositedToday = depositMillis.any { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today }

    var streakAtRisk: Int? = null
    var askToSave = false
    if (settings.dailyEnabled && !depositedToday) {
        val streak = computeStreak(depositMillis, today, zone)
        if (streak > 0) streakAtRisk = streak else askToSave = true
    }

    val deadlines = if (!settings.deadlineEnabled) emptyList() else metas.mapNotNull { meta ->
        val deadline = meta.deadlineMillis ?: return@mapNotNull null
        val missing = meta.goal - minOf(totalSaved, meta.goal)
        if (missing <= 0.01 || deadline <= nowMillis) return@mapNotNull null
        val daysLeft = ceil((deadline - nowMillis) / DAY_MILLIS.toDouble()).toInt().coerceAtLeast(1)
        if (daysLeft > DEADLINE_WARNING_DAYS) null else DeadlineAlert(meta.name, daysLeft, missing)
    }.sortedBy { it.daysLeft }

    return ReminderPlan(streakAtRisk, askToSave, deadlines)
}
