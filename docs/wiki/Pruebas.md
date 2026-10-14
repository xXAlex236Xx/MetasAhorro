<!-- AUTO-GENERADO por tools/wiki/WikiGenerator.java. No editar a mano. -->
# Pruebas

> Página generada automáticamente desde el código. Para cambiarla, mejora el KDoc del código y corre `gradlew generateWiki`.

Tests unitarios (JVM) e instrumentados (emulador o dispositivo).

## Unitarios (`app/src/test`)

**AchievementsTest** ([`AchievementsTest.kt`](../../app/src/test/java/com/alex/metasahorro/repository/AchievementsTest.kt))

| Test | Último resultado |
| --- | --- |
| `sinActividadNoHayLogros` | ✅ |
| `primerDepositoDesbloqueaPrimerPaso` | ✅ |
| `lasRachasSeAcumulan` | ✅ |
| `losMontosSeDesbloqueanEnSusUmbrales` | ✅ |
| `comprasTransformacionesYCategorias` | ✅ |
| `cadaLogroTieneIdUnicoParaGuardarlo` | ✅ |

**ForecastTest** ([`ForecastTest.kt`](../../app/src/test/java/com/alex/metasahorro/repository/ForecastTest.kt))

| Test | Último resultado |
| --- | --- |
| `aporte_repartidoEntreLosDiasQueQuedan` | ✅ |
| `aporte_conMenosDeUnaSemanaLaSemanaCuentaComoUna` | ✅ |
| `aporte_sinFechaFechaPasadaOSinFaltanteEsNulo` | ✅ |
| `ritmo_sinMovimientosEsCero` | ✅ |
| `ritmo_depositosMenosRetirosEntreLosDiasDeHistorial` | ✅ |
| `ritmo_lasComprasNoCuentan` | ✅ |
| `ritmo_conPocoHistorialLaVentanaMinimaEsSieteDias` | ✅ |
| `ritmo_laVentanaMaximaEsTreintaDiasYIgnoraLoAntiguo` | ✅ |
| `fechaEstimada_faltanteDivididoPorRitmo` | ✅ |
| `fechaEstimada_redondeaHaciaArriba` | ✅ |
| `fechaEstimada_nulaSiYaEstaSinRitmoOMasDeDiezAnios` | ✅ |
| `serie_sinMovimientosEstaVacia` | ✅ |
| `serie_todoElHistorialArrancaEnCeroYSumaCadaDia` | ✅ |
| `serie_conRangoPartedelSaldoAcumuladoAnterior` | ✅ |
| `serie_elUltimoPuntoSeAnclaAlTotalReal` | ✅ |
| `serie_unMovimientoConFechaFuturaCuentaComoHoy` | ✅ |

**MoneyMathTest** ([`MoneyMathTest.kt`](../../app/src/test/java/com/alex/metasahorro/repository/MoneyMathTest.kt))

| Test | Último resultado |
| --- | --- |
| `split_proporcionalALaMezcla` | ✅ |
| `split_soloEfectivo` | ✅ |
| `split_poolVacioNoRestaNada` | ✅ |
| `split_sumaExactamenteElPrecio` | ✅ |
| `streak_sinDepositosEsCero` | ✅ |
| `streak_cuentaDiasConsecutivosHastaHoy` | ✅ |
| `streak_conservaLaRachaDeAyerSiHoyAunNoHayDeposito` | ✅ |
| `streak_sePierdeTrasUnDiaCompletoSinDepositar` | ✅ |
| `streak_variosDepositosEnUnDiaCuentanUnaVez` | ✅ |
| `streak_unHuecoCortaLaRacha` | ✅ |

**ReminderPlannerTest** ([`ReminderPlannerTest.kt`](../../app/src/test/java/com/alex/metasahorro/repository/ReminderPlannerTest.kt))

