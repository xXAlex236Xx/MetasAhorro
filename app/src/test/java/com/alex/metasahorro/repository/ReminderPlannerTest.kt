package com.alex.metasahorro.repository

import com.alex.metasahorro.data.Meta
import com.alex.metasahorro.data.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReminderPlannerTest {

    private val zone = ZoneId.of("UTC")
    private val day = 24L * 60 * 60 * 1000
    private val today = LocalDate.of(2026, 10, 14)
    private val now = today.atTime(20, 0).atZone(zone).toInstant().toEpochMilli()

    private val all = ReminderSettings(dailyEnabled = true, hour = 20, deadlineEnabled = true)

    private fun depositAt(daysAgo: Long) = now - daysAgo * day

    private fun meta(name: String, goal: Double, deadlineInDays: Long?) =
        Meta(name = name, goal = goal, deadlineMillis = deadlineInDays?.let { now + it * day })

    // ---- aviso diario ----

    @Test
    fun siYaRegistroHoyNoSeAvisa() {
        val plan = planReminders(now, listOf(depositAt(0), depositAt(1)), emptyList(), 0.0, all, zone)
        assertTrue(plan.isEmpty)
    }

    @Test
    fun sinRegistroHoyYConRachaSeAvisaQueSePierde() {
        val plan = planReminders(now, listOf(depositAt(1), depositAt(2)), emptyList(), 0.0, all, zone)
        assertEquals(2, plan.streakAtRisk)
        assertEquals(false, plan.askToSave)
    }

    @Test
    fun sinRegistroHoyNiRachaSeInvitaAAhorrar() {
        val plan = planReminders(now, listOf(depositAt(5)), emptyList(), 0.0, all, zone)
        assertNull(plan.streakAtRisk)
        assertTrue(plan.askToSave)
    }

    @Test
    fun conElAvisoDiarioApagadoNoSeAvisa() {
        val off = all.copy(dailyEnabled = false)
        val plan = planReminders(now, listOf(depositAt(1)), emptyList(), 0.0, off, zone)
        assertNull(plan.streakAtRisk)
        assertEquals(false, plan.askToSave)
    }

    // ---- metas por vencer ----

    @Test
    fun avisaLasMetasQueVencenEnTresDiasOMenos() {
        val metas = listOf(meta("Cerca", 100_000.0, 2), meta("Lejos", 100_000.0, 5), meta("Mañana", 100_000.0, 1))
        val plan = planReminders(now, listOf(depositAt(0)), metas, 40_000.0, all, zone)
        assertEquals(listOf("Mañana", "Cerca"), plan.deadlines.map { it.metaName })
        assertEquals(listOf(1, 2), plan.deadlines.map { it.daysLeft })
        assertEquals(60_000.0, plan.deadlines.first().missing, 0.001)
    }

    @Test
    fun noAvisaMetasCompletasVencidasNiSinFecha() {
        val metas = listOf(
            meta("Completa", 30_000.0, 2),
            meta("Vencida", 100_000.0, -1),
            meta("SinFecha", 100_000.0, null)
        )
        val plan = planReminders(now, listOf(depositAt(0)), metas, 40_000.0, all, zone)
        assertTrue(plan.deadlines.isEmpty())
    }

    @Test
    fun conElAvisoDeFechasApagadoNoSeAvisa() {
        val off = all.copy(deadlineEnabled = false)
        val plan = planReminders(now, listOf(depositAt(0)), listOf(meta("Cerca", 100_000.0, 2)), 0.0, off, zone)
        assertTrue(plan.deadlines.isEmpty())
    }

    // ---- millisUntilNext ----

    private fun at(hour: Int, minute: Int = 0) = today.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun faltanLasHorasHastaLaHoraDeHoy() {
        assertEquals(10L * 60 * 60 * 1000, millisUntilNext(20, at(10), zone))
    }

    @Test
    fun siLaHoraYaPasoEsManana() {
        assertEquals(23L * 60 * 60 * 1000, millisUntilNext(20, at(21), zone))
    }

    @Test
    fun justoALaHoraEsDentroDeUnDia() {
        assertEquals(day, millisUntilNext(20, at(20), zone))
    }
}
