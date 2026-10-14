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

/** Restar dinero del pozo. Valida que haya saldo suficiente en el tipo elegido antes de confirmar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawSheet(pool: PoolState, onDismiss: () -> Unit, onConfirm: (amount: Double, type: MoneyType) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MoneyType.EFECTIVO) }
    var showError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.withdraw_title), fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(
                stringResource(R.string.withdraw_available, fmtMoney(pool.cash), fmtMoney(pool.digital)),
                fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            FieldLabel(stringResource(R.string.field_amount))
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it.filter { c -> c.isDigit() }; showError = false },
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            FieldLabel(stringResource(R.string.field_type))
            MoneyTypeToggle(selected = type, onSelect = { type = it; showError = false })

            if (showError) {
                Text(stringResource(R.string.withdraw_error), color = CashColor, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        val disponible = if (type == MoneyType.EFECTIVO) pool.cash else pool.digital
                        when {
                            amount == null || amount <= 0 -> {}
                            amount > disponible -> showError = true
                            else -> onConfirm(amount, type)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CashColor),
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.withdraw_confirm)) }
            }
        }
    }
}
