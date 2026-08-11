# Mis Metas — App Android

App personal de metas de ahorro. 100% local (Room), sin backend ni internet.

## Cómo abrirlo
1. Abre **Android Studio** (versión reciente, Hedgehog o superior).
2. `File > Open...` y selecciona la carpeta `MetasAhorro` (la que contiene `settings.gradle.kts`).
3. Espera a que Android Studio sincronice Gradle. **La primera vez puede pedir descargar el Gradle Wrapper** — dale "OK / Sync Now" cuando aparezca (no incluí el binario `gradle-wrapper.jar` en el zip para no pesar innecesariamente).
4. Corre la app en un emulador o tu teléfono conectado (▶️ Run).

## Qué incluye
- **Kotlin + Jetpack Compose + Material 3**, todo en un solo módulo (`app`).
- **Room** para guardar todo localmente: metas activas, comprados, historial de movimientos y el "pozo" de dinero (efectivo/digital).
- **DataStore** para recordar el color de acento elegido.
- **Coil** para mostrar las fotos que subas a cada meta.
- Misma lógica que el prototipo HTML: cada meta compara su precio contra el total ahorrado (sin repartir entre metas), reordenar con flechas, archivo de comprados, racha de ahorro, confeti al comprar, fecha límite opcional y 4 temas de color.

## Estructura
```
app/src/main/java/com/alex/metasahorro/
  data/         -> entidades Room, DAO, base de datos, DataStore del tema
  repository/   -> lógica de negocio (registrar, restar, marcar comprado, etc.)
  viewmodel/    -> MetasViewModel, combina todo en un solo UiState
  ui/           -> MainScreen + componentes (tarjetas, sheets, confeti)
```

## Próximos pasos sugeridos
- Ícono de la app (por ahora usa el ícono por defecto de Android).
- Backup/exportar datos (hoy todo vive solo en el teléfono).
- Notificaciones para las metas con fecha límite próxima.
