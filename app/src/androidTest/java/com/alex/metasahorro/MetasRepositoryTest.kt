package com.alex.metasahorro

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alex.metasahorro.data.AppDatabase
import com.alex.metasahorro.data.Meta
import com.alex.metasahorro.data.MoneyType
import com.alex.metasahorro.data.Movement
import com.alex.metasahorro.data.MovementKind
import com.alex.metasahorro.data.PoolState
import com.alex.metasahorro.repository.Achievement
import com.alex.metasahorro.repository.MetasRepository
import com.alex.metasahorro.repository.UndoAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba la lógica de dinero del repositorio contra una base de datos Room real pero en memoria
 * (se borra al terminar cada test, no toca los datos de la app).
 */
@RunWith(AndroidJUnit4::class)
class MetasRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: MetasRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = MetasRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun pool(): PoolState = runBlocking { db.dao().getPool() ?: PoolState() }
    private fun movements(): List<Movement> = runBlocking { db.dao().getMovements() }
    private fun metas(): List<Meta> = runBlocking { db.dao().getMetas() }
    private fun seed(cash: Double, digital: Double) = runBlocking { db.dao().upsertPool(PoolState(cash = cash, digital = digital)) }

    // ---- registrar / restar ----

    @Test
    fun registrar_sumaAlTipoCorrectoYAnotaElMovimiento() = runBlocking {
        assertNotNull(repo.registrar(1_000.0, MoneyType.EFECTIVO))
        assertNotNull(repo.registrar(500.0, MoneyType.DIGITAL))
        assertEquals(1_000.0, pool().cash, 0.001)
        assertEquals(500.0, pool().digital, 0.001)
        assertEquals(listOf(MovementKind.DEPOSIT, MovementKind.DEPOSIT), movements().map { it.kind })
    }

    @Test
    fun registrar_rechazaMontosInvalidos() = runBlocking {
        listOf(0.0, -5.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { assertNull(repo.registrar(it, MoneyType.EFECTIVO)) }
        assertEquals(0.0, pool().cash, 0.0)
        assertTrue(movements().isEmpty())
    }

    @Test
    fun restar_descuentaYSinSaldoNoTocaNada() = runBlocking {
        seed(cash = 1_000.0, digital = 0.0)
        assertNotNull(repo.restar(400.0, MoneyType.EFECTIVO))
        assertEquals(600.0, pool().cash, 0.001)

        assertNull(repo.restar(700.0, MoneyType.EFECTIVO))
        assertNull(repo.restar(1.0, MoneyType.DIGITAL))
        assertEquals(600.0, pool().cash, 0.001)
        assertEquals(1, movements().size)
    }

    @Test
    fun registrar_dosVecesAlMismoTiempoNoPierdeNingunaActualizacion() = runBlocking {
        (1..20).map { async(Dispatchers.IO) { repo.registrar(100.0, MoneyType.EFECTIVO) } }.awaitAll()
        assertEquals(2_000.0, pool().cash, 0.001)
        assertEquals(20, movements().size)
    }

    // ---- transformar ----

    @Test
    fun transformar_conservaElTotalYGuardaElOrigen() = runBlocking {
        seed(cash = 1_000.0, digital = 200.0)
        assertNotNull(repo.transformar(300.0, MoneyType.EFECTIVO))
        assertEquals(700.0, pool().cash, 0.001)
        assertEquals(500.0, pool().digital, 0.001)
        assertEquals(1_200.0, pool().cash + pool().digital, 0.001)
        assertEquals(MoneyType.EFECTIVO, movements().single().moneyType)
    }

    @Test
    fun transformar_sinSaldoEnElOrigenNoHaceNada() = runBlocking {
        seed(cash = 100.0, digital = 0.0)
        assertNull(repo.transformar(50.0, MoneyType.DIGITAL))
        assertEquals(100.0, pool().cash, 0.001)
        assertTrue(movements().isEmpty())
    }

    // ---- comprar ----

    @Test
    fun marcarComprado_repartePorLaMezclaArchivaYBorraLaMeta() = runBlocking {
        seed(cash = 300.0, digital = 700.0)
        repo.addMeta("Audífonos", null, 500.0, null, "  Tecnología ")
        val meta = metas().single()

        assertNotNull(repo.marcarComprado(meta))

        assertEquals(150.0, pool().cash, 0.001)
        assertEquals(350.0, pool().digital, 0.001)
        assertTrue(metas().isEmpty())
        val bought = repo.purchased.first().single()
        assertEquals("Audífonos", bought.name)
        assertEquals("Tecnología", bought.category)
        assertEquals(MovementKind.PURCHASE, movements().single().kind)
    }

    @Test
    fun marcarComprado_sinFondosNoTocaNada() = runBlocking {
        seed(cash = 100.0, digital = 100.0)
        repo.addMeta("Bicicleta", null, 650.0, null)
        assertNull(repo.marcarComprado(metas().single()))

        assertEquals(1, metas().size)
        assertEquals(100.0, pool().cash, 0.001)
        assertTrue(repo.purchased.first().isEmpty())
        assertTrue(movements().isEmpty())
    }

    // ---- deshacer ----

    @Test
    fun deshacer_unDepositoUnRetiroYUnaTransformacion() = runBlocking {
        seed(cash = 1_000.0, digital = 1_000.0)
        val before = pool()

        listOf(
            repo.registrar(250.0, MoneyType.DIGITAL),
            repo.restar(100.0, MoneyType.EFECTIVO),
            repo.transformar(400.0, MoneyType.EFECTIVO)
        ).forEach { undo ->
            assertNotNull(undo)
            assertTrue(repo.undo(undo!!))
        }
        assertEquals(before, pool())
        assertTrue(movements().isEmpty())
    }

    @Test
    fun deshacer_unaCompraReponeLaMetaElDineroYQuitaElArchivo() = runBlocking {
        seed(cash = 300.0, digital = 700.0)
        repo.addMeta("Audífonos", null, 500.0, null)
        val meta = metas().single()
        val undo = repo.marcarComprado(meta)!!

        assertTrue(repo.undo(undo))

        assertEquals(300.0, pool().cash, 0.001)
        assertEquals(700.0, pool().digital, 0.001)
        assertEquals(meta, metas().single())
        assertTrue(repo.purchased.first().isEmpty())
        assertTrue(movements().isEmpty())
    }

    @Test
    fun deshacer_unaMetaBorradaLaDevuelveConSuMismoId() = runBlocking {
        repo.addMeta("Bicicleta", null, 650.0, null)
        val meta = metas().single()
        val undo = repo.deleteMeta(meta)
        assertTrue(metas().isEmpty())

        assertTrue(undo is UndoAction.RestoreMeta)
        assertTrue(repo.undo(undo))
        assertEquals(meta, metas().single())
    }

    // ---- borrar movimientos ----

    @Test
    fun revertMovement_noDejaSaldoNegativo() = runBlocking {
        repo.registrar(100.0, MoneyType.EFECTIVO)
        repo.restar(100.0, MoneyType.EFECTIVO)
        val deposit = movements().first { it.kind == MovementKind.DEPOSIT }

        assertFalse(repo.revertMovement(deposit.id))
        assertEquals(2, movements().size)
        assertEquals(0.0, pool().cash, 0.001)
    }

    @Test
    fun revertMovement_lasComprasNoSePuedenBorrarDesdeElHistorial() = runBlocking {
        seed(cash = 500.0, digital = 0.0)
        repo.addMeta("X", null, 500.0, null)
        repo.marcarComprado(metas().single())
        val purchase = movements().single()

        assertFalse(repo.revertMovement(purchase.id))
        assertEquals(1, movements().size)
    }

    @Test
    fun revertMovement_entiendeTransformacionesAntiguasSinOrigenGuardado() = runBlocking {
        seed(cash = 700.0, digital = 500.0)
        val id = db.dao().insertMovement(
            Movement(kind = MovementKind.TRANSFORM, amount = 300.0, moneyType = null, label = "Efectivo → Digital", tsMillis = 1L)
        )

        assertTrue(repo.revertMovement(id))
        assertEquals(1_000.0, pool().cash, 0.001)
        assertEquals(200.0, pool().digital, 0.001)
    }

    // ---- metas, categorías y logros ----

    @Test
    fun categorias_seLimpianAlCrearYAlEditar() = runBlocking {
        repo.addMeta("A", null, 100.0, null, "   ")
        repo.addMeta("B", null, 100.0, null, " Viajes ")
        assertEquals(listOf(null, "Viajes"), metas().map { it.category })

        repo.updateMeta(metas().first().copy(category = "  Hogar  "))
        assertEquals("Hogar", metas().first().category)
    }

    @Test
    fun moveMeta_intercambiaLasPosiciones() = runBlocking {
        repo.addMeta("Primera", null, 100.0, null)
        repo.addMeta("Segunda", null, 100.0, null)
        val list = metas()

        repo.moveMeta(list, list[1], -1)

        assertEquals(listOf("Segunda", "Primera"), metas().map { it.name })
    }

    @Test
    fun logros_soloSeCuentanComoNuevosLaPrimeraVez() = runBlocking {
        val first = repo.unlockAchievements(listOf(Achievement.FIRST_DEPOSIT, Achievement.STREAK_3))
        val second = repo.unlockAchievements(listOf(Achievement.STREAK_3, Achievement.STREAK_7))

        assertEquals(listOf(Achievement.FIRST_DEPOSIT, Achievement.STREAK_3), first)
        assertEquals(listOf(Achievement.STREAK_7), second)
        assertEquals(3, repo.achievements.first().size)
    }
}
