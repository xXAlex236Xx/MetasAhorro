package com.alex.metasahorro.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alex.metasahorro.R
import com.alex.metasahorro.ui.theme.BorderColor
import com.alex.metasahorro.ui.theme.CashColor
import com.alex.metasahorro.ui.theme.SurfaceAltColor
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.util.fmtDateShort
import com.alex.metasahorro.util.fmtMoney
import com.alex.metasahorro.viewmodel.MetaConSaldo

private val STAGES = listOf("🌰", "🌱", "🌿", "🪴", "🌳")

/** Emoji de la etapa de crecimiento de una meta según su avance (0 a 100). */
internal fun stageIcon(pct: Float): String {
    val idx = (pct / 100f * STAGES.size).toInt().coerceIn(0, STAGES.size - 1)
    return STAGES[idx]
}

/**
 * Tarjeta de una meta: foto o ícono de crecimiento, categoría, progreso, lo que falta, fecha límite y flechas para
 * reordenar (que se ocultan con `showReorder = false`, por ejemplo mientras la lista está filtrada).
 */
@Composable
fun ItemCard(
    item: MetaConSaldo,
    isFirst: Boolean,
    isLast: Boolean,
    showReorder: Boolean,
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
            .clickable(role = Role.Button) { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Foto o ícono
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceAltColor)
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (meta.imageUri != null) {
                AsyncImage(
                    model = meta.imageUri,
                    contentDescription = meta.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                )
            } else {
                Text(if (item.complete) "🌳" else stageIcon(pct), fontSize = 24.sp)
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
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${animatedPct.toInt()}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            meta.category?.let { CategoryPill(it) }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animatedPct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
            Spacer(Modifier.height(4.dp))
            val faltante = meta.goal - item.saved
            Text(
                text = stringResource(R.string.item_saved_of, fmtMoney(item.saved), fmtMoney(meta.goal)) +
                    if (!item.complete) stringResource(R.string.item_missing, fmtMoney(faltante)) else "",
                fontSize = 11.sp,
                color = TextMutedColor
            )
            if (item.complete) {
                Text(stringResource(R.string.item_done), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            } else if (item.saved == 0.0) {
                Text(stringResource(R.string.item_waiting), fontSize = 10.5.sp, color = TextMutedColor)
            }
            meta.deadlineMillis?.let {
                Text(
                    text = stringResource(if (overdue) R.string.item_overdue else R.string.item_due, fmtDateShort(it)),
                    fontSize = 10.5.sp,
                    color = if (overdue) CashColor else TextMutedColor
                )
            }
        }

        if (showReorder) {
            Spacer(Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ReorderButton(enabled = !isFirst, up = true, description = stringResource(R.string.cd_move_up, meta.name), onClick = onMoveUp)
                ReorderButton(enabled = !isLast, up = false, description = stringResource(R.string.cd_move_down, meta.name), onClick = onMoveDown)
            }
        }
    }
}

/** Etiqueta pequeña con la categoría de la meta. */
@Composable
fun CategoryPill(text: String) {
    Text(
        "#$text",
        fontSize = 10.5.sp,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 2.dp)
    )
}

/** Flecha de reordenar: el área táctil es de 40×34 dp aunque el dibujo sea más pequeño. */
@Composable
private fun ReorderButton(enabled: Boolean, up: Boolean, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 34.dp)
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClickLabel = description) { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 26.dp, height = 22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceAltColor)
                .border(1.dp, BorderColor, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (up) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = description,
                tint = if (enabled) TextMutedColor else TextMutedColor.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
