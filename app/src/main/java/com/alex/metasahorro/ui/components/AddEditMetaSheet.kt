package com.alex.metasahorro.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alex.metasahorro.R
import com.alex.metasahorro.data.Meta
import com.alex.metasahorro.repository.MAX_CATEGORY_LENGTH
import com.alex.metasahorro.repository.normalizeCategory
import com.alex.metasahorro.ui.theme.BorderColor
import com.alex.metasahorro.ui.theme.SurfaceAltColor
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.util.fmtDateShort
import java.util.*

/**
 * Formulario para crear o editar una meta: nombre, precio, categoría opcional (se puede escribir una o elegir
 * una sugerida), fecha límite opcional y foto opcional. `categories` son las que ya se usan en la app.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditMetaSheet(
    editing: Meta?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, imageUri: String?, goal: Double, deadlineMillis: Long?, category: String?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var priceText by remember { mutableStateOf(editing?.goal?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "") }
    var category by remember { mutableStateOf(editing?.category ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(editing?.imageUri?.let { Uri.parse(it) }) }
    var deadlineMillis by remember { mutableStateOf(editing?.deadlineMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val defaults = listOf(
        stringResource(R.string.cat_tech), stringResource(R.string.cat_travel), stringResource(R.string.cat_home),
        stringResource(R.string.cat_clothes), stringResource(R.string.cat_health)
    )
    val suggestions = (categories + defaults).distinctBy { it.lowercase() }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            imageUri = uri
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 24.dp)) {
            Text(
                stringResource(if (editing != null) R.string.meta_edit_title else R.string.meta_new_title),
                fontWeight = FontWeight.SemiBold, fontSize = 19.sp
            )
            Text(
                stringResource(if (editing != null) R.string.meta_edit_subtitle else R.string.meta_new_subtitle),
                fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            FieldLabel(stringResource(R.string.field_name))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.field_name_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            FieldLabel(stringResource(R.string.field_price))
            OutlinedTextField(
                value = priceText, onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            FieldLabel(stringResource(R.string.field_category))
            OutlinedTextField(
                value = category, onValueChange = { category = it.take(MAX_CATEGORY_LENGTH) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.field_category_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                suggestions.forEach { suggestion ->
                    val selected = suggestion.equals(category.trim(), ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { category = if (selected) "" else suggestion },
                        label = { Text(suggestion, fontSize = 12.sp, maxLines = 1) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            FieldLabel(stringResource(R.string.field_deadline))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceAltColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { showDatePicker = true }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    deadlineMillis?.let { fmtDateShort(it) } ?: stringResource(R.string.deadline_none),
                    color = if (deadlineMillis != null) MaterialTheme.colorScheme.onSurface else TextMutedColor
                )
                if (deadlineMillis != null) {
                    Text(
                        stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable(role = Role.Button) { deadlineMillis = null }.padding(horizontal = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            FieldLabel(stringResource(R.string.field_image))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceAltColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("🌱", fontSize = 28.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.image_upload), fontSize = 12.5.sp)
                    }
                    if (imageUri != null) {
                        TextButton(onClick = { imageUri = null }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.image_remove), fontSize = 12.5.sp)
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.image_hint),
                fontSize = 11.sp, color = TextMutedColor, modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    onClick = {
                        val price = priceText.toDoubleOrNull()
                        if (name.isNotBlank() && price != null && price > 0) {
                            onSave(name.trim(), imageUri?.toString(), price, deadlineMillis, normalizeCategory(category))
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(if (editing != null) R.string.meta_save_changes else R.string.meta_create)) }
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
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) {
            DatePicker(state = state)
        }
    }
}

/** Etiqueta pequeña en mayúsculas sobre un campo de formulario. */
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
