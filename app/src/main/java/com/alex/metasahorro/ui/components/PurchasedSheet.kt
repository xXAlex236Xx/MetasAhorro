package com.alex.metasahorro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alex.metasahorro.data.Purchased
import com.alex.metasahorro.ui.fmtDateTime
import com.alex.metasahorro.ui.fmtMoney
import com.alex.metasahorro.ui.theme.GoldColor
import com.alex.metasahorro.ui.theme.SurfaceAltColor
import com.alex.metasahorro.ui.theme.TextMutedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasedSheet(purchased: List<Purchased>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp)) {
            Text("Ya compré esto 🏆", fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text("Tu archivo de metas cumplidas", fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

            if (purchased.isEmpty()) {
                EmptyState("🏆", "Aún nada en la vitrina", "Cuando completes una meta y la marques como comprada, va a quedar guardada acá.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(purchased) { p -> PurchasedRow(p) }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
        }
    }
}

@Composable
private fun PurchasedRow(p: Purchased) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceAltColor),
            contentAlignment = Alignment.Center
        ) {
            if (p.imageUri != null) {
                AsyncImage(model = p.imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text("🌳", fontSize = 17.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(p.name, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
            Text("Comprado el ${fmtDateTime(p.boughtAtMillis)}", fontSize = 11.sp, color = TextMutedColor)
        }
        Text(fmtMoney(p.goal), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GoldColor)
    }
}
