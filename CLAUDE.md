# CLAUDE.md

App Android "Mis Metas" (paquete `com.alex.metasahorro`): metas de ahorro 100% locales. Kotlin, Jetpack Compose, Material 3, Room, DataStore, WorkManager, Coil. Un solo módulo (`app`). Textos de la UI, KDoc y documentación en **español**.

## Comandos (Windows, desde la raíz)
```
gradlew.bat :app:assembleDebug            # compilar
gradlew.bat :app:testDebugUnitTest        # tests unitarios (JVM)
gradlew.bat :app:connectedDebugAndroidTest   # tests instrumentados (necesita emulador encendido)
gradlew.bat :app:generateWiki             # regenerar docs/wiki (sin emulador)
powershell -File tools\wiki\capture-screenshots.ps1   # todos los tests instrumentados + capturas + regenerar wiki
```
- Para correr solo algunas clases instrumentadas: `-Pandroid.testInstrumentationRunnerArguments.class=com.alex.metasahorro.MetasRepositoryTest,...`
- **JDK 17–21 obligatorio.** El proyecto usa Gradle 8.13 + AGP 8.13.2 (los subió Android Studio el 14/10/2026), que no soportan JDK 25 (el JBR de Android Studio puede serlo). Usar `JAVA_HOME=C:\Users\eljug\.jdks\jbr-21.0.11` o similar.
- **Emulador:** AVD `Pixel_10_Pro` (Android 17 / API 37.1). El usuario lo abre desde Android Studio y yo lo uso por `adb` (está en `C:\Users\eljug\AppData\Local\Android\Sdk\platform-tools`). Comprobar con `adb devices` una sola vez: si dice `offline`, aún está arrancando; no reintentar en bucle ni lanzar otro emulador sin avisar. Si arranca mal, suele ser por falta de recursos del equipo (cerrar juegos).
- Espresso está fijado en 3.7.0 en `app/build.gradle.kts`: la versión que trae Compose (3.5) falla en Android 16+ con `InputManager.getInstance`.
- Sin red, Gradle puede no resolver dependencias nuevas; `--offline` solo sirve con lo ya cacheado.
- Git puede dar "dubious ownership" en esta carpeta: lo resuelve el usuario con `git config --global --add safe.directory`. No cambiar la config de git sin que lo pida.
- Herramienta Bash: los heredocs largos con comillas pueden fallar; para archivos de código usar la herramienta de escritura.

## Arquitectura
`ui` → `viewmodel` → `repository` → `data` (DAO/Room). `notifications` (WorkManager) lee `data` y `repository`; `util` tiene el formato de dinero y fechas. Detalle y diagramas en `docs/wiki/Arquitectura.md`.
- `MetasViewModel` combina Room + DataStore en un único `MetasUiState` y emite `UiEvent` (avisos con "Deshacer", meta lista, logros nuevos). La UI no habla con el DAO.
- `MainScreen` maneja todas las vistas secundarias como hojas inferiores con un enum `SheetType`.
- Tema: los colores propios (`SurfaceColor`, `TextMutedColor`, `CashColor`...) son valores **composables** que dependen del tema claro/oscuro/dinámico (`LocalAppColors`). No usar `Color.White`/`Color.Gray` fijos en la UI: usar `MaterialTheme.colorScheme` o estos colores.

## Reglas que no hay que romper
1. **Toda operación que toque el pozo** (`registrar`, `restar`, `transformar`, `marcarComprado`, `undo`, `revertMovement`) vive en `MetasRepository`, dentro de `db.withTransaction`, y **lee el pozo de la base** (`dao.getPool()`), nunca el valor que tenía la UI. Es lo que evita carreras y datos a medias.
2. Las operaciones que pueden fallar (sin saldo, compra sin fondos) devuelven `null`/`false` y la UI avisa; no fallar en silencio. Las que se pueden revertir devuelven un `UndoAction`.
3. Los cálculos puros (reparto de compra, racha, pronóstico, serie del gráfico, logros, planificador de recordatorios) van en `repository/` (`MoneyMath.kt`, `Achievements.kt`, `ReminderPlanner.kt`) y tienen test en `app/src/test`. Si cambias esa lógica, agrega o ajusta tests.
4. **Cambios de esquema de Room:** subir `version` en `AppDatabase`, escribir una `Migration`, dejar el nuevo `app/schemas/<n>.json` versionado y agregar su test en `MigrationTest`. No usar `fallbackToDestructiveMigration`: son datos de dinero del usuario.
5. **Textos:** nunca en el código; van en `res/values/strings.xml` (usar `stringResource`, plurales con `pluralText`). Excepción: el `label` de los movimientos de transformación que se guarda en la base.
6. Los montos se muestran con `fmtMoney` (formato es-CL). Los campos de monto solo aceptan dígitos.
7. **Accesibilidad:** íconos clicables con `contentDescription` (la prueba de capturas los usa para navegar), áreas táctiles de al menos 40 dp, y `Role` en los `clickable` personalizados.
8. Los recordatorios solo se programan si el usuario los activa, y activarlos pide el permiso `POST_NOTIFICATIONS`.

