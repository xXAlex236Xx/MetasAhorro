package com.alex.metasahorro.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alex.metasahorro.data.Meta
import com.alex.metasahorro.ui.fmtDateShort
import com.alex.metasahorro.ui.fmtMoney
import com.alex.metasahorro.ui.theme.BorderColor
import com.alex.metasahorro.ui.theme.CashColor
import com.alex.metasahorro.ui.theme.SurfaceAltColor
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.viewmodel.MetaConSaldo

private val STAGES = listOf("🌰", "🌱", "🌿", "🪴", "🌳")
private fun stageIcon(pct: Float): String {
    val idx = (pct / 100f * STAGES.size).toInt().coerceIn(0, STAGES.size - 1)
    return STAGES[idx]
}

@Composable
fun ItemCard(
    item: MetaConSaldo,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val meta = item.meta
    val pct = if (meta.goal > 0) (item.saved / meta.goal * 100).toFloat().coerceIn(0f, 100f) else 0f
    val animatedPct by animateFloatAsState(targetValue = pct, animationSpec = tween(600), label = "itemPct")
    val overdue = meta.deadlineMillis != null && meta.deadlineMillis < System.currentTimeMillis() && !item.complete

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (item.complete) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else BorderColor,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícono / imagen
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceAltColor)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (meta.imageUri != null) {
                AsyncImage(
                    model = meta.imageUri,
                    contentDescription = meta.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                )
            } else {
                Text(if (item.complete) "🌳" else stageIcon(pct), fontSize = 20.sp)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    meta.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${animatedPct.toInt()}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animatedPct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.06f)
            )
            Spacer(Modifier.height(4.dp))
            val faltante = meta.goal - item.saved
            Text(
                text = fmtMoney(item.saved) + " de " + fmtMoney(meta.goal) +
                    if (!item.complete) " · falta ${fmtMoney(faltante)}" else "",
                fontSize = 11.sp,
                color = TextMutedColor
            )
            if (item.complete) {
                Text("🏆 Misión cumplida", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            } else if (item.saved == 0.0) {
                Text("Esperando plata para esta meta", fontSize = 10.5.sp, color = TextMutedColor)
            }
            meta.deadlineMillis?.let {
                Text(
                    text = (if (overdue) "⚠️ Venció " else "📅 Vence ") + fmtDateShort(it),
                    fontSize = 10.5.sp,
                    color = if (overdue) CashColor else TextMutedColor
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Flechas de reordenar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ReorderButton(enabled = !isFirst, icon = Icons.Filled.KeyboardArrowUp, onClick = onMoveUp)
            Spacer(Modifier.height(3.dp))
            ReorderButton(enabled = !isLast, icon = Icons.Filled.KeyboardArrowDown, onClick = onMoveDown)
        }
    }
}

@Composable
private fun ReorderButton(enabled: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 22.dp, height = 18.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(SurfaceAltColor)
            .border(1.dp, BorderColor, RoundedCornerShape(5.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) TextMutedColor else TextMutedColor.copy(alpha = 0.3f),
            modifier = Modifier.size(12.dp)
        )
    }
}
