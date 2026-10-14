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
import com.alex.metasahorro.data.PoolState
import com.alex.metasahorro.ui.theme.CashColor
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.util.fmtMoney

/** Convertir dinero de efectivo a digital (o al revés) sin cambiar el total ahorrado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransformSheet(pool: PoolState, onDismiss: () -> Unit, onConfirm: (amount: Double, from: MoneyType) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var from by remember { mutableStateOf(MoneyType.EFECTIVO) }
    var showError by remember { mutableStateOf(false) }
    val fromCash = from == MoneyType.EFECTIVO

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.transform_title), fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(
                stringResource(R.string.transform_subtitle),
                fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            FieldLabel(stringResource(R.string.transform_from))
            MoneyTypeToggle(selected = from, onSelect = { from = it; showError = false })
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(if (fromCash) R.string.transform_to_digital else R.string.transform_to_cash),
                fontSize = 11.5.sp, color = TextMutedColor
            )

            Spacer(Modifier.height(14.dp))
            FieldLabel(stringResource(R.string.field_amount))
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it.filter { c -> c.isDigit() }; showError = false },
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(
                    if (fromCash) R.string.transform_available_cash else R.string.transform_available_digital,
                    fmtMoney(if (fromCash) pool.cash else pool.digital)
                ),
                fontSize = 11.sp, color = TextMutedColor, modifier = Modifier.padding(top = 6.dp)
            )

            if (showError) {
                Text(stringResource(R.string.transform_error), color = CashColor, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        val disponible = if (fromCash) pool.cash else pool.digital
                        when {
                            amount == null || amount <= 0 -> {}
                            amount > disponible -> showError = true
                            else -> onConfirm(amount, from)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.transform_confirm)) }
            }
        }
    }
}
