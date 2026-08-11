package com.alex.metasahorro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.ui.fmtMoney
import com.alex.metasahorro.ui.theme.CashColor
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.viewmodel.MetaConSaldo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemMenuSheet(
    item: MetaConSaldo,
    onDismiss: () -> Unit,
    onEditar: () -> Unit,
    onBorrar: () -> Unit,
    onComprado: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text(item.meta.name, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(
                if (item.complete) "Ya juntaste el monto completo para esta meta."
                else "Llevas ${fmtMoney(item.saved)} de ${fmtMoney(item.meta.goal)} para esta meta.",
                fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.complete) {
                    Button(onClick = onComprado, modifier = Modifier.fillMaxWidth()) { Text("✅ Comprado") }
                } else {
                    OutlinedButton(onClick = onEditar, modifier = Modifier.fillMaxWidth()) { Text("✏️ Modificar") }
                    OutlinedButton(
                        onClick = onBorrar,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CashColor),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🗑️ Borrar") }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            }
        }
    }
}
