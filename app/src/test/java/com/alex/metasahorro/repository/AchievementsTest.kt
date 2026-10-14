package com.alex.metasahorro.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementsTest {

    private val empty = AchievementStats(deposits = 0, transforms = 0, purchases = 0, streak = 0, totalSaved = 0.0, categories = 0)

    @Test
    fun sinActividadNoHayLogros() {
        assertEquals(emptySet<Achievement>(), achievementsReached(empty))
    }

    @Test
    fun primerDepositoDesbloqueaPrimerPaso() {
        assertEquals(setOf(Achievement.FIRST_DEPOSIT), achievementsReached(empty.copy(deposits = 1)))
    }

    @Test
    fun lasRachasSeAcumulan() {
        assertEquals(
            setOf(Achievement.STREAK_3),
            achievementsReached(empty.copy(streak = 3))
        )
        assertEquals(
            setOf(Achievement.STREAK_3, Achievement.STREAK_7, Achievement.STREAK_30),
            achievementsReached(empty.copy(streak = 30))
        )
    }

    @Test
    fun losMontosSeDesbloqueanEnSusUmbrales() {
        assertEquals(emptySet<Achievement>(), achievementsReached(empty.copy(totalSaved = 99_999.0)))
        assertEquals(setOf(Achievement.SAVED_100K), achievementsReached(empty.copy(totalSaved = 100_000.0)))
        assertEquals(
            setOf(Achievement.SAVED_100K, Achievement.SAVED_500K, Achievement.SAVED_1M),
            achievementsReached(empty.copy(totalSaved = 1_000_000.0))
        )
    }

    @Test
    fun comprasTransformacionesYCategorias() {
        assertEquals(setOf(Achievement.FIRST_PURCHASE), achievementsReached(empty.copy(purchases = 1)))
        assertEquals(
            setOf(Achievement.FIRST_PURCHASE, Achievement.PURCHASES_5),
            achievementsReached(empty.copy(purchases = 5))
        )
        assertEquals(setOf(Achievement.FIRST_TRANSFORM), achievementsReached(empty.copy(transforms = 2)))
        assertEquals(emptySet<Achievement>(), achievementsReached(empty.copy(categories = 2)))
        assertEquals(setOf(Achievement.CATEGORIES_3), achievementsReached(empty.copy(categories = 3)))
    }

    @Test
    fun cadaLogroTieneIdUnicoParaGuardarlo() {
        assertEquals(Achievement.entries.size, Achievement.entries.map { it.name }.toSet().size)
    }
}
