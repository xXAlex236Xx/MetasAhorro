package com.alex.metasahorro.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val clFormat = NumberFormat.getInstance(Locale("es", "CL"))

fun fmtMoney(n: Double): String = "$" + clFormat.format(Math.round(n))

fun fmtDateShort(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("es", "CL")).format(Date(millis))

fun fmtDateTime(millis: Long): String =
    SimpleDateFormat("dd/MM · HH:mm", Locale("es", "CL")).format(Date(millis))
