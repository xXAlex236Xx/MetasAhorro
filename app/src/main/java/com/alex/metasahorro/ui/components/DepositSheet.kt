package com.alex.metasahorro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.data.MoneyType
import com.alex.metasahorro.ui.theme.TextMutedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositSheet(onDismiss: () -> Unit, onConfirm: (amount: Double, type: MoneyType) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MoneyType.EFECTIVO) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text("Registrar nueva cantidad", fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text("Este monto se suma a tu total ahorrado", fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(5000, 10000, 20000).forEach { quick ->
                    OutlinedButton(onClick = { amountText = quick.toString() }, modifier = Modifier.weight(1f)) {
                        Text("+" + "%,d".format(quick).replace(",", "."))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            FieldLabel("Monto")
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                placeholder = { Text("0") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            FieldLabel("Tipo")
            MoneyTypeToggle(selected = type, onSelect = { type = it })

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount != null && amount > 0) onConfirm(amount, type)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Registrar") }
            }
        }
    }
}

@Composable
fun MoneyTypeToggle(selected: MoneyType, onSelect: (MoneyType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ToggleChip("💵 Efectivo", selected == MoneyType.EFECTIVO, Modifier.weight(1f)) { onSelect(MoneyType.EFECTIVO) }
        ToggleChip("💳 Digital", selected == MoneyType.DIGITAL, Modifier.weight(1f)) { onSelect(MoneyType.DIGITAL) }
    }
}

@Composable
fun ToggleChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        modifier = modifier
    )
}
