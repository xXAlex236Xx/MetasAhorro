<!-- AUTO-GENERADO por tools/wiki/WikiGenerator.java. No editar a mano. -->
# Wiki de Mis Metas

> Página generada automáticamente desde el código. Para cambiarla, mejora el KDoc del código y corre `gradlew generateWiki`.

Documentación interna de la app Android **Mis Metas**: arquitectura, datos, pantallas y lógica.

**Punto de entrada.** Única actividad de la app: muestra el splash mientras carga las preferencias, aplica el tema elegido y decide entre el onboarding (primera vez) y la pantalla principal. ([`MainActivity.kt`](../../app/src/main/java/com/alex/metasahorro/MainActivity.kt))

<img src="screenshots/MainScreen.png" alt="MainScreen" width="280">

## Resumen

| Dato | Valor |
| --- | --- |
| Aplicación (applicationId) | `com.alex.metasahorro` |
| Versión | 1.1 (código 2) |
| SDK mínimo / objetivo / compilación | 26 / 34 / 34 |
| Archivos Kotlin (main) | 40 (3776 líneas) |
| Pantallas y componentes públicos | 23 |
| Tablas de Room | 5 |
| Tests unitarios / instrumentados | 48 / 23 |

## Contenido

- [Arquitectura](Arquitectura.md): capas, dependencias entre paquetes y manifest.
- [Modelo de datos](Modelo-de-datos.md): tablas de Room, enums, DataStore y consultas.
- [Pantallas](Pantallas.md): cada vista con su captura, parámetros y cómo se abre.
- [Lógica de negocio](Logica-de-negocio.md): operaciones del repositorio, ViewModel y cálculos.
- [Tema](Tema.md): colores y temas de acento.
- [Dependencias](Dependencias.md): librerías y versiones.
- [Pruebas](Pruebas.md): tests y su último resultado.

## Cómo se genera este wiki

- **Texto:** `gradlew generateWiki` corre `tools/wiki/WikiGenerator.java`, que lee el código, el KDoc, el esquema de Room, el manifest y Gradle. No requiere emulador.
- **Capturas:** `tools/wiki/capture-screenshots.ps1` levanta el emulador, corre la prueba instrumentada `WikiScreenshotsTest` (carga datos de ejemplo y recorre la app) y luego regenera el wiki.
- **Regla:** no se edita nada en `docs/wiki/` a mano. Si una explicación está floja, se mejora el KDoc del código.
