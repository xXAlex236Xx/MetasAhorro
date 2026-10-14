package com.alex.metasahorro.ui.components

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Texto con plural desde `strings.xml` (1 día / 2 días). `args` se insertan en el texto, normalmente la cantidad misma. */
@Composable
fun pluralText(@PluralsRes id: Int, count: Int, vararg args: Any): String =
    LocalContext.current.resources.getQuantityString(id, count, *args)
