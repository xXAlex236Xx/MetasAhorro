package com.alex.metasahorro

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.withTransaction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.alex.metasahorro.data.AccentTheme
import com.alex.metasahorro.data.AppDatabase
import com.alex.metasahorro.data.Meta
import com.alex.metasahorro.data.MoneyType
import com.alex.metasahorro.data.Movement
import com.alex.metasahorro.data.MovementKind
import com.alex.metasahorro.data.PoolState
import com.alex.metasahorro.data.Purchased
import com.alex.metasahorro.data.ThemeMode
import com.alex.metasahorro.data.ThemeStore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * No es un test de comportamiento: recorre la app con datos de ejemplo y toma las capturas que usa
 * el wiki (docs/wiki/screenshots). El nombre de cada captura es el del composable que muestra, con
 * un sufijo `__variante` si hay más de una. Se corre con tools/wiki/capture-screenshots.ps1.
 */
@RunWith(AndroidJUnit4::class)
class WikiScreenshotsTest {

    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val outDir = "/data/local/tmp/wiki"

    private val day = TimeUnit.DAYS.toMillis(1)

    @Test
    fun capturarPantallas() {
        prepararDispositivo()
        val db = AppDatabase.getInstance(context)
        runBlocking { db.clearAllTables() }
        val themeStore = ThemeStore(context)
        runBlocking {
            themeStore.setMode(ThemeMode.DARK)
            themeStore.setDynamicColor(false)
            themeStore.setTheme(AccentTheme.GREEN)
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            // ---- Onboarding (instalación limpia) ----
            esperarTexto("Bienvenido a Mis Metas")
            shot("OnboardingScreen")
            listOf("Registra lo que ahorras", "Cada meta compara contra tu total", "Marca, reordena y celebra")
                .forEachIndexed { i, titulo ->
                    compose.onNodeWithText("Siguiente").performClick()
                    esperarTexto(titulo)
                    shot("OnboardingScreen__pagina-${i + 2}")
                }
            compose.onNodeWithText("Empezar").performClick()

            // ---- Estado vacío ----
            esperarTexto("Aún no hay nada plantado")
            shot("MainScreen__vacio")

            // ---- Datos de ejemplo: de una sola vez, para que los logros salgan juntos ----
            sembrar(db)
            esperarTexto("Logros desbloqueados")
            shot("AchievementUnlockedDialog")
            compose.onNodeWithText("¡Genial!").performClick()
            esperarTexto("Audífonos")
            shot("MainScreen")

            // ---- Filtro por categoría ----
            compose.onNodeWithText("Tecnología").performClick()
            settle()
            shot("MainScreen__filtro-categoria")
            compose.onNodeWithText("Todas").performClick()
            settle()

            // ---- Detalle de una meta (foto grande y pronóstico) ----
            compose.onNodeWithText("Viaje a la playa").performClick()
            esperarTexto("Modificar")
            shot("ItemMenuSheet")
            expandirHoja()
            compose.onNodeWithText("PRONÓSTICO").performScrollTo()
            shot("ItemMenuSheet__pronostico")

            compose.onNodeWithText("Modificar", substring = true).performClick()
            esperarTexto("Guardar cambios")
            shot("AddEditMetaSheet__editar")
            cerrarHoja()

            compose.onNodeWithText("Audífonos").performClick()
            esperarTexto("✅ Comprado")
            shot("ItemMenuSheet__completa")
            cerrarHoja()

            compose.onNodeWithContentDescription("Nueva meta").performClick()
            esperarTexto("Crear meta")
            shot("AddEditMetaSheet")
            cerrarHoja()

            // ---- Borrar con Deshacer ----
            compose.onNodeWithText("Notebook").performClick()
            esperarTexto("Borrar")
            compose.onNodeWithText("Borrar", substring = true).performClick()
            esperarTexto("«Notebook» borrada")
            shot("MainScreen__meta-borrada-deshacer")
            compose.onNodeWithText("Deshacer").performClick()
            esperarTexto("Notebook")

            // ---- Dinero ----
            compose.onNodeWithText("+ Registrar").performClick()
            esperarTexto("Registrar nueva cantidad")
            compose.onNode(hasSetTextAction()).performTextInput("15000")
            shot("DepositSheet")
            compose.onNodeWithText("Registrar").performClick()
            esperarTexto("Registrado +")
            shot("MainScreen__registrado-deshacer")
            compose.onNodeWithText("Deshacer").performClick()
            settle()

            compose.onNodeWithText("− Restar").performClick()
            esperarTexto("Restar dinero")
            compose.onNode(hasSetTextAction()).performTextInput("999999")
            compose.onNodeWithText("Restar").performClick()
            esperarTexto("No tienes esa cantidad guardada.")
            shot("WithdrawSheet__error-sin-saldo")
            cerrarHoja()

            compose.onNodeWithText("🔁 Transformar").performClick()
            esperarTexto("Transformar dinero")
            compose.onNode(hasSetTextAction()).performTextInput("20000")
            shot("TransformSheet")
            cerrarHoja()

            // ---- Accesos de la cabecera ----
            abrirDesdeCabecera("Detalles", "Detalles del ahorro", "DetailsSheet", expandir = true)
            abrirDesdeCabecera("Historial", "Historial de movimientos", "HistorySheet")
            abrirDesdeCabecera("Comprados", "Ya compré esto 🏆", "PurchasedSheet")
            abrirDesdeCabecera("Logros", "desbloqueados", "AchievementsSheet", expandir = true)
            abrirDesdeCabecera("Acerca de", "Hecho por xXAlex236Xx", "InfoSheet")

            // ---- Ajustes: apariencia, montos rápidos y recordatorios ----
            compose.onNodeWithContentDescription("Ajustes").performClick()
            esperarTexto("Apariencia")
            shot("SettingsSheet")
            expandirHoja()
            compose.onNodeWithText("Recordatorios").performScrollTo()
            settle()
            shot("SettingsSheet__montos-y-recordatorios")
            compose.onNodeWithText("Verde").performScrollTo()
            compose.onNodeWithText("Morado").performClick()
            settle()
            shot("SettingsSheet__tema-morado")
            compose.onNodeWithText("Claro").performClick()
            settle()
            shot("SettingsSheet__modo-claro")
            cerrarHoja()
            shot("MainScreen__modo-claro")

            compose.onNodeWithContentDescription("Ajustes").performClick()
            esperarTexto("Apariencia")
            compose.onNodeWithText("Oscuro").performClick()
            settle()
            compose.onAllNodes(isToggleable())[0].performClick() // colores dinámicos
            settle()
            shot("SettingsSheet__colores-dinamicos")
            cerrarHoja()
            shot("MainScreen__colores-dinamicos")

            // dejar el tema original para la próxima corrida
            runBlocking {
                themeStore.setDynamicColor(false)
                themeStore.setMode(ThemeMode.DARK)
                themeStore.setTheme(AccentTheme.GREEN)
            }
            settle()
        }
    }

