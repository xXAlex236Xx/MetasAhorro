package com.alex.metasahorro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alex.metasahorro.R
import com.alex.metasahorro.data.Movement
import com.alex.metasahorro.repository.estimateCompletion
import com.alex.metasahorro.repository.estimateDailyRate
import com.alex.metasahorro.repository.suggestedContribution
import com.alex.metasahorro.ui.theme.CashColor
import com.alex.metasahorro.ui.theme.GoldColor
import com.alex.metasahorro.ui.theme.SurfaceAltColor
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.util.fmtDateShort
import com.alex.metasahorro.util.fmtMoney
import com.alex.metasahorro.viewmodel.MetaConSaldo

/** Qué mostrar en el recuadro de pronóstico de una meta. */
private sealed interface Insight {
    data object Complete : Insight
    data object DeadlinePassed : Insight
    data class Contribution(val perWeek: Double?, val perDay: Double, val daysLeft: Int) : Insight
    data class Estimate(val dateMillis: Long, val perDay: Double, val afterDeadline: Boolean) : Insight
    data object NoRate : Insight
}

private fun insightsFor(item: MetaConSaldo, movements: List<Movement>, now: Long): List<Insight> {
    if (item.complete) return listOf(Insight.Complete)
    val remaining = item.meta.goal - item.saved
    val deadline = item.meta.deadlineMillis
    val out = mutableListOf<Insight>()

    if (deadline != null && deadline <= now) {
        out += Insight.DeadlinePassed
    } else {
        suggestedContribution(remaining, deadline, now)?.let {
            out += Insight.Contribution(perWeek = if (it.daysLeft >= 14) it.perWeek else null, perDay = it.perDay, daysLeft = it.daysLeft)
        }
    }
    val rate = estimateDailyRate(movements, now)
    val eta = estimateCompletion(remaining, rate, now)
    out += if (eta != null) Insight.Estimate(eta, rate, afterDeadline = deadline != null && eta > deadline) else Insight.NoRate
    return out
}

/**
 * Detalle y menú de una meta: foto grande (o ícono de crecimiento), categoría, progreso, fecha límite y pronóstico
 * (cuánto ahorrar para llegar a tiempo y qué día llegarías al ritmo actual). Permite marcarla como comprada si ya
 * está completa, modificarla o borrarla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemMenuSheet(
    item: MetaConSaldo,
    movements: List<Movement>,
    onDismiss: () -> Unit,
    onEditar: () -> Unit,
    onBorrar: () -> Unit,
    onComprado: () -> Unit
) {
    val meta = item.meta
    val pct = if (meta.goal > 0) (item.saved / meta.goal * 100).toFloat().coerceIn(0f, 100f) else 0f
    val insights = remember(item, movements) { insightsFor(item, movements, System.currentTimeMillis()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 24.dp)) {
            // Foto grande o ícono de crecimiento
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (meta.imageUri != null) Modifier.aspectRatio(16f / 9f) else Modifier.height(96.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceAltColor),
                contentAlignment = Alignment.Center
            ) {
                if (meta.imageUri != null) {
                    AsyncImage(
                        model = meta.imageUri, contentDescription = meta.name,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(if (item.complete) "🌳" else stageIcon(pct), fontSize = 44.sp)
                }
            }
            Spacer(Modifier.height(14.dp))

            Text(meta.name, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            meta.category?.let { CategoryPill(it) }
            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { pct / 100f },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.item_saved_of, fmtMoney(item.saved), fmtMoney(meta.goal)) +
                    if (!item.complete) stringResource(R.string.item_missing, fmtMoney(meta.goal - item.saved)) else "",
                fontSize = 12.sp, color = TextMutedColor
            )
            meta.deadlineMillis?.let {
                val overdue = it < System.currentTimeMillis() && !item.complete
                Text(
                    stringResource(if (overdue) R.string.item_overdue else R.string.item_due, fmtDateShort(it)),
                    fontSize = 12.sp, color = if (overdue) CashColor else TextMutedColor
                )
            }

            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SurfaceAltColor).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.insight_title), fontSize = 11.sp, color = TextMutedColor, letterSpacing = 0.08.sp)
                insights.forEach { InsightLine(it) }
            }

            Spacer(Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.complete) {
                    Button(onClick = onComprado, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_bought)) }
                }
                OutlinedButton(onClick = onEditar, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_edit)) }
                OutlinedButton(
                    onClick = onBorrar,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CashColor),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.action_delete_goal)) }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    }
}

@Composable
private fun InsightLine(insight: Insight) {
    val text: String
    var color = MaterialTheme.colorScheme.onSurface
    when (insight) {
        Insight.Complete -> text = stringResource(R.string.insight_complete)
        Insight.DeadlinePassed -> { text = stringResource(R.string.insight_deadline_passed); color = CashColor }
        Insight.NoRate -> { text = stringResource(R.string.insight_no_rate); color = TextMutedColor }
        is Insight.Contribution -> text = if (insight.perWeek != null) {
            stringResource(R.string.insight_contribution_week, fmtMoney(insight.perWeek), insight.daysLeft)
        } else {
            stringResource(R.string.insight_contribution_day, fmtMoney(insight.perDay), insight.daysLeft)
        }
        is Insight.Estimate -> {
            text = stringResource(
                if (insight.afterDeadline) R.string.insight_estimate_late else R.string.insight_estimate,
                fmtDateShort(insight.dateMillis), fmtMoney(insight.perDay)
            )
            if (insight.afterDeadline) color = GoldColor
        }
    }
    Text(text, fontSize = 12.5.sp, color = color)
}
