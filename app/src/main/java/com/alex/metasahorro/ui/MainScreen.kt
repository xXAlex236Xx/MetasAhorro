package com.alex.metasahorro.ui

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.R
import com.alex.metasahorro.data.Meta
import com.alex.metasahorro.repository.Achievement
import com.alex.metasahorro.ui.components.*
import com.alex.metasahorro.ui.theme.*
import com.alex.metasahorro.util.fmtMoney
import com.alex.metasahorro.viewmodel.MetasViewModel
import com.alex.metasahorro.viewmodel.UiEvent
import kotlinx.coroutines.launch

private enum class SheetType { NONE, ADD, EDIT, DEPOSIT, WITHDRAW, TRANSFORM, ITEM_MENU, DETAILS, HISTORY, PURCHASED, ACHIEVEMENTS, SETTINGS, INFO }

/**
 * Pantalla principal: cabecera con racha y accesos, tarjeta resumen del pozo (efectivo vs digital),
 * filtro por categoría, lista de metas con su progreso y botón + para crear una. Todas las demás vistas se abren
 * como hojas inferiores. También muestra los avisos con "Deshacer" y las celebraciones (confeti y logros nuevos).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MetasViewModel) {
    val state by viewModel.uiState.collectAsState()
    var sheet by remember { mutableStateOf(SheetType.NONE) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var editingMeta by remember { mutableStateOf<Meta?>(null) }
    var confettiTrigger by remember { mutableStateOf(0) }
    var newAchievements by remember { mutableStateOf<List<Achievement>>(emptyList()) }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val sheetOpen by rememberUpdatedState(sheet != SheetType.NONE)
    val undoLabel = stringResource(R.string.action_undo)

    // Avisos y celebraciones que emite el ViewModel (una sola vez cada uno)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> {
                    val text = context.getString(event.res, *event.args.toTypedArray())
                    if (event.res == R.string.msg_purchase_done) {
                        confettiTrigger++
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    if (sheetOpen && event.undo == null) {
                        // Con una hoja abierta el aviso inferior queda tapado: se usa un Toast.
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    } else {
                        launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val result = snackbar.showSnackbar(
                                message = text,
                                actionLabel = if (event.undo != null) undoLabel else null,
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed && event.undo != null) viewModel.undo(event.undo)
                        }
                    }
                }
                is UiEvent.GoalReached -> {
                    confettiTrigger++
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    launch {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar(context.getString(R.string.msg_goal_reached, event.name), duration = SnackbarDuration.Long)
                    }
                }
                is UiEvent.AchievementsUnlocked -> {
                    confettiTrigger++
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Room avisa de los cambios por partes: se juntan los lotes para no perder ninguno.
                    newAchievements = (newAchievements + event.list).distinct()
                }
            }
        }
    }

    // Si la categoría filtrada ya no existe (se borró o cambió la última meta que la usaba), se quita el filtro.
    val categories = state.categories
    val activeFilter = categoryFilter?.takeIf { f -> categories.any { it.equals(f, ignoreCase = true) } }
    val visibleMetas = state.metas.filter { activeFilter == null || it.meta.category.equals(activeFilter, ignoreCase = true) }
    val selectedItem = state.metas.firstOrNull { it.meta.id == selectedId }

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
                    onAchievements = { sheet = SheetType.ACHIEVEMENTS },
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
                    onRestar = { sheet = SheetType.WITHDRAW },
                    onTransformar = { sheet = SheetType.TRANSFORM }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.items_header), fontSize = 11.sp, letterSpacing = 0.12.sp, color = TextMutedColor, fontFamily = FontFamily.Monospace)
                    Text(stringResource(R.string.items_subheader), fontSize = 10.5.sp, color = TextMutedColor.copy(alpha = 0.8f))
                }
            }
            if (categories.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = activeFilter == null,
                                onClick = { categoryFilter = null },
                                label = { Text(stringResource(R.string.filter_all)) }
                            )
                        }
                        items(categories) { category ->
                            FilterChip(
                                selected = category.equals(activeFilter, ignoreCase = true),
                                onClick = { categoryFilter = category },
                                label = { Text(category, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }
            if (state.metas.isEmpty()) {
                item { EmptyState("🪴", stringResource(R.string.empty_title), stringResource(R.string.empty_text)) }
            } else {
                items(visibleMetas, key = { it.meta.id }) { item ->
                    Box(modifier = Modifier.animateItemPlacement().padding(bottom = 10.dp)) {
                        ItemCard(
                            item = item,
                            isFirst = visibleMetas.first() == item,
                            isLast = visibleMetas.last() == item,
                            showReorder = activeFilter == null,
                            onClick = { selectedId = item.meta.id; sheet = SheetType.ITEM_MENU },
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
        ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_new_goal)) }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
        )

        ConfettiOverlay(trigger = confettiTrigger, modifier = Modifier.fillMaxSize())
    }

    when (sheet) {
        SheetType.ADD, SheetType.EDIT -> AddEditMetaSheet(
            editing = editingMeta,
            categories = categories,
            onDismiss = { sheet = SheetType.NONE },
            onSave = { name, uri, goal, deadline, category ->
                if (editingMeta != null) {
                    viewModel.updateMeta(editingMeta!!.copy(name = name, imageUri = uri, goal = goal, deadlineMillis = deadline, category = category))
                } else {
                    viewModel.addMeta(name, uri, goal, deadline, category)
                }
                sheet = SheetType.NONE
            }
        )
        SheetType.DEPOSIT -> DepositSheet(
            quickAmounts = state.quickAmounts,
            onDismiss = { sheet = SheetType.NONE },
            onConfirm = { amount, type ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.registrar(amount, type)
                sheet = SheetType.NONE
            }
        )
        SheetType.WITHDRAW -> WithdrawSheet(
            pool = state.pool,
            onDismiss = { sheet = SheetType.NONE },
            onConfirm = { amount, type -> viewModel.restar(amount, type); sheet = SheetType.NONE }
        )
        SheetType.TRANSFORM -> TransformSheet(
            pool = state.pool,
            onDismiss = { sheet = SheetType.NONE },
            onConfirm = { amount, from -> viewModel.transformar(amount, from); sheet = SheetType.NONE }
        )
        SheetType.ITEM_MENU -> selectedItem?.let { item ->
            ItemMenuSheet(
                item = item,
                movements = state.movements,
                onDismiss = { sheet = SheetType.NONE },
                onEditar = { editingMeta = item.meta; sheet = SheetType.EDIT },
                onBorrar = { viewModel.deleteMeta(item.meta); sheet = SheetType.NONE },
                onComprado = { viewModel.marcarComprado(item.meta); sheet = SheetType.NONE }
            )
        }
        SheetType.DETAILS -> DetailsSheet(pool = state.pool, movements = state.movements, onDismiss = { sheet = SheetType.NONE })
        SheetType.HISTORY -> HistorySheet(
            movements = state.movements,
            onDelete = { viewModel.deleteMovement(it) },
            onDismiss = { sheet = SheetType.NONE }
        )
        SheetType.PURCHASED -> PurchasedSheet(purchased = state.purchased, onDismiss = { sheet = SheetType.NONE })
        SheetType.ACHIEVEMENTS -> AchievementsSheet(unlocked = state.achievements, onDismiss = { sheet = SheetType.NONE })
        SheetType.SETTINGS -> SettingsSheet(
            theme = state.theme,
            mode = state.themeMode,
            dynamicColor = state.dynamicColor,
            quickAmounts = state.quickAmounts,
            reminders = state.reminders,
            onDismiss = { sheet = SheetType.NONE },
            onTheme = { viewModel.setTheme(it) },
            onMode = { viewModel.setThemeMode(it) },
            onDynamicColor = { viewModel.setDynamicColor(it) },
            onQuickAmounts = { viewModel.setQuickAmounts(it) },
            onDailyReminder = { viewModel.setDailyReminder(it) },
            onReminderHour = { viewModel.setReminderHour(it) },
            onDeadlineReminder = { viewModel.setDeadlineReminder(it) }
        )
        SheetType.INFO -> InfoSheet(onDismiss = { sheet = SheetType.NONE })
        SheetType.NONE -> {}
    }

    if (newAchievements.isNotEmpty()) {
        AchievementUnlockedDialog(list = newAchievements, onDismiss = { newAchievements = emptyList() })
    }
}

@Composable
private fun HeaderSection(
    streak: Int,
    onHistory: () -> Unit,
    onPurchased: () -> Unit,
    onAchievements: () -> Unit,
    onDetails: () -> Unit,
    onSettings: () -> Unit,
    onInfo: () -> Unit
) {
    val animatedStreak by animateIntAsState(targetValue = streak, animationSpec = tween(600), label = "streak")
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.header_label), fontSize = 11.sp, letterSpacing = 0.12.sp, maxLines = 1,
                color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Row {
                HeaderIconButton(Icons.Filled.History, stringResource(R.string.cd_history), onHistory)
                HeaderIconButton(Icons.Filled.EmojiEvents, stringResource(R.string.cd_purchased), onPurchased)
                HeaderIconButton(Icons.Filled.WorkspacePremium, stringResource(R.string.cd_achievements), onAchievements)
                HeaderIconButton(Icons.Filled.BarChart, stringResource(R.string.cd_details), onDetails)
                HeaderIconButton(Icons.Filled.Settings, stringResource(R.string.cd_settings), onSettings)
                HeaderIconButton(Icons.Filled.Info, stringResource(R.string.cd_about), onInfo)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row {
            Text(stringResource(R.string.header_title_1), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.header_title_2), fontSize = 26.sp, fontWeight = FontWeight.Medium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.primary)
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
            Text(pluralText(R.plurals.streak_days, animatedStreak, animatedStreak), fontSize = 11.5.sp, color = GoldColor, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Botón de ícono de la cabecera: se ve de 30 dp pero el área táctil es de 40 dp. */
