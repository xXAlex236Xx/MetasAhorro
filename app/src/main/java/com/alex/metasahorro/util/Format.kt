package com.alex.metasahorro.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val clFormat = NumberFormat.getInstance(Locale("es", "CL"))

/** Dinero en pesos chilenos, sin decimales y con punto de miles: `$150.000`. */
fun fmtMoney(n: Double): String = "$" + clFormat.format(Math.round(n))

/** Número entero con punto de miles (sin el signo `$`): `150.000`. */
fun fmtNumber(n: Int): String = clFormat.format(n)

/** Fecha corta `dd/MM/yyyy`. */
fun fmtDateShort(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("es", "CL")).format(Date(millis))

/** Fecha y hora `dd/MM · HH:mm`. */
fun fmtDateTime(millis: Long): String =
    SimpleDateFormat("dd/MM · HH:mm", Locale("es", "CL")).format(Date(millis))
