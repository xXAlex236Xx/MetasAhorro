package com.alex.metasahorro.repository

import com.alex.metasahorro.data.MoneyType
import com.alex.metasahorro.data.Movement
import com.alex.metasahorro.data.MovementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Aporte sugerido, ritmo de ahorro, fecha estimada y serie del gráfico. */
class ForecastTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 14)

    private fun deposit(amount: Double, msAgo: Long) =
        Movement(kind = MovementKind.DEPOSIT, amount = amount, moneyType = MoneyType.EFECTIVO, tsMillis = now - msAgo)

    private fun withdraw(amount: Double, msAgo: Long) =
        Movement(kind = MovementKind.WITHDRAW, amount = amount, moneyType = MoneyType.EFECTIVO, tsMillis = now - msAgo)

    private fun at(date: LocalDate, kind: MovementKind, amount: Double) = Movement(
        kind = kind, amount = amount, moneyType = if (kind == MovementKind.TRANSFORM) null else MoneyType.EFECTIVO,
        tsMillis = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    )

    // ---- suggestedContribution ----

    @Test
    fun aporte_repartidoEntreLosDiasQueQuedan() {
        val c = suggestedContribution(remaining = 70_000.0, deadlineMillis = now + 14 * day, nowMillis = now)!!
        assertEquals(14, c.daysLeft)
        assertEquals(5_000.0, c.perDay, 0.001)
        assertEquals(35_000.0, c.perWeek, 0.001)
    }

    @Test
    fun aporte_conMenosDeUnaSemanaLaSemanaCuentaComoUna() {
        val c = suggestedContribution(remaining = 30_000.0, deadlineMillis = now + 3 * day, nowMillis = now)!!
        assertEquals(10_000.0, c.perDay, 0.001)
        assertEquals(30_000.0, c.perWeek, 0.001)
    }

    @Test
    fun aporte_sinFechaFechaPasadaOSinFaltanteEsNulo() {
        assertNull(suggestedContribution(10_000.0, null, now))
        assertNull(suggestedContribution(10_000.0, now - day, now))
        assertNull(suggestedContribution(10_000.0, now, now))
        assertNull(suggestedContribution(0.0, now + day, now))
    }

    // ---- estimateDailyRate ----

    @Test
    fun ritmo_sinMovimientosEsCero() {
        assertEquals(0.0, estimateDailyRate(emptyList(), now), 0.0)
    }

    @Test
    fun ritmo_depositosMenosRetirosEntreLosDiasDeHistorial() {
        // 10 días de historial → ventana de 11 días
        val movements = listOf(deposit(100_000.0, 10 * day), withdraw(20_000.0, 5 * day))
        assertEquals(80_000.0 / 11, estimateDailyRate(movements, now), 0.001)
    }

    @Test
    fun ritmo_lasComprasNoCuentan() {
        val purchase = Movement(kind = MovementKind.PURCHASE, amount = 50_000.0, label = "X", tsMillis = now - day)
        val movements = listOf(deposit(100_000.0, 10 * day), purchase)
        assertEquals(100_000.0 / 11, estimateDailyRate(movements, now), 0.001)
    }

    @Test
    fun ritmo_conPocoHistorialLaVentanaMinimaEsSieteDias() {
        assertEquals(1_000.0, estimateDailyRate(listOf(deposit(7_000.0, 60 * 60 * 1000)), now), 0.001)
    }

    @Test
    fun ritmo_laVentanaMaximaEsTreintaDiasYIgnoraLoAntiguo() {
        val movements = listOf(deposit(1_000_000.0, 100 * day), deposit(3_000.0, 1000))
        assertEquals(100.0, estimateDailyRate(movements, now), 0.001)
    }

    // ---- estimateCompletion ----

    @Test
    fun fechaEstimada_faltanteDivididoPorRitmo() {
        assertEquals(now + 10 * day, estimateCompletion(100_000.0, 10_000.0, now))
    }

    @Test
    fun fechaEstimada_redondeaHaciaArriba() {
        assertEquals(now + 4 * day, estimateCompletion(30_001.0, 10_000.0, now))
    }

    @Test
    fun fechaEstimada_nulaSiYaEstaSinRitmoOMasDeDiezAnios() {
        assertNull(estimateCompletion(0.0, 10_000.0, now))
        assertNull(estimateCompletion(100_000.0, 0.0, now))
        assertNull(estimateCompletion(100_000.0, -5.0, now))
        assertNull(estimateCompletion(1_000_000_000.0, 1.0, now))
    }

    // ---- balanceSeries ----

    @Test
    fun serie_sinMovimientosEstaVacia() {
        assertEquals(emptyList<BalancePoint>(), balanceSeries(emptyList(), 0.0, null, today, zone))
    }

    @Test
    fun serie_todoElHistorialArrancaEnCeroYSumaCadaDia() {
        val movements = listOf(
            at(today.minusDays(4), MovementKind.DEPOSIT, 100.0),
            at(today.minusDays(2), MovementKind.WITHDRAW, 30.0),
            at(today.minusDays(1), MovementKind.PURCHASE, 20.0),
            at(today.minusDays(1), MovementKind.TRANSFORM, 999.0)
        )
        val series = balanceSeries(movements, currentTotal = 50.0, rangeDays = null, today = today, zone = zone)
        assertEquals(listOf(0.0, 100.0, 100.0, 70.0, 50.0, 50.0), series.map { it.balance })
    }

    @Test
    fun serie_conRangoPartedelSaldoAcumuladoAnterior() {
        val movements = listOf(
            at(today.minusDays(4), MovementKind.DEPOSIT, 100.0),
            at(today.minusDays(2), MovementKind.WITHDRAW, 30.0),
            at(today.minusDays(1), MovementKind.PURCHASE, 20.0)
        )
        val series = balanceSeries(movements, currentTotal = 50.0, rangeDays = 2, today = today, zone = zone)
        assertEquals(listOf(50.0, 50.0), series.map { it.balance })
    }

    @Test
    fun serie_elUltimoPuntoSeAnclaAlTotalReal() {
        val movements = listOf(at(today.minusDays(1), MovementKind.DEPOSIT, 100.0))
        val series = balanceSeries(movements, currentTotal = 90.0, rangeDays = null, today = today, zone = zone)
        assertEquals(90.0, series.last().balance, 0.0)
        assertNotNull(series.first())
    }

    @Test
    fun serie_unMovimientoConFechaFuturaCuentaComoHoy() {
        val movements = listOf(at(today.plusDays(3), MovementKind.DEPOSIT, 100.0))
        val series = balanceSeries(movements, currentTotal = 100.0, rangeDays = null, today = today, zone = zone)
        assertEquals(today.atStartOfDay(zone).toInstant().toEpochMilli(), series.last().dayStartMillis)
    }
}
