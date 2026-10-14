package com.alex.metasahorro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.R
import com.alex.metasahorro.data.MoneyType
import com.alex.metasahorro.data.Movement
import com.alex.metasahorro.data.MovementKind
import com.alex.metasahorro.ui.theme.CashColor
import com.alex.metasahorro.ui.theme.GoldColor
import com.alex.metasahorro.ui.theme.SurfaceAltColor
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.util.fmtDateTime
import com.alex.metasahorro.util.fmtMoney

/**
 * Lista cronológica (más reciente primero) de depósitos, retiros, transformaciones y compras.
 * Los depósitos, retiros y transformaciones se pueden borrar (con confirmación): se revierte su efecto en el ahorro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(movements: List<Movement>, onDelete: (Movement) -> Unit, onDismiss: () -> Unit) {
    var pendingDelete by remember { mutableStateOf<Movement?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.history_title), fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(stringResource(R.string.history_subtitle), fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

            if (movements.isEmpty()) {
                EmptyState("🕒", stringResource(R.string.history_empty_title), stringResource(R.string.history_empty_text))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(movements, key = { it.id }) { m -> MovementRow(m, onDelete = { pendingDelete = m }) }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_close)) }
        }
    }

    pendingDelete?.let { movement ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_text)) },
            confirmButton = {
                TextButton(onClick = { onDelete(movement); pendingDelete = null }) {
                    Text(stringResource(R.string.action_delete), color = CashColor)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun MovementRow(m: Movement, onDelete: () -> Unit) {
    val moneyName = stringResource(if (m.moneyType == MoneyType.EFECTIVO) R.string.money_cash_name else R.string.money_digital_name)
    val icon: String
    val label: String
    val color: Color
    val prefix: String
    when (m.kind) {
        MovementKind.DEPOSIT -> {
            icon = "➕"; label = stringResource(R.string.history_deposit, moneyName)
            color = MaterialTheme.colorScheme.primary; prefix = "+"
        }
        MovementKind.WITHDRAW -> {
            icon = "➖"; label = stringResource(R.string.history_withdraw, moneyName)
            color = CashColor; prefix = "−"
        }
        MovementKind.PURCHASE -> {
            icon = "🏆"; label = stringResource(R.string.history_purchase, m.label ?: "")
            color = GoldColor; prefix = "−"
        }
        MovementKind.TRANSFORM -> {
            icon = "🔁"
            label = when (m.moneyType) {
                MoneyType.EFECTIVO -> stringResource(R.string.history_cash_to_digital)
                MoneyType.DIGITAL -> stringResource(R.string.history_digital_to_cash)
                null -> m.label ?: stringResource(R.string.history_transform)
            }
            color = MaterialTheme.colorScheme.primary; prefix = ""
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceAltColor),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 15.sp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(fmtDateTime(m.tsMillis), fontSize = 11.sp, color = TextMutedColor)
        }
        Text("$prefix${fmtMoney(m.amount)}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = color)
        if (m.kind != MovementKind.PURCHASE) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_movement), tint = TextMutedColor)
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}
