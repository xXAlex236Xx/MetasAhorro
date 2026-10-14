<!-- AUTO-GENERADO por tools/wiki/WikiGenerator.java. No editar a mano. -->
# Lógica de negocio

> Página generada automáticamente desde el código. Para cambiarla, mejora el KDoc del código y corre `gradlew generateWiki`.

Operaciones sobre metas y dinero. Las que tocan el pozo corren dentro de una transacción de Room y leen el saldo actual de la base.

## MetasRepository

Código: [`MetasRepository.kt`](../../app/src/main/java/com/alex/metasahorro/repository/MetasRepository.kt)

Todas las operaciones que tocan el pozo corren dentro de una transacción de Room y leen el pozo actual de la base (no un valor viejo de la UI), así dos acciones rápidas seguidas no se pisan y un fallo a medias no deja datos inconsistentes.

### `addMeta`

Crea una meta nueva al final de la lista (la posición se calcula como la última + 1).

```kotlin
suspend fun addMeta(
    name: String,
    imageUri: String?,
    goal: Double,
    deadlineMillis: Long?,
    category: String? = null
)
```

### `updateMeta`

Guarda los cambios de una meta existente (nombre, precio, foto, fecha límite o categoría).

```kotlin
suspend fun updateMeta(meta: Meta)
```

### `deleteMeta`

Borra una meta. No toca el pozo ni el historial. Devuelve cómo deshacerlo.

```kotlin
suspend fun deleteMeta(meta: Meta): UndoAction
```

### `moveMeta`

Intercambia la posición de una meta con su vecina (`direction` = -1 sube, +1 baja).

```kotlin
suspend fun moveMeta(current: List<Meta>, meta: Meta, direction: Int)
```

### `registrar`

Suma dinero al pozo (efectivo o digital) y lo anota como `DEPOSIT` en el historial. Cuenta para la racha. Null si el monto no es válido.

```kotlin
suspend fun registrar(amount: Double, type: MoneyType): UndoAction?
```

### `restar`

Descuenta dinero del tipo indicado y lo anota como `WITHDRAW`. Devuelve null si no hay saldo suficiente.

```kotlin
suspend fun restar(amount: Double, type: MoneyType): UndoAction?
```

### `transformar`

Convierte dinero de efectivo a digital (o al revés) sin cambiar el total. Anota un `TRANSFORM` (con el tipo de origen en `moneyType`). Devuelve null si no hay saldo.

```kotlin
suspend fun transformar(amount: Double, from: MoneyType): UndoAction?
```

### `marcarComprado`

Marca una meta como comprada: descuenta su precio del pozo (repartido en proporción efectivo/digital), la archiva en `purchased` (con su categoría), anota un `PURCHASE` y la borra de las activas. Devuelve null (sin tocar nada) si el pozo no alcanza para el precio de la meta.

```kotlin
suspend fun marcarComprado(meta: Meta): UndoAction?
```

### `undo`

Deshace una operación. Devuelve false si ya no se puede (por ejemplo, el dinero que habría que quitar ya se gastó).

```kotlin
suspend fun undo(action: UndoAction): Boolean
```

### `revertMovement`

Borra un depósito, retiro o transformación del historial y revierte su efecto en el pozo. Falla (false) si es una compra, si no existe, o si revertirlo dejaría un saldo negativo.

```kotlin
suspend fun revertMovement(movementId: Long): Boolean
```

### `unlockAchievements`

Guarda los logros recién alcanzados. Devuelve solo los que de verdad eran nuevos (los repetidos se ignoran).

```kotlin
suspend fun unlockAchievements(reached: Collection<Achievement>): List<Achievement>
```

### `savedFor`

Cuánto lleva ahorrado una meta: el total del pozo, con tope en el precio de la meta (no se reparte entre metas).

```kotlin
fun savedFor(meta: Meta, pool: PoolState): Double
```

## MoneyMath

Código: [`MoneyMath.kt`](../../app/src/main/java/com/alex/metasahorro/repository/MoneyMath.kt)

Cuánto ahorrar por día y por semana para llegar a la fecha límite, y cuántos días quedan.

Un punto del gráfico: el total ahorrado al cierre de ese día.

### `round2`

Redondea a centavos para que las proporciones no arrastren decimales basura.

```kotlin
fun round2(value: Double): Double
```

### `splitPurchase`

Reparte el precio de una compra entre efectivo y digital en proporción a lo que hay en cada uno. Devuelve (efectivo a restar, digital a restar).

```kotlin
fun splitPurchase(cash: Double, digital: Double, goal: Double): Pair<Double, Double>
```

### `computeStreak`

Días seguidos con al menos un depósito. Si hoy todavía no hay depósito, la racha de ayer se conserva (se pierde recién cuando pasa un día completo sin depositar).

```kotlin
fun computeStreak(
    depositMillis: List<Long>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): Int
```

### `suggestedContribution`

Aporte necesario para juntar lo que falta antes de la fecha límite. Devuelve null si la meta no tiene fecha, la fecha ya pasó o ya no falta nada.

```kotlin
fun suggestedContribution(remaining: Double, deadlineMillis: Long?, nowMillis: Long): Contribution?
```

### `estimateDailyRate`

Ritmo de ahorro neto por día (depósitos menos retiros) en los últimos 7 a 30 días, según cuánto historial haya. Las compras no cuentan: son gasto planeado, no un cambio de hábito.

