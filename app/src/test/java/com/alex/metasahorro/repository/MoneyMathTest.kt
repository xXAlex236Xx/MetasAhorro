package com.alex.metasahorro.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MoneyMathTest {

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 14)

    private fun millisOf(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000

    // ---- splitPurchase ----

    @Test
    fun split_proporcionalALaMezcla() {
        val (cash, digital) = splitPurchase(cash = 300.0, digital = 700.0, goal = 500.0)
        assertEquals(150.0, cash, 0.001)
        assertEquals(350.0, digital, 0.001)
    }

    @Test
    fun split_soloEfectivo() {
        val (cash, digital) = splitPurchase(cash = 1000.0, digital = 0.0, goal = 400.0)
        assertEquals(400.0, cash, 0.001)
        assertEquals(0.0, digital, 0.001)
    }

    @Test
    fun split_poolVacioNoRestaNada() {
        val (cash, digital) = splitPurchase(cash = 0.0, digital = 0.0, goal = 100.0)
        assertEquals(0.0, cash, 0.001)
        assertEquals(0.0, digital, 0.001)
    }

    @Test
    fun split_sumaExactamenteElPrecio() {
        val (cash, digital) = splitPurchase(cash = 333.0, digital = 667.0, goal = 777.0)
        assertEquals(777.0, cash + digital, 0.011)
    }

    // ---- computeStreak ----

    @Test
    fun streak_sinDepositosEsCero() {
        assertEquals(0, computeStreak(emptyList(), today, zone))
    }

    @Test
    fun streak_cuentaDiasConsecutivosHastaHoy() {
        val days = listOf(today, today.minusDays(1), today.minusDays(2)).map(::millisOf)
        assertEquals(3, computeStreak(days, today, zone))
    }

    @Test
    fun streak_conservaLaRachaDeAyerSiHoyAunNoHayDeposito() {
        val days = listOf(today.minusDays(1), today.minusDays(2)).map(::millisOf)
        assertEquals(2, computeStreak(days, today, zone))
    }

    @Test
    fun streak_sePierdeTrasUnDiaCompletoSinDepositar() {
        val days = listOf(today.minusDays(2), today.minusDays(3)).map(::millisOf)
        assertEquals(0, computeStreak(days, today, zone))
    }

    @Test
    fun streak_variosDepositosEnUnDiaCuentanUnaVez() {
        val d = millisOf(today)
        assertEquals(1, computeStreak(listOf(d, d + 1000, d + 2000), today, zone))
    }

    @Test
    fun streak_unHuecoCortaLaRacha() {
        val days = listOf(today, today.minusDays(1), today.minusDays(3)).map(::millisOf)
        assertEquals(2, computeStreak(days, today, zone))
    }
}
