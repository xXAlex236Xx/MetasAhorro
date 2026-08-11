package com.alex.metasahorro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.data.Meta
import com.alex.metasahorro.data.MoneyType
import com.alex.metasahorro.ui.components.*
import com.alex.metasahorro.ui.theme.*
import com.alex.metasahorro.viewmodel.MetaConSaldo
import com.alex.metasahorro.viewmodel.MetasViewModel

private enum class SheetType { NONE, ADD, EDIT, DEPOSIT, WITHDRAW, ITEM_MENU, DETAILS, HISTORY, PURCHASED, SETTINGS, INFO }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MetasViewModel) {
    val state by viewModel.uiState.collectAsState()
    var sheet by remember { mutableStateOf(SheetType.NONE) }
    var selectedItem by remember { mutableStateOf<MetaConSaldo?>(null) }
    var editingMeta by remember { mutableStateOf<Meta?>(null) }
    var confettiTrigger by remember { mutableStateOf(0) }
    val haptics = LocalHapticFeedback.current

    val totalGoal = state.metas.sumOf { it.meta.goal }
    val totalSaved = state.pool.cash + state.pool.digital
    val totalPct = if (totalGoal > 0) (totalSaved / totalGoal * 100).toFloat().coerceIn(0f, 100f) else 0f
    val doneCount = state.metas.count { it.complete }
    val extra = (totalSaved - totalGoal).coerceAtLeast(0.0)
    val cashShare = if (totalSaved > 0) (state.pool.cash.coerceAtMost(totalSaved) / totalSaved).toFloat() else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 100.dp)
        ) {
            item {
                HeaderSection(
                    streak = state.streak,
                    onHistory = { sheet = SheetType.HISTORY },
                    onPurchased = { sheet = SheetType.PURCHASED },
                    onDetails = { sheet = SheetType.DETAILS },
                    onSettings = { sheet = SheetType.SETTINGS },
                    onInfo = { sheet = SheetType.INFO }
                )
            }
            item {
                SummaryCard(
                    totalSaved = totalSaved, totalGoal = totalGoal, totalPct = totalPct,
                    cashShare = cashShare, doneCount = doneCount, itemCount = state.metas.size,
                    extra = extra,
                    onRegistrar = { sheet = SheetType.DEPOSIT },
                    onRestar = { sheet = SheetType.WITHDRAW }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ÍTEMS", fontSize = 11.sp, letterSpacing = 0.12.sp, color = TextMutedColor, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text("contra tu total actual", fontSize = 10.5.sp, color = TextMutedColor.copy(alpha = 0.8f))
                }
            }
            if (state.metas.isEmpty()) {
                item { EmptyState("🪴", "Aún no hay nada plantado", "Toca el botón + para agregar lo primero que quieras juntar.") }
            } else {
                items(state.metas, key = { it.meta.id }) { item ->
                    Box(modifier = Modifier.animateItemPlacement().padding(bottom = 10.dp)) {
                        ItemCard(
                            item = item,
                            isFirst = state.metas.first() == item,
                            isLast = state.metas.last() == item,
                            onClick = { selectedItem = item; sheet = SheetType.ITEM_MENU },
                            onMoveUp = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.moveMeta(item.meta, -1) },
                            onMoveDown = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.moveMeta(item.meta, 1) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editingMeta = null; sheet = SheetType.ADD },
            containerColor = SurfaceColor,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(22.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "Nueva meta") }

        ConfettiOverlay(trigger = confettiTrigger, modifier = Modifier.fillMaxSize())
    }

    when (sheet) {
        SheetType.ADD, SheetType.EDIT -> AddEditMetaSheet(
            editing = editingMeta,
            onDismiss = { sheet = SheetType.NONE },
            onSave = { name, uri, goal, deadline ->
                if (editingMeta != null) {
                    viewModel.updateMeta(editingMeta!!.copy(name = name, imageUri = uri, goal = goal, deadlineMillis = deadline))
                } else {
                    viewModel.addMeta(name, uri, goal, deadline)
                }
                sheet = SheetType.NONE
            }
        )
        SheetType.DEPOSIT -> DepositSheet(
            onDismiss = { sheet = SheetType.NONE },
            onConfirm = { amount, type -> viewModel.registrar(amount, type); sheet = SheetType.NONE }
        )
        SheetType.WITHDRAW -> WithdrawSheet(
            pool = state.pool,
            onDismiss = { sheet = SheetType.NONE },
            onConfirm = { amount, type -> viewModel.restar(amount, type) {}; sheet = SheetType.NONE }
        )
        SheetType.ITEM_MENU -> selectedItem?.let { item ->
            ItemMenuSheet(
                item = item,
                onDismiss = { sheet = SheetType.NONE },
                onEditar = { editingMeta = item.meta; sheet = SheetType.EDIT },
                onBorrar = { viewModel.deleteMeta(item.meta); sheet = SheetType.NONE },
                onComprado = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.marcarComprado(item.meta)
                    sheet = SheetType.NONE
                    confettiTrigger++
                }
            )
        }
        SheetType.DETAILS -> DetailsSheet(pool = state.pool, onDismiss = { sheet = SheetType.NONE })
        SheetType.HISTORY -> HistorySheet(movements = state.movements, onDismiss = { sheet = SheetType.NONE })
        SheetType.PURCHASED -> PurchasedSheet(purchased = state.purchased, onDismiss = { sheet = SheetType.NONE })
        SheetType.SETTINGS -> SettingsSheet(
            current = state.theme,
            onDismiss = { sheet = SheetType.NONE },
            onSelect = { viewModel.setTheme(it) }
        )
        SheetType.INFO -> InfoSheet(onDismiss = { sheet = SheetType.NONE })
        SheetType.NONE -> {}
    }
}

