package com.alex.metasahorro.repository

import com.alex.metasahorro.data.DEFAULT_QUICK_AMOUNTS
import com.alex.metasahorro.data.PrefsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Lectura de montos rápidos guardados y limpieza de categorías. */
class SettingsLogicTest {

    @Test
    fun montosRapidos_sinDatoUsaLosDeFabrica() {
        assertEquals(DEFAULT_QUICK_AMOUNTS, PrefsStore.parseQuickAmounts(null))
        assertEquals(DEFAULT_QUICK_AMOUNTS, PrefsStore.parseQuickAmounts(""))
    }

    @Test
    fun montosRapidos_valoresInvalidosUsanLosDeFabrica() {
        assertEquals(DEFAULT_QUICK_AMOUNTS, PrefsStore.parseQuickAmounts("abc"))
        assertEquals(DEFAULT_QUICK_AMOUNTS, PrefsStore.parseQuickAmounts("0,-5"))
    }

    @Test
    fun montosRapidos_leeLosValidosYSaltaLosMalos() {
        assertEquals(listOf(1_000, 2_000), PrefsStore.parseQuickAmounts("1000,2000"))
        assertEquals(listOf(500, 700), PrefsStore.parseQuickAmounts(" 500 , x,700"))
    }

    @Test
    fun montosRapidos_soloSeGuardanLosPrimerosTres() {
        assertEquals(listOf(1, 2, 3), PrefsStore.parseQuickAmounts("1,2,3,4,5"))
    }

    @Test
    fun categoria_quitaEspaciosYVaciosSonNulos() {
        assertEquals("Tecnología", normalizeCategory("  Tecnología  "))
        assertNull(normalizeCategory(""))
        assertNull(normalizeCategory("    "))
        assertNull(normalizeCategory(null))
    }

    @Test
    fun categoria_seCortaAlLargoMaximo() {
        val long = "a".repeat(MAX_CATEGORY_LENGTH + 10)
        assertEquals(MAX_CATEGORY_LENGTH, normalizeCategory(long)!!.length)
    }
}
