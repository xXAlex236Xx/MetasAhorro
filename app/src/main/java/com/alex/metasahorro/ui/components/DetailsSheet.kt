package com.alex.metasahorro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.data.PoolState
import com.alex.metasahorro.ui.fmtMoney
import com.alex.metasahorro.ui.theme.BorderColor
import com.alex.metasahorro.ui.theme.CashColor
import com.alex.metasahorro.ui.theme.DigitalColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsSheet(pool: PoolState, onDismiss: () -> Unit) {
    val total = pool.cash + pool.digital
    val cashShare = if (total > 0) (pool.cash / total).toFloat() else 0f

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text("Detalles del ahorro", fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text("Cuánto tienes guardado por tipo", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

            DetailRow("Efectivo", CashColor, fmtMoney(pool.cash))
            DetailRow("Digital", DigitalColor, fmtMoney(pool.digital))
            DetailRow("Total", null, fmtMoney(total))

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f))
            ) {
                Box(modifier = Modifier.weight(cashShare.coerceAtLeast(0.0001f)).fillMaxHeight().background(CashColor))
                Box(modifier = Modifier.weight((1f - cashShare).coerceAtLeast(0.0001f)).fillMaxHeight().background(DigitalColor))
            }

            Spacer(Modifier.height(18.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
        }
    }
}

@Composable
private fun DetailRow(label: String, dotColor: androidx.compose.ui.graphics.Color?, amount: String) {
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