@Composable
private fun HeaderSection(streak: Int, onHistory: () -> Unit, onPurchased: () -> Unit, onDetails: () -> Unit, onSettings: () -> Unit, onInfo: () -> Unit) {
    val animatedStreak by animateIntAsState(targetValue = streak, animationSpec = tween(600), label = "streak")
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("MIS METAS", fontSize = 11.sp, letterSpacing = 0.12.sp, color = MaterialTheme.colorScheme.primary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HeaderIconButton(Icons.Filled.History, onHistory)
                HeaderIconButton(Icons.Filled.EmojiEvents, onPurchased)
                HeaderIconButton(Icons.Filled.BarChart, onDetails)
                HeaderIconButton(Icons.Filled.Settings, onSettings)
                HeaderIconButton(Icons.Filled.Info, onInfo)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row {
            Text("Lo que estoy ", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text("ahorrando", fontSize = 26.sp, fontWeight = FontWeight.Medium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(SurfaceAltColor)
                .border(1.dp, BorderColor, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text("🔥", fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Text("$animatedStreak días seguidos ahorrando", fontSize = 11.5.sp, color = GoldColor, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HeaderIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(9.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = TextMutedColor, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun SummaryCard(
    totalSaved: Double, totalGoal: Double, totalPct: Float, cashShare: Float,
    doneCount: Int, itemCount: Int, extra: Double,
    onRegistrar: () -> Unit, onRestar: () -> Unit
) {
    val animatedSaved by animateFloatAsState(targetValue = totalSaved.toFloat(), animationSpec = tween(600), label = "saved")
    val animatedPct by animateFloatAsState(targetValue = totalPct, animationSpec = tween(600), label = "pct")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceAltColor)
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text(fmtMoney(animatedSaved.toDouble()), fontSize = 30.sp, fontWeight = FontWeight.SemiBold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Text("de ${fmtMoney(totalGoal)} meta", fontSize = 13.sp, color = TextMutedColor)
            }
            Text("${animatedPct.toInt()}%", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPct / 100f)
                    .fillMaxHeight()
            ) {
                Row(Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(cashShare.coerceAtLeast(0.0001f)).fillMaxHeight().background(CashColor))
                    Box(modifier = Modifier.weight((1f - cashShare).coerceAtLeast(0.0001f)).fillMaxHeight().background(DigitalColor))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$doneCount de $itemCount en flor 🌳", fontSize = 12.sp, color = TextMutedColor)
            Row {
                LegendDot(CashColor, "Efectivo")
                Spacer(Modifier.width(10.dp))
                LegendDot(DigitalColor, "Digital")
            }
        }
        if (extra > 0) {
            Spacer(Modifier.height(4.dp))
            Text("${fmtMoney(extra)} disponibles para una meta nueva", fontSize = 12.sp, color = GoldColor)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRegistrar, modifier = Modifier.weight(1f)) { Text("+ Registrar") }
            OutlinedButton(onClick = onRestar, modifier = Modifier.weight(1f)) { Text("− Restar") }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.5.sp, color = TextMutedColor)
    }
}