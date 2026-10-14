package com.alex.metasahorro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.R
import com.alex.metasahorro.repository.BalancePoint
import com.alex.metasahorro.ui.theme.BorderColor
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.util.fmtDateShort
import com.alex.metasahorro.util.fmtMoney

/**
 * Gráfico de línea del total ahorrado día a día (curva con área sombreada). Dibuja solo con Canvas, sin
 * librerías. Bajo el gráfico muestra la fecha inicial y final; el resumen accesible describe el cambio del período.
 */
@Composable
fun SavingsChart(points: List<BalancePoint>, modifier: Modifier = Modifier) {
    val first = points.first()
    val last = points.last()
    val summary = stringResource(
        R.string.chart_summary, fmtMoney(first.balance), fmtMoney(last.balance),
        fmtDateShort(first.dayStartMillis), fmtDateShort(last.dayStartMillis)
    )
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = BorderColor

    Column(modifier = modifier.semantics { contentDescription = summary }) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val minV = points.minOf { it.balance }
            val maxV = points.maxOf { it.balance }
            val span = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
            val pad = 8.dp.toPx()
            val w = size.width
            val h = size.height - pad * 2

            fun x(i: Int) = if (points.size == 1) w / 2 else w * i / (points.size - 1)
            fun y(v: Double) = pad + h - (((v - minV) / span) * h).toFloat()

            // líneas guía
            repeat(3) { i ->
                val gy = pad + h * i / 2f
                drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1.dp.toPx())
            }

            val line = Path().apply {
                points.forEachIndexed { i, p -> if (i == 0) moveTo(x(i), y(p.balance)) else lineTo(x(i), y(p.balance)) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(x(points.lastIndex), size.height)
                lineTo(x(0), size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f))))
            drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(x(points.lastIndex), y(last.balance)))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmtDateShort(first.dayStartMillis), fontSize = 10.5.sp, color = TextMutedColor)
            Text(fmtDateShort(last.dayStartMillis), fontSize = 10.5.sp, color = TextMutedColor)
        }
    }
}
