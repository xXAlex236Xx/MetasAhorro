package com.alex.metasahorro

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alex.metasahorro.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprueba que actualizar de la versión 1 a la 2 de la base de datos conserva todos los datos del usuario.
 * Usa los esquemas exportados en app/schemas. Cada migración futura debería tener su test aquí.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private fun createV1WithData() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO metas (id, name, imageUri, goal, deadlineMillis, position) VALUES (7, 'Audífonos', 'content://foto', 150000.0, 1900000000000, 2)")
            execSQL("INSERT INTO purchased (id, name, imageUri, goal, boughtAtMillis) VALUES (3, 'Mouse', NULL, 25000.0, 1700000000000)")
            execSQL("INSERT INTO movements (id, kind, amount, moneyType, label, tsMillis) VALUES (1, 'DEPOSIT', 100.0, 'EFECTIVO', NULL, 1000)")
            execSQL("INSERT INTO pool_state (id, cash, digital) VALUES (0, 130000.0, 270000.0)")
            close()
        }
    }

    @Test
    fun migrar1a2_conservaLosDatosYAgregaLasColumnasNuevas() {
        createV1WithData()

        val db = helper.runMigrationsAndValidate(dbName, 2, true, AppDatabase.MIGRATION_1_2)

        db.query("SELECT id, name, imageUri, goal, deadlineMillis, position, category FROM metas").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(7L, c.getLong(0))
            assertEquals("Audífonos", c.getString(1))
            assertEquals("content://foto", c.getString(2))
            assertEquals(150000.0, c.getDouble(3), 0.0)
            assertEquals(1900000000000L, c.getLong(4))
            assertEquals(2, c.getInt(5))
            assertTrue("la categoría de una meta vieja debe quedar vacía", c.isNull(6))
        }
        db.query("SELECT name, goal, category FROM purchased").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Mouse", c.getString(0))
            assertEquals(25000.0, c.getDouble(1), 0.0)
            assertTrue(c.isNull(2))
        }
        db.query("SELECT cash, digital FROM pool_state WHERE id = 0").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(130000.0, c.getDouble(0), 0.0)
            assertEquals(270000.0, c.getDouble(1), 0.0)
        }
        db.query("SELECT COUNT(*) FROM movements").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM achievements").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
    }

    @Test
    fun migrar1a2_laBaseAbreConRoomYSePuedeUsar() {
        createV1WithData()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val room = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        try {
            val metas = kotlinx.coroutines.runBlocking { room.dao().getMetas() }
            assertEquals("Audífonos", metas.single().name)
            assertEquals(null, metas.single().category)
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }
}