```kotlin
fun estimateDailyRate(movements: List<Movement>, nowMillis: Long): Double
```

### `estimateCompletion`

Fecha estimada para juntar `remaining` al ritmo dado. Null si ya está, el ritmo no es positivo o tardaría más de 10 años.

```kotlin
fun estimateCompletion(remaining: Double, dailyRate: Double, nowMillis: Long): Long?
```

### `balanceSeries`

Serie diaria del total ahorrado para el gráfico. `rangeDays` null = desde el primer movimiento. El último punto se ancla al total real del pozo (`currentTotal`) para que el gráfico termine exactamente donde está el saldo. Sin movimientos devuelve una lista vacía.

```kotlin
fun balanceSeries(
    movements: List<Movement>,
    currentTotal: Double,
    rangeDays: Int?,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): List<BalancePoint>
```

## Achievements

Código: [`Achievements.kt`](../../app/src/main/java/com/alex/metasahorro/repository/Achievements.kt)

Logros que se desbloquean solos al cumplir su condición. El nombre del enum es el id que se guarda en Room.

Datos de la app que miran las condiciones de los logros.

### `achievementsReached`

Todos los logros cuya condición se cumple con estos datos (incluye los ya desbloqueados antes).

```kotlin
fun achievementsReached(s: AchievementStats): Set<Achievement>
```

## ReminderPlanner

Código: [`ReminderPlanner.kt`](../../app/src/main/java/com/alex/metasahorro/repository/ReminderPlanner.kt)

Una meta con fecha límite a pocos días de vencer.

Qué avisar hoy: `streakAtRisk` es la racha que se perdería si hoy no se registra nada (null si no aplica), `askToSave` invita a ahorrar cuando no hay racha, y `deadlines` lista las metas por vencer.

### `millisUntilNext`

Milisegundos hasta la próxima vez que el reloj marque `hour`:00 (siempre en el futuro; si ya pasó hoy, es mañana).

```kotlin
fun millisUntilNext(hour: Int, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long
```

### `planReminders`

Decide qué avisos corresponden ahora según los ajustes del usuario. Pura: no toca notificaciones.

```kotlin
fun planReminders(
    nowMillis: Long,
    depositMillis: List<Long>,
    metas: List<Meta>,
    totalSaved: Double,
    settings: ReminderSettings,
    zone: ZoneId = ZoneId.systemDefault()
): ReminderPlan
```

## Reminders

Código: [`Reminders.kt`](../../app/src/main/java/com/alex/metasahorro/notifications/Reminders.kt)

Programa y muestra los recordatorios. Un trabajo periódico de WorkManager corre una vez al día cerca de la hora elegida (Android puede retrasarlo unos minutos para ahorrar batería) y decide qué avisar con `planReminders`.

### `sync`

Deja programado (o cancela) el aviso diario según los ajustes. Se llama al abrir la app y al cambiar los ajustes.

```kotlin
fun sync(context: Context, settings: ReminderSettings)
```

### `show`

Muestra las notificaciones del plan. No hace nada si el usuario no dio permiso de notificaciones.

```kotlin
fun show(context: Context, plan: ReminderPlan)
```

## MetasViewModel

Código: [`MetasViewModel.kt`](../../app/src/main/java/com/alex/metasahorro/viewmodel/MetasViewModel.kt)

Una meta junto con cuánto lleva ahorrado y si ya está completa.

Todo lo que necesita la pantalla en un solo objeto: metas, comprados, historial, pozo, logros, ajustes y racha.

Combina los flujos de Room y DataStore en un único `uiState`, expone las acciones de la pantalla y emite [UiEvent].

### `completeOnboarding`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun completeOnboarding()
```

### `addMeta`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun addMeta(name: String, imageUri: String?, goal: Double, deadlineMillis: Long?, category: String?)
```

### `updateMeta`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun updateMeta(meta: Meta)
```

### `deleteMeta`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun deleteMeta(meta: Meta)
```

### `moveMeta`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun moveMeta(meta: Meta, direction: Int)
```

### `registrar`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun registrar(amount: Double, type: MoneyType)
```

### `restar`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun restar(amount: Double, type: MoneyType)
```

### `transformar`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun transformar(amount: Double, from: MoneyType)
```

### `marcarComprado`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun marcarComprado(meta: Meta)
```

### `undo`

Deshace una operación reciente (el botón "Deshacer" del aviso).

```kotlin
fun undo(action: UndoAction)
```

### `deleteMovement`

Borra un movimiento del historial revirtiendo su efecto en el pozo.

```kotlin
fun deleteMovement(movement: Movement)
```

### `setTheme`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun setTheme(theme: AccentTheme)
```

### `setThemeMode`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun setThemeMode(mode: ThemeMode)
```

### `setDynamicColor`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun setDynamicColor(enabled: Boolean)
```

### `setQuickAmounts`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun setQuickAmounts(amounts: List<Int>)
```

### `setDailyReminder`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun setDailyReminder(enabled: Boolean)
```

### `setReminderHour`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun setReminderHour(hour: Int)
```

### `setDeadlineReminder`

_Sin documentación en el código (agregar KDoc)._

```kotlin
fun setDeadlineReminder(enabled: Boolean)
```
