package com.alex.metasahorro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.R
import com.alex.metasahorro.data.Movement
import com.alex.metasahorro.data.PoolState
import com.alex.metasahorro.repository.balanceSeries
import com.alex.metasahorro.ui.theme.BorderColor
import com.alex.metasahorro.ui.theme.CashColor
import com.alex.metasahorro.ui.theme.DigitalColor
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.util.fmtMoney

/** Rangos del gráfico en días; null es "todo el historial". */
private val CHART_RANGES = listOf(7 to R.string.range_7d, 30 to R.string.range_30d, 90 to R.string.range_90d, null to R.string.range_all)

/**
 * Detalle del ahorro: cuánto hay en efectivo, cuánto en digital y el total (con barra de proporción), y un gráfico
 * de cómo ha evolucionado el total en 7, 30 o 90 días o todo el historial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsSheet(pool: PoolState, movements: List<Movement>, onDismiss: () -> Unit) {
    val total = pool.cash + pool.digital
    val cashShare = if (total > 0) (pool.cash / total).toFloat() else 0f
    var rangeIndex by rememberSaveable { mutableIntStateOf(1) }
    val points = remember(movements, total, rangeIndex) { balanceSeries(movements, total, CHART_RANGES[rangeIndex].first) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.details_title), fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(stringResource(R.string.details_subtitle), fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

            DetailRow(stringResource(R.string.money_cash_name), CashColor, fmtMoney(pool.cash))
            DetailRow(stringResource(R.string.money_digital_name), DigitalColor, fmtMoney(pool.digital))
            DetailRow(stringResource(R.string.details_total), null, fmtMoney(total))

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Box(modifier = Modifier.weight(cashShare.coerceAtLeast(0.0001f)).fillMaxHeight().background(CashColor))
                Box(modifier = Modifier.weight((1f - cashShare).coerceAtLeast(0.0001f)).fillMaxHeight().background(DigitalColor))
            }

            Spacer(Modifier.height(22.dp))
            Text(stringResource(R.string.chart_title), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CHART_RANGES.forEachIndexed { i, (_, label) ->
                    ToggleChip(stringResource(label), selected = i == rangeIndex) { rangeIndex = i }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (points.size >= 2) {
                val change = points.last().balance - points.first().balance
                Text(
                    stringResource(if (change >= 0) R.string.chart_change_up else R.string.chart_change_down, fmtMoney(kotlin.math.abs(change))),
                    fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(bottom = 8.dp)
                )
                SavingsChart(points)
            } else {
                Text(stringResource(R.string.chart_empty), fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(vertical = 16.dp))
            }

            Spacer(Modifier.height(18.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_close)) }
        }
    }
}

@Composable
private fun DetailRow(label: String, dotColor: Color?, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(width = 0.dp, color = BorderColor),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            dotColor?.let {
                Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(it))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, fontSize = 13.5.sp)
        }
        Text(amount, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