    // ------------------------------------------------------------------ ayudas

    private fun abrirDesdeCabecera(descripcion: String, tituloHoja: String, captura: String, expandir: Boolean = false) {
        compose.onNodeWithContentDescription(descripcion).performClick()
        esperarTexto(tituloHoja)
        if (expandir) expandirHoja()
        shot(captura)
        cerrarHoja()
    }

    /** Sube la hoja inferior a pantalla completa (por defecto abre a media altura y el contenido largo queda cortado). */
    private fun expandirHoja() {
        val w = device.displayWidth
        val h = device.displayHeight
        device.swipe(w / 2, (h * 0.70).toInt(), w / 2, (h * 0.12).toInt(), 20)
        settle()
    }

    private fun cerrarHoja() {
        device.pressBack()
        settle()
    }

    private fun esperarTexto(texto: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(texto, substring = true).fetchSemanticsNodes().isNotEmpty() }
        settle()
    }

    private fun settle() {
        compose.waitForIdle()
        Thread.sleep(900) // deja terminar animaciones de hojas y barras de progreso
    }

    /** Cierra el teclado en pantalla si está visible (taparía el contenido de las hojas). */
    private fun ocultarTeclado() {
        if (device.executeShellCommand("dumpsys input_method").contains("mInputShown=true")) {
            device.pressBack()
            settle()
        }
    }

    private fun shot(name: String) {
        ocultarTeclado()
        settle()
        device.executeShellCommand("screencap -p $outDir/$name.png")
    }

    /** Reloj fijo, batería llena y sin notificaciones en la barra de estado para que las capturas sean estables. */
    private fun prepararDispositivo() {
        device.executeShellCommand("rm -rf $outDir")
        device.executeShellCommand("mkdir -p $outDir")
        device.executeShellCommand("settings put global sysui_demo_allowed 1")
        listOf(
            "enter",
            "clock -e hhmm 1000",
            "battery -e level 100 -e plugged false",
            "network -e wifi show -e level 4",
            "notifications -e visible false"
        ).forEach { device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command $it") }
    }

    /** Una foto de ejemplo dibujada en código (degradado, sol y cerros), para no depender de archivos externos. */
    private fun fotoDeEjemplo(): String {
        val bitmap = Bitmap.createBitmap(900, 506, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, 506f, 0xFF6EC6F5.toInt(), 0xFFFFE3B0.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 900f, 506f, paint)
        paint.shader = null
        paint.color = 0xFFFFD36B.toInt()
        canvas.drawCircle(680f, 150f, 70f, paint)
        paint.color = 0xFF3FA796.toInt()
        canvas.drawPath(Path().apply { moveTo(0f, 506f); lineTo(0f, 330f); quadTo(250f, 220f, 520f, 360f); lineTo(900f, 300f); lineTo(900f, 506f); close() }, paint)
        paint.color = 0xFF2B7A78.toInt()
        canvas.drawPath(Path().apply { moveTo(0f, 506f); lineTo(0f, 420f); quadTo(300f, 350f, 600f, 440f); lineTo(900f, 410f); lineTo(900f, 506f); close() }, paint)
        val file = File(context.filesDir, "foto_ejemplo.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return android.net.Uri.fromFile(file).toString()
    }

    /** Todo en una sola transacción: la pantalla se actualiza una vez y los logros salen en un solo aviso. */
    private fun sembrar(db: AppDatabase) = runBlocking {
        val dao = db.dao()
        val now = System.currentTimeMillis()
        db.withTransaction {
            dao.upsertPool(PoolState(cash = 130_000.0, digital = 270_000.0))

            listOf(
                Meta(name = "Audífonos", goal = 150_000.0, category = "Tecnología"),
                Meta(name = "Viaje a la playa", goal = 1_200_000.0, deadlineMillis = now + 45 * day, category = "Viajes", imageUri = fotoDeEjemplo()),
                Meta(name = "Notebook", goal = 900_000.0, deadlineMillis = now - 2 * day, category = "Tecnología"),
                Meta(name = "Bicicleta", goal = 650_000.0, category = "Deporte")
            ).forEachIndexed { i, m -> dao.insertMeta(m.copy(position = i)) }

            dao.insertPurchased(Purchased(name = "Mouse inalámbrico", goal = 25_000.0, boughtAtMillis = now - 9 * day, category = "Tecnología"))
            dao.insertPurchased(Purchased(name = "Mochila", goal = 45_000.0, boughtAtMillis = now - 20 * day, category = "Viajes"))

            // Depósitos hoy, ayer y anteayer → racha de 3 días
            listOf(
                Movement(kind = MovementKind.DEPOSIT, amount = 50_000.0, moneyType = MoneyType.EFECTIVO, tsMillis = now - 60_000),
                Movement(kind = MovementKind.DEPOSIT, amount = 120_000.0, moneyType = MoneyType.DIGITAL, tsMillis = now - day),
                Movement(kind = MovementKind.TRANSFORM, amount = 30_000.0, moneyType = MoneyType.EFECTIVO, label = "Efectivo → Digital", tsMillis = now - day - 3_600_000),
                Movement(kind = MovementKind.DEPOSIT, amount = 80_000.0, moneyType = MoneyType.EFECTIVO, tsMillis = now - 2 * day),
                Movement(kind = MovementKind.DEPOSIT, amount = 90_000.0, moneyType = MoneyType.DIGITAL, tsMillis = now - 12 * day),
                Movement(kind = MovementKind.WITHDRAW, amount = 10_000.0, moneyType = MoneyType.EFECTIVO, tsMillis = now - 5 * day),
                Movement(kind = MovementKind.PURCHASE, amount = 25_000.0, label = "Mouse inalámbrico", tsMillis = now - 9 * day)
            ).forEach { dao.insertMovement(it) }
        }
    }
}
