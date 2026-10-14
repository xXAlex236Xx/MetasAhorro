package com.alex.metasahorro.repository

import com.alex.metasahorro.data.Movement
import com.alex.metasahorro.data.MovementKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** Redondea a centavos para que las proporciones no arrastren decimales basura. */
fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

/**
 * Reparte el precio de una compra entre efectivo y digital en proporción a lo que hay
 * en cada uno. Devuelve (efectivo a restar, digital a restar).
 */
fun splitPurchase(cash: Double, digital: Double, goal: Double): Pair<Double, Double> {
    val total = cash + digital
    val ratioCash = if (total > 0) cash / total else 0.0
    val subtractCash = round2(minOf(cash, goal * ratioCash))
    val subtractDigital = round2(minOf(digital, goal - subtractCash))
    return subtractCash to subtractDigital
}

/**
 * Días seguidos con al menos un depósito. Si hoy todavía no hay depósito, la racha de
 * ayer se conserva (se pierde recién cuando pasa un día completo sin depositar).
 */
fun computeStreak(
    depositMillis: List<Long>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): Int {
    val days = depositMillis.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet()
    var day = if (today in days) today else today.minusDays(1)
    var streak = 0
    while (day in days) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

/** Cuánto ahorrar por día y por semana para llegar a la fecha límite, y cuántos días quedan. */
data class Contribution(val perDay: Double, val perWeek: Double, val daysLeft: Int)

/**
 * Aporte necesario para juntar lo que falta antes de la fecha límite. Devuelve null si la meta
 * no tiene fecha, la fecha ya pasó o ya no falta nada.
 */
fun suggestedContribution(remaining: Double, deadlineMillis: Long?, nowMillis: Long): Contribution? {
    if (deadlineMillis == null || remaining <= 0.0 || deadlineMillis <= nowMillis) return null
    val daysLeft = ceil((deadlineMillis - nowMillis) / DAY_MILLIS.toDouble()).toInt().coerceAtLeast(1)
    val weeks = ceil(daysLeft / 7.0).coerceAtLeast(1.0)
    return Contribution(perDay = remaining / daysLeft, perWeek = remaining / weeks, daysLeft = daysLeft)
}

/**
 * Ritmo de ahorro neto por día (depósitos menos retiros) en los últimos 7 a 30 días, según cuánto
 * historial haya. Las compras no cuentan: son gasto planeado, no un cambio de hábito.
 */
fun estimateDailyRate(movements: List<Movement>, nowMillis: Long): Double {
    val flows = movements.filter { it.kind == MovementKind.DEPOSIT || it.kind == MovementKind.WITHDRAW }
    val first = flows.minOfOrNull { it.tsMillis } ?: return 0.0
    val historyDays = ((nowMillis - first) / DAY_MILLIS).toInt() + 1
    val window = historyDays.coerceIn(7, 30)
    val since = nowMillis - window * DAY_MILLIS
    val net = flows.filter { it.tsMillis >= since }
        .sumOf { if (it.kind == MovementKind.DEPOSIT) it.amount else -it.amount }
    return net / window
}

/** Fecha estimada para juntar `remaining` al ritmo dado. Null si ya está, el ritmo no es positivo o tardaría más de 10 años. */
fun estimateCompletion(remaining: Double, dailyRate: Double, nowMillis: Long): Long? {
    if (remaining <= 0.0 || dailyRate <= 0.0) return null
    val days = ceil(remaining / dailyRate)
    if (days > 3650) return null
    return nowMillis + days.toLong() * DAY_MILLIS
}

/** Un punto del gráfico: el total ahorrado al cierre de ese día. */
data class BalancePoint(val dayStartMillis: Long, val balance: Double)

/**
 * Serie diaria del total ahorrado para el gráfico. `rangeDays` null = desde el primer movimiento.
 * El último punto se ancla al total real del pozo (`currentTotal`) para que el gráfico termine
 * exactamente donde está el saldo. Sin movimientos devuelve una lista vacía.
 */
fun balanceSeries(
    movements: List<Movement>,
    currentTotal: Double,
    rangeDays: Int?,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): List<BalancePoint> {
    if (movements.isEmpty()) return emptyList()

    fun dayOf(m: Movement): LocalDate =
        minOf(Instant.ofEpochMilli(m.tsMillis).atZone(zone).toLocalDate(), today)

    fun delta(m: Movement): Double = when (m.kind) {
        MovementKind.DEPOSIT -> m.amount
        MovementKind.WITHDRAW, MovementKind.PURCHASE -> -m.amount
        MovementKind.TRANSFORM -> 0.0
    }

    val deltas = movements.groupBy(::dayOf).mapValues { (_, list) -> list.sumOf(::delta) }
    val firstDay = deltas.keys.min()
    val rangeStart = if (rangeDays == null) firstDay.minusDays(1) else today.minusDays(rangeDays.toLong() - 1)
    val start = maxOf(rangeStart, firstDay.minusDays(1))

    var running = deltas.filterKeys { it < start }.values.sum()
    val points = mutableListOf<BalancePoint>()
    var day = start
    while (!day.isAfter(today)) {
        running += deltas[day] ?: 0.0
        points += BalancePoint(day.atStartOfDay(zone).toInstant().toEpochMilli(), running)
        day = day.plusDays(1)
    }
    if (points.isNotEmpty()) points[points.lastIndex] = points.last().copy(balance = currentTotal)
    return points
}