@Composable
private fun HeaderIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = description, tint = TextMutedColor, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    totalSaved: Double, totalGoal: Double, totalPct: Float, cashShare: Float,
    doneCount: Int, itemCount: Int, extra: Double,
    onRegistrar: () -> Unit, onRestar: () -> Unit, onTransformar: () -> Unit
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
                Text(fmtMoney(animatedSaved.toDouble()), fontSize = 30.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                Text(stringResource(R.string.summary_of_goal, fmtMoney(totalGoal)), fontSize = 13.sp, color = TextMutedColor)
            }
            Text("${animatedPct.toInt()}%", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(animatedPct / 100f, 0f..1f) }
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
            Text(stringResource(R.string.summary_in_bloom, doneCount, itemCount), fontSize = 12.sp, color = TextMutedColor)
            Row {
                LegendDot(CashColor, stringResource(R.string.money_cash_name))
                Spacer(Modifier.width(10.dp))
                LegendDot(DigitalColor, stringResource(R.string.money_digital_name))
            }
        }
        if (extra > 0) {
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.summary_extra, fmtMoney(extra)), fontSize = 12.sp, color = GoldColor)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRegistrar, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.summary_register)) }
            OutlinedButton(onClick = onRestar, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.summary_withdraw)) }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onTransformar, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.summary_transform)) }
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
