package com.alex.metasahorro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.data.AccentTheme
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.ui.theme.accentFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(current: AccentTheme, onDismiss: () -> Unit, onSelect: (AccentTheme) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text("Apariencia", fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text("Elige el color de acento de la app", fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AccentTheme.values().forEach { theme ->
                    Column(
                        modifier = Modifier.weight(1f).clickable { onSelect(theme) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(accentFor(theme).accent)
                                .border(2.dp, if (theme == current) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(theme.label, fontSize = 11.5.sp, color = if (theme == current) MaterialTheme.colorScheme.onSurface else TextMutedColor)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Listo") }
        }
    }
}
