package com.alex.metasahorro.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alex.metasahorro.data.Meta
import com.alex.metasahorro.ui.fmtDateShort
import com.alex.metasahorro.ui.theme.BorderColor
import com.alex.metasahorro.ui.theme.SurfaceAltColor
import com.alex.metasahorro.ui.theme.TextMutedColor
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMetaSheet(
    editing: Meta?,
    onDismiss: () -> Unit,
    onSave: (name: String, imageUri: String?, goal: Double, deadlineMillis: Long?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var priceText by remember { mutableStateOf(editing?.goal?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(editing?.imageUri?.let { Uri.parse(it) }) }
    var deadlineMillis by remember { mutableStateOf(editing?.deadlineMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            imageUri = uri
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text(if (editing != null) "Editar meta" else "Nueva meta", fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(
                if (editing != null) "Modifica los datos de tu meta" else "Agrega algo que quieras comprar",
                fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            FieldLabel("Nombre")
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                placeholder = { Text("Ej: Audífonos, Silla gamer...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            FieldLabel("Precio total")
            OutlinedTextField(
                value = priceText, onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                placeholder = { Text("0") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            FieldLabel("Fecha límite (opcional)")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceAltColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .clickable { showDatePicker = true }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    deadlineMillis?.let { fmtDateShort(it) } ?: "Sin fecha",
                    color = if (deadlineMillis != null) MaterialTheme.colorScheme.onSurface else TextMutedColor
                )
                if (deadlineMillis != null) {
                    Text("Quitar", color = MaterialTheme.colorScheme.error, modifier = Modifier.clickable { deadlineMillis = null })
                }
            }
            Spacer(Modifier.height(12.dp))

            FieldLabel("Imagen (opcional)")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceAltColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("🌱", fontSize = 20.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .clickable { imagePicker.launch("image/*") }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Subir foto", fontSize = 12.5.sp, color = TextMutedColor)
                }
            }
            Text(
                "Si no subes imagen, se muestra el ícono de crecimiento de la meta.",
                fontSize = 11.sp, color = TextMutedColor, modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    onClick = {
                        val price = priceText.toDoubleOrNull()
                        if (name.isNotBlank() && price != null && price > 0) {
                            onSave(name.trim(), imageUri?.toString(), price, deadlineMillis)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (editing != null) "Guardar cambios" else "Crear meta") }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = deadlineMillis ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineMillis = state.selectedDateMillis
                    showDatePicker = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text.uppercase(Locale.getDefault()),
        fontSize = 11.sp,
        letterSpacing = 0.08.sp,
        color = TextMutedColor,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
