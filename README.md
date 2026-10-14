# Mis Metas — App Android

App personal de metas de ahorro. 100% local (Room), sin backend ni internet.

<p align="center">
  <img src="docs/wiki/screenshots/MainScreen.png" alt="Pantalla principal" width="260">
  <img src="docs/wiki/screenshots/ItemMenuSheet.png" alt="Detalle de una meta" width="260">
  <img src="docs/wiki/screenshots/DetailsSheet.png" alt="Gráfico de ahorro" width="260">
</p>

> Si las capturas no aparecen, corre `tools\wiki\capture-screenshots.ps1` (ver [Documentación](#documentación)).

## Cómo funciona
- Hay **un solo pozo de dinero**, dividido en efectivo y digital. Se suma con **Registrar**, se descuenta con **Restar** y se mueve entre tipos con **Transformar**.
- Cada meta compara su precio contra el total del pozo (no se reparte entre metas): si el total alcanza, la meta queda lista.
- Una meta lista se marca como **comprada**: se descuenta su precio del pozo (en proporción efectivo/digital), pasa al archivo de comprados y cae confeti.

### Qué incluye
- **Pronóstico por meta:** cuánto ahorrar por semana o por día para llegar a la fecha límite, y qué día llegarías al ritmo de tus últimos días.
- **Gráfico del ahorro** (7, 30, 90 días o todo el historial) dentro de *Detalles*.
- **Deshacer:** registrar, restar, transformar, comprar y borrar una meta muestran un aviso con "Deshacer". Los movimientos del historial también se pueden borrar (se revierte su efecto).
- **Logros e insignias** que se desbloquean solos (primer ahorro, rachas, montos ahorrados, metas cumplidas...), con una vitrina propia.
- **Celebraciones:** confeti y aviso cuando una meta llega al 100%, al comprar y al desbloquear logros.
- **Categorías o etiquetas** por meta, con filtro en la lista.
- **Foto grande** de la meta en su detalle.
- **Montos rápidos** personalizables para el botón Registrar.
- **Recordatorios** opcionales: aviso diario a la hora que elijas (cuida tu racha) y aviso de metas por vencer. Piden el permiso de notificaciones al activarlos.
- **Tema claro, oscuro o del sistema**, color de acento a elegir y **colores dinámicos** (Material You, Android 12+).
- Racha de días seguidos, historial, reordenar con flechas y onboarding.

## Cómo abrirlo
1. Abre **Android Studio** (Hedgehog o superior) y elige `File > Open...` sobre la carpeta que contiene `settings.gradle.kts`.
2. Espera a que sincronice Gradle.
3. Corre la app en un emulador o en tu teléfono (▶️ Run).

Por línea de comandos (Windows): `gradlew.bat :app:assembleDebug`.

> **JDK:** el proyecto (Gradle 8.13 + AGP 8.13.2) necesita un JDK **17 o superior**, pero **no funciona con el 25**; usa uno entre **17 y 21**. Si tu JDK por defecto es más nuevo (p. ej. el de Android Studio puede ser 25), en *Settings → Build → Gradle* elige un JDK 21.

## Stack
Kotlin · Jetpack Compose · Material 3 · Room 2.6 (KSP) · DataStore · WorkManager · Coil. Un solo módulo (`app`), `minSdk` 26, `targetSdk` 34.

## Estructura
```
app/src/main/java/com/alex/metasahorro/
  data/           -> entidades Room, DAO, base de datos (con migraciones), DataStore (tema, ajustes, onboarding)
  repository/     -> lógica de negocio (registrar, restar, comprar, deshacer...), todo en transacciones de Room;
                     MoneyMath.kt, Achievements.kt y ReminderPlanner.kt tienen los cálculos puros
  notifications/  -> recordatorio diario (WorkManager) y notificaciones
  util/           -> formato de dinero y fechas
  viewmodel/      -> MetasViewModel: un solo UiState, eventos (avisos, celebraciones) y acciones
  ui/             -> MainScreen, OnboardingScreen, componentes (tarjetas, hojas, gráfico, confeti) y tema
app/src/main/res/values/strings.xml -> todos los textos (español; base para traducir)
app/schemas/      -> esquemas exportados de Room (versionarlos en git)
docs/wiki/        -> wiki interno AUTO-GENERADO (no editar a mano)
tools/wiki/       -> generador del wiki y script de capturas
```

## Documentación
El **wiki interno** está en [`docs/wiki/Home.md`](docs/wiki/Home.md): arquitectura, modelo de datos, pantallas con capturas, lógica de negocio, tema, dependencias y pruebas. Se genera solo desde el código; no se escribe a mano.

| Comando | Qué hace |
| --- | --- |
| `gradlew.bat :app:generateWiki` | Regenera el texto, tablas y diagramas del wiki. Rápido, sin emulador. |
| `tools\wiki\capture-screenshots.ps1` | Con el emulador encendido: corre todos los tests instrumentados, recorre la app con datos de ejemplo, toma las capturas y regenera el wiki. |

Para mejorar una explicación del wiki, mejora el KDoc del código y regenera.

**Capturas:** conviene abrir antes el emulador desde Android Studio (Device Manager → ▶) y esperar a ver la pantalla de inicio de Android. El script usa el que esté encendido; si no hay ninguno, intenta arrancar el primer AVD sin ventana, pero ese modo falló una vez en esta máquina. Mientras corre, no toques el emulador: se mueve solo. Cierra programas pesados (p. ej. juegos) para que arranque a tiempo.

## Pruebas
- **Unitarias** (JVM, rápidas): `gradlew.bat :app:testDebugUnitTest`. Cubren reparto de compras, racha, aporte sugerido, fecha estimada, serie del gráfico, logros, planificador de recordatorios y ajustes.
- **Instrumentadas** (necesitan emulador o teléfono): `gradlew.bat :app:connectedDebugAndroidTest`.
  - `MetasRepositoryTest`: la lógica de dinero (registrar, restar, transformar, comprar, deshacer, borrar movimientos, concurrencia) contra una base de datos Room real en memoria.
  - `RemindersTest`: que las notificaciones se publican de verdad (con el permiso concedido) y que el aviso diario se programa y cancela en WorkManager.
  - `MigrationTest`: que actualizar la base de la versión 1 a la 2 conserva los datos del usuario.
  - `WikiScreenshotsTest`: no verifica comportamiento; solo toma las capturas del wiki.

## Base de datos
Versión 2. Al cambiar una entidad: sube `version` en `AppDatabase`, agrega una `Migration`, versiona el nuevo `app/schemas/<n>.json` y agrega su test en `MigrationTest`. Sin eso, Room fallará al abrir la base ya instalada.

## Versiones
La versión de la app está en `app/build.gradle.kts` (`versionName` / `versionCode`). La más reciente va primero.

### 1.1 — 14/10/2026
**Nuevo**
- **Transformar:** convertir dinero de efectivo a digital (o al revés) sin cambiar el total.
- **Pronóstico por meta:** cuánto ahorrar por semana o por día para llegar a la fecha límite, y qué día llegarías al ritmo de tus últimos días.
- **Gráfico del ahorro** (7, 30, 90 días o todo el historial) en *Detalles*.
- **Deshacer** para registrar, restar, transformar, comprar y borrar una meta. Los movimientos del historial se pueden borrar (se revierte su efecto).
- **Logros e insignias** (11), con vitrina propia y aviso de celebración.
- **Más celebraciones:** confeti y aviso cuando una meta llega al 100%, al comprar y al desbloquear logros.
- **Categorías o etiquetas** por meta, con filtro en la lista.
- **Foto grande** de la meta en su detalle (y miniatura más grande en la lista).
- **Montos rápidos** personalizables.
- **Recordatorios** opcionales: aviso diario (cuida tu racha) y aviso de metas por vencer.
- **Tema claro, oscuro o del sistema** y **colores dinámicos** (Material You, Android 12+).

**Cambios**
- Borrar una meta ya no pide confirmación: se avisa con "Deshacer".
- El detalle de una meta completa también permite modificarla o borrarla (antes solo "Comprado").
- Base de datos versión 2 (agrega categorías y logros). Se actualiza sola y conserva tus datos.
- Gradle 8.13 y Android Gradle Plugin 8.13.2.

**Correcciones**
- Registrar, restar, transformar y comprar ahora son operaciones atómicas: dos toques rápidos ya no pierden una actualización ni dejan datos a medias.
- Un retiro o una transformación sin saldo ahora avisa en vez de cerrarse sin decir nada, y no se puede marcar como comprada una meta que el ahorro no alcanza.
- La racha conserva la del día anterior hasta que termina el día (antes mostraba 0 desde la medianoche).
- Las fotos de las metas usan un permiso persistente para que no se pierdan al reiniciar el teléfono.
- Los montos se redondean a centavos en los repartos de compra.
- Íconos y flechas con área táctil más grande y descripciones para lectores de pantalla.

**Internas**
- Textos extraídos a `strings.xml` (listos para traducir).
- 71 pruebas automáticas: 48 unitarias y 23 en emulador (dinero, migración de la base de datos y notificaciones).
- Wiki interno auto-generado en `docs/wiki`, esquemas de Room exportados en `app/schemas` y `CLAUDE.md`.

### 1.0 — 11/08/2026
Primera versión, 100% local y sin internet.
- Metas de ahorro con nombre, precio, foto y fecha límite opcionales; reordenar con las flechas.
- Un solo pozo de dinero dividido en efectivo y digital, con **Registrar** y **Restar**.
- Cada meta compara su precio contra el total ahorrado (sin repartir entre metas) y muestra su avance y lo que falta.
- Marcar una meta como **comprada**: se descuenta del ahorro, pasa al archivo de comprados y cae confeti.
- Racha de días seguidos ahorrando, historial de movimientos y detalle del ahorro por tipo.
- 4 colores de acento, bienvenida (onboarding) y pantalla "Acerca de".
- Hecha con Kotlin, Jetpack Compose, Material 3, Room, DataStore y Coil.

## Próximos pasos sugeridos
- Exportar/importar datos (hoy todo vive solo en el teléfono).
- Traducir la app a otros idiomas (los textos ya están en `strings.xml`; falta agregar `values-<idioma>` y que el formato de dinero siga el idioma).
- Activar R8 (`isMinifyEnabled`) probando antes un build release.