| Test | Último resultado |
| --- | --- |
| `siYaRegistroHoyNoSeAvisa` | ✅ |
| `sinRegistroHoyYConRachaSeAvisaQueSePierde` | ✅ |
| `sinRegistroHoyNiRachaSeInvitaAAhorrar` | ✅ |
| `conElAvisoDiarioApagadoNoSeAvisa` | ✅ |
| `avisaLasMetasQueVencenEnTresDiasOMenos` | ✅ |
| `noAvisaMetasCompletasVencidasNiSinFecha` | ✅ |
| `conElAvisoDeFechasApagadoNoSeAvisa` | ✅ |
| `faltanLasHorasHastaLaHoraDeHoy` | ✅ |
| `siLaHoraYaPasoEsManana` | ✅ |
| `justoALaHoraEsDentroDeUnDia` | ✅ |

**SettingsLogicTest** ([`SettingsLogicTest.kt`](../../app/src/test/java/com/alex/metasahorro/repository/SettingsLogicTest.kt))

| Test | Último resultado |
| --- | --- |
| `montosRapidos_sinDatoUsaLosDeFabrica` | ✅ |
| `montosRapidos_valoresInvalidosUsanLosDeFabrica` | ✅ |
| `montosRapidos_leeLosValidosYSaltaLosMalos` | ✅ |
| `montosRapidos_soloSeGuardanLosPrimerosTres` | ✅ |
| `categoria_quitaEspaciosYVaciosSonNulos` | ✅ |
| `categoria_seCortaAlLargoMaximo` | ✅ |

## Instrumentados (`app/src/androidTest`)

**MetasRepositoryTest** ([`MetasRepositoryTest.kt`](../../app/src/androidTest/java/com/alex/metasahorro/MetasRepositoryTest.kt))

| Test | Último resultado |
| --- | --- |
| `registrar_sumaAlTipoCorrectoYAnotaElMovimiento` | ✅ |
| `registrar_rechazaMontosInvalidos` | ✅ |
| `restar_descuentaYSinSaldoNoTocaNada` | ✅ |
| `registrar_dosVecesAlMismoTiempoNoPierdeNingunaActualizacion` | ✅ |
| `transformar_conservaElTotalYGuardaElOrigen` | ✅ |
| `transformar_sinSaldoEnElOrigenNoHaceNada` | ✅ |
| `marcarComprado_repartePorLaMezclaArchivaYBorraLaMeta` | ✅ |
| `marcarComprado_sinFondosNoTocaNada` | ✅ |
| `deshacer_unDepositoUnRetiroYUnaTransformacion` | ✅ |
| `deshacer_unaCompraReponeLaMetaElDineroYQuitaElArchivo` | ✅ |
| `deshacer_unaMetaBorradaLaDevuelveConSuMismoId` | ✅ |
| `revertMovement_noDejaSaldoNegativo` | ✅ |
| `revertMovement_lasComprasNoSePuedenBorrarDesdeElHistorial` | ✅ |
| `revertMovement_entiendeTransformacionesAntiguasSinOrigenGuardado` | ✅ |
| `categorias_seLimpianAlCrearYAlEditar` | ✅ |
| `moveMeta_intercambiaLasPosiciones` | ✅ |
| `logros_soloSeCuentanComoNuevosLaPrimeraVez` | ✅ |

**MigrationTest** ([`MigrationTest.kt`](../../app/src/androidTest/java/com/alex/metasahorro/MigrationTest.kt))

| Test | Último resultado |
| --- | --- |
| `migrar1a2_conservaLosDatosYAgregaLasColumnasNuevas` | ✅ |
| `migrar1a2_laBaseAbreConRoomYSePuedeUsar` | ✅ |

**RemindersTest** ([`RemindersTest.kt`](../../app/src/androidTest/java/com/alex/metasahorro/RemindersTest.kt))

| Test | Último resultado |
| --- | --- |
| `show_publicaElAvisoDeRachaYElDeMetaPorVencer` | ✅ |
| `show_conUnPlanVacioNoPublicaNada` | ✅ |
| `sync_programaElAvisoDiarioYLoCancelaAlApagarlo` | ✅ |

**WikiScreenshotsTest** ([`WikiScreenshotsTest.kt`](../../app/src/androidTest/java/com/alex/metasahorro/WikiScreenshotsTest.kt))

| Test | Último resultado |
| --- | --- |
| `capturarPantallas` | ✅ |

_✅ pasó · ❌ falló · — sin resultado (correr `gradlew :app:testDebugUnitTest` antes de generar el wiki)._
