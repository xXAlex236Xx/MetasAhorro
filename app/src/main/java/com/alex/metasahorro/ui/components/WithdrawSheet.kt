package com.alex.metasahorro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.data.MoneyType
import com.alex.metasahorro.data.PoolState
import com.alex.metasahorro.ui.fmtMoney
import com.alex.metasahorro.ui.theme.CashColor
import com.alex.metasahorro.ui.theme.TextMutedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawSheet(pool: PoolState, onDismiss: () -> Unit, onConfirm: (amount: Double, type: MoneyType) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MoneyType.EFECTIVO) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text("Restar dinero", fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(
                "Disponible — Efectivo: ${fmtMoney(pool.cash)} · Digital: ${fmtMoney(pool.digital)}",
                fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            FieldLabel("Monto")
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it.filter { c -> c.isDigit() }; error = null },
                placeholder = { Text("0") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            FieldLabel("Tipo")
            MoneyTypeToggle(selected = type, onSelect = { type = it; error = null })

            error?.let {
                Text(it, color = CashColor, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        val disponible = if (type == MoneyType.EFECTIVO) pool.cash else pool.digital
                        when {
                            amount == null || amount <= 0 -> {}
                            amount > disponible -> error = "No tienes esa cantidad guardada."
                            else -> onConfirm(amount, type)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CashColor),
                    modifier = Modifier.weight(1f)
                ) { Text("Restar") }
            }
        }
    }
}