## Regla: mantener la documentación al día
Todo cambio que lo amerite debe actualizar los documentos **en el mismo trabajo**, cada uno donde corresponda:
- **Código/KDoc → wiki:** cambios en pantallas, entidades, DAO, repositorio, ViewModel, tema o dependencias → actualizar el KDoc y regenerar (`gradlew.bat :app:generateWiki`; con capturas si cambió la UI).
- **`README.md`:** cambios en cómo se abre, compila o usa la app, en su funcionamiento visible, estructura de carpetas, comandos o pasos sugeridos. **Cada versión nueva** (al subir `versionName`/`versionCode` en `app/build.gradle.kts`) lleva su entrada en la sección "Versiones" del README, con Nuevo / Cambios / Correcciones, y la más reciente arriba.
- **`CLAUDE.md`:** cambios en comandos, reglas de arquitectura, convenciones, requisitos del entorno o limitaciones conocidas (incluido borrar de "Estado conocido" lo que se resuelva).
- **Generador/test de capturas:** si cambia la estructura del código que parsea o se agrega una pantalla, ajustar `WikiGenerator.java` o `WikiScreenshotsTest.kt`.
No hace falta tocar un documento si el cambio no le afecta; lo que no se debe hacer es dejar uno desactualizado. Antes de cerrar una tarea, repasar qué documentos tocó.

## Documentación auto-generada
- `docs/wiki/` **se genera; no se edita a mano.** Cualquier cambio manual se pierde al regenerar.
- La fuente de las explicaciones es el **KDoc del código**. Al agregar o cambiar una pantalla, función del repositorio, entidad o enum, escribe/actualiza su KDoc (una o dos frases, en español) y corre `gradlew.bat :app:generateWiki`.
- El generador es `tools/wiki/WikiGenerator.java` (Java de un solo archivo, sin dependencias). Lee los `.kt`, `app/schemas`, el manifest, Gradle y los resultados de tests (unitarios e instrumentados). Si agregas un paquete nuevo, añade su descripción en `LAYER_DESC`; si agregas un archivo con lógica que debe aparecer en la página de lógica de negocio, añádelo a la lista de `logica()`. El DAO se lee línea a línea: cada `@Query("...")` debe ir completo en una línea.
- Las capturas salen de `app/src/androidTest/.../WikiScreenshotsTest.kt`, que siembra datos de ejemplo y recorre la app. El nombre de cada captura es el del composable (`DepositSheet.png`; variantes con `__sufijo`). Si agregas una pantalla, agrega su paso a ese test.
- `README.md` y este archivo sí se escriben a mano.

## Estado conocido
- Los montos son `Double` (los campos solo aceptan enteros, así que hoy no hay decimales reales). Pasar a centavos enteros exigiría migración.
- `isMinifyEnabled = false` en release; activarlo requiere probar antes un build release.
- La app tiene solo textos en español; el formato de dinero/fechas está fijo en es-CL. Para traducir: agregar `values-<idioma>/strings.xml` y hacer que `util/Format.kt` siga el idioma.
- Los recordatorios usan WorkManager periódico (24 h): Android puede retrasarlos unos minutos. `RemindersTest` comprueba que las notificaciones se publican y que el trabajo se programa; la ejecución real del trabajo a la hora elegida no está probada de punta a punta.
- `docs/wiki/screenshots/*.png` ya vienen reducidas a 540 px de ancho por el generador; no subir capturas crudas (`build/wiki-screenshots` está ignorado).
