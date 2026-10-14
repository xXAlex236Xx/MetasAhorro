package com.alex.metasahorro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.R
import com.alex.metasahorro.data.MoneyType
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.util.fmtNumber

/** Registrar dinero ahorrado: monto (con atajos personalizables, por defecto +5.000, +10.000 y +20.000) y tipo, efectivo o digital. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositSheet(quickAmounts: List<Int>, onDismiss: () -> Unit, onConfirm: (amount: Double, type: MoneyType) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MoneyType.EFECTIVO) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.deposit_title), fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(stringResource(R.string.deposit_subtitle), fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                quickAmounts.forEach { quick ->
                    OutlinedButton(
                        onClick = { amountText = quick.toString() },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+" + fmtNumber(quick), maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            FieldLabel(stringResource(R.string.field_amount))
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            FieldLabel(stringResource(R.string.field_type))
            MoneyTypeToggle(selected = type, onSelect = { type = it })

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount != null && amount > 0) onConfirm(amount, type)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.deposit_confirm)) }
            }
        }
    }
}

/** Selector de dos opciones, efectivo o digital. */
@Composable
fun MoneyTypeToggle(selected: MoneyType, onSelect: (MoneyType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ToggleChip(stringResource(R.string.money_cash), selected == MoneyType.EFECTIVO, Modifier.weight(1f)) { onSelect(MoneyType.EFECTIVO) }
        ToggleChip(stringResource(R.string.money_digital), selected == MoneyType.DIGITAL, Modifier.weight(1f)) { onSelect(MoneyType.DIGITAL) }
    }
}

/** Botón de opción seleccionable (chip). */
@Composable
fun ToggleChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        modifier = modifier
    )
}
