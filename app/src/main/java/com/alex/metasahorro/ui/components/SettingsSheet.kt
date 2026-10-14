package com.alex.metasahorro.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.alex.metasahorro.R
import com.alex.metasahorro.data.AccentTheme
import com.alex.metasahorro.data.DEFAULT_QUICK_AMOUNTS
import com.alex.metasahorro.data.MAX_QUICK_AMOUNTS
import com.alex.metasahorro.data.ReminderSettings
import com.alex.metasahorro.data.ThemeMode
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.ui.theme.accentFor
import com.alex.metasahorro.ui.theme.LocalAppColors

/**
 * Ajustes en tres bloques: apariencia (modo claro/oscuro/sistema, colores dinámicos en Android 12+ y color de
 * acento), montos rápidos del botón Registrar, y recordatorios (aviso diario con hora y aviso de metas por vencer).
 * Activar un recordatorio pide el permiso de notificaciones en Android 13+.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    theme: AccentTheme,
    mode: ThemeMode,
    dynamicColor: Boolean,
    quickAmounts: List<Int>,
    reminders: ReminderSettings,
    onDismiss: () -> Unit,
    onTheme: (AccentTheme) -> Unit,
    onMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onQuickAmounts: (List<Int>) -> Unit,
    onDailyReminder: (Boolean) -> Unit,
    onReminderHour: (Int) -> Unit,
    onDeadlineReminder: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val dark = LocalAppColors.current.isDark

    // Se recuerda qué se quería activar mientras se espera la respuesta del permiso de notificaciones.
    var pendingEnable by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingEnable?.invoke()
        else Toast.makeText(context, R.string.reminders_permission_denied, Toast.LENGTH_LONG).show()
        pendingEnable = null
    }
    fun enableWithPermission(action: () -> Unit) {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingEnable = action
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 24.dp)) {
            // ---- Apariencia ----
            Text(stringResource(R.string.settings_appearance), fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(stringResource(R.string.settings_appearance_subtitle), fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEach { m ->
                    ToggleChip(stringResource(m.label), selected = m == mode, modifier = Modifier.weight(1f)) { onMode(m) }
                }
            }

            if (dynamicAvailable) {
                Spacer(Modifier.height(14.dp))
                SwitchRow(
                    title = stringResource(R.string.settings_dynamic),
                    subtitle = stringResource(R.string.settings_dynamic_hint),
                    checked = dynamicColor,
                    onChange = onDynamicColor
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().alpha(if (dynamicColor && dynamicAvailable) 0.35f else 1f)
            ) {
                AccentTheme.entries.forEach { t ->
                    Column(
                        modifier = Modifier.weight(1f).clickable(role = Role.RadioButton, enabled = !(dynamicColor && dynamicAvailable)) { onTheme(t) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(accentFor(t, dark).accent)
                                .border(2.dp, if (t == theme) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(t.label), fontSize = 11.5.sp, color = if (t == theme) MaterialTheme.colorScheme.onSurface else TextMutedColor)
                    }
                }
            }

            // ---- Montos rápidos ----
            SectionDivider()
            Text(stringResource(R.string.settings_quick), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(stringResource(R.string.settings_quick_hint), fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            QuickAmountsEditor(quickAmounts, onQuickAmounts)

            // ---- Recordatorios ----
            SectionDivider()
            Text(stringResource(R.string.settings_reminders), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                title = stringResource(R.string.settings_daily),
                subtitle = stringResource(R.string.settings_daily_hint),
                checked = reminders.dailyEnabled,
                onChange = { on -> if (on) enableWithPermission { onDailyReminder(true) } else onDailyReminder(false) }
            )
            if (reminders.dailyEnabled) {
                HourStepper(reminders.hour, onReminderHour)
            }
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                title = stringResource(R.string.settings_deadline),
                subtitle = stringResource(R.string.settings_deadline_hint),
                checked = reminders.deadlineEnabled,
                onChange = { on -> if (on) enableWithPermission { onDeadlineReminder(true) } else onDeadlineReminder(false) }
            )

            Spacer(Modifier.height(18.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_ready)) }
        }
    }
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(18.dp))
    HorizontalDivider()
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.5.sp, color = TextMutedColor)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Campos para editar los montos rápidos. Cada cambio válido se guarda al instante; los vacíos se omiten. */
@Composable
private fun QuickAmountsEditor(amounts: List<Int>, onChange: (List<Int>) -> Unit) {
    // Se arma una sola vez: si se rearmara con cada guardado, se perderían teclas mientras se escribe.
    val texts = remember {
        mutableStateListOf<String>().apply {
            addAll(amounts.map { it.toString() })
            while (size < MAX_QUICK_AMOUNTS) add("")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        texts.forEachIndexed { i, text ->
            OutlinedTextField(
                value = text,
                onValueChange = { raw ->
                    texts[i] = raw.filter { c -> c.isDigit() }.take(9)
                    onChange(texts.mapNotNull { it.toIntOrNull() }.filter { it > 0 })
                },
                singleLine = true,
                label = { Text(stringResource(R.string.settings_quick_label, i + 1), fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
    }
    TextButton(onClick = {
        texts.clear()
        texts.addAll(DEFAULT_QUICK_AMOUNTS.map { it.toString() })
        onChange(DEFAULT_QUICK_AMOUNTS)
    }) { Text(stringResource(R.string.settings_quick_reset)) }
}

/** Selector de hora con botones − y +. */
@Composable
private fun HourStepper(hour: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.settings_hour), fontSize = 13.sp, color = TextMutedColor, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((hour + 23) % 24) }) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.cd_hour_earlier))
        }
        Text("%02d:00".format(hour), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = { onChange((hour + 1) % 24) }) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_hour_later))
        }
    }
}
