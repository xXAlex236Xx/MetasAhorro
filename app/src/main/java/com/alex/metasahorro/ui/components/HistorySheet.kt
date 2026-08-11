package com.alex.metasahorro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.data.Movement
import com.alex.metasahorro.data.MovementKind
import com.alex.metasahorro.data.MoneyType
import com.alex.metasahorro.ui.fmtDateTime
import com.alex.metasahorro.ui.fmtMoney
import com.alex.metasahorro.ui.theme.CashColor
import com.alex.metasahorro.ui.theme.GoldColor
import com.alex.metasahorro.ui.theme.SurfaceAltColor
import com.alex.metasahorro.ui.theme.TextMutedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(movements: List<Movement>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text("Historial de movimientos", fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text("Todo lo que has registrado", fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

            if (movements.isEmpty()) {
                EmptyState("🕒", "Sin movimientos todavía", "Cada vez que registres o restes plata, va a aparecer aquí.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(movements) { m -> MovementRow(m) }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
        }
    }
}

@Composable
private fun MovementRow(m: Movement) {
    val (icon, label, color, prefix) = when (m.kind) {
        MovementKind.DEPOSIT -> listOf("➕", "Registro · ${if (m.moneyType == MoneyType.EFECTIVO) "Efectivo" else "Digital"}", null, "+")
        MovementKind.WITHDRAW -> listOf("➖", "Retiro · ${if (m.moneyType == MoneyType.EFECTIVO) "Efectivo" else "Digital"}", CashColor, "−")
        MovementKind.PURCHASE -> listOf("🏆", "Comprado: ${m.label}", GoldColor, "−")
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceAltColor),
            contentAlignment = Alignment.Center
        ) { Text(icon as String, fontSize = 15.sp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label as String, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(fmtDateTime(m.tsMillis), fontSize = 11.sp, color = TextMutedColor)
        }
        Text(
            "$prefix${fmtMoney(m.amount)}",
            fontWeight = FontWeight.Bold, fontSize = 13.5.sp,
            color = (color as? androidx.compose.ui.graphics.Color) ?: MaterialTheme.colorScheme.primary
        )
    }
}
