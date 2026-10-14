package com.alex.metasahorro.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alex.metasahorro.R
import com.alex.metasahorro.data.*
import com.alex.metasahorro.notifications.Reminders
import com.alex.metasahorro.repository.Achievement
import com.alex.metasahorro.repository.AchievementStats
import com.alex.metasahorro.repository.MetasRepository
import com.alex.metasahorro.repository.UndoAction
import com.alex.metasahorro.repository.achievementsReached
import com.alex.metasahorro.repository.computeStreak
import com.alex.metasahorro.repository.normalizeCategory
import com.alex.metasahorro.util.fmtMoney
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Una meta junto con cuánto lleva ahorrado y si ya está completa. */
data class MetaConSaldo(
    val meta: Meta,
    val saved: Double,
    val complete: Boolean
)

/** Todo lo que necesita la pantalla en un solo objeto: metas, comprados, historial, pozo, logros, ajustes y racha. */
data class MetasUiState(
    val metas: List<MetaConSaldo> = emptyList(),
    val purchased: List<Purchased> = emptyList(),
    val movements: List<Movement> = emptyList(),
    val pool: PoolState = PoolState(),
    val achievements: List<AchievementUnlock> = emptyList(),
    val theme: AccentTheme = AccentTheme.GREEN,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val quickAmounts: List<Int> = DEFAULT_QUICK_AMOUNTS,
    val reminders: ReminderSettings = ReminderSettings(),
    val streak: Int = 0
) {
    /** Categorías en uso (metas activas y compradas), sin repetir ni distinguir mayúsculas, en orden alfabético. */
    val categories: List<String>
        get() = (metas.map { it.meta.category } + purchased.map { it.category })
            .filterNotNull()
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
}

/** Cosas que la pantalla debe mostrar una sola vez (no son estado): avisos, celebraciones y logros nuevos. */
sealed interface UiEvent {
    /** Aviso corto abajo, con "Deshacer" si `undo` no es null. */
    data class Message(@StringRes val res: Int, val args: List<Any> = emptyList(), val undo: UndoAction? = null) : UiEvent

    /** Una meta que estaba incompleta acaba de llegar al 100%. */
    data class GoalReached(val name: String) : UiEvent

    /** Logros que se acaban de desbloquear. */
    data class AchievementsUnlocked(val list: List<Achievement>) : UiEvent
}

/** Combina los flujos de Room y DataStore en un único `uiState`, expone las acciones de la pantalla y emite [UiEvent]. */
class MetasViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MetasRepository(AppDatabase.getInstance(application))
    private val themeStore = ThemeStore(application)
    private val prefs = PrefsStore(application)
    private val onboardingStore = OnboardingStore(application)

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val hasSeenOnboarding: StateFlow<Boolean?> = onboardingStore.hasSeenOnboarding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun completeOnboarding() {
        viewModelScope.launch { onboardingStore.markSeen() }
    }

    private data class Data(
        val metas: List<Meta>, val pool: PoolState, val purchased: List<Purchased>,
        val movements: List<Movement>, val achievements: List<AchievementUnlock>
    )

    private data class Settings(
        val theme: AccentTheme, val mode: ThemeMode, val dynamic: Boolean,
        val quick: List<Int>, val reminders: ReminderSettings
    )

    private val dataFlow = combine(
        repo.metas, repo.pool, repo.purchased, repo.movements, repo.achievements, ::Data
    )

    private val settingsFlow = combine(
        themeStore.theme, themeStore.mode, themeStore.dynamicColor, prefs.quickAmounts, prefs.reminders, ::Settings
    )

    val uiState: StateFlow<MetasUiState> = combine(dataFlow, settingsFlow) { d, s ->
        MetasUiState(
            metas = d.metas.map { m ->
                val saved = repo.savedFor(m, d.pool)
                MetaConSaldo(m, saved, saved >= m.goal - 0.01)
            },
            purchased = d.purchased,
            movements = d.movements,
            pool = d.pool,
            achievements = d.achievements,
            theme = s.theme,
            themeMode = s.mode,
            dynamicColor = s.dynamic,
            quickAmounts = s.quick,
            reminders = s.reminders,
            streak = computeStreak(d.movements.filter { it.kind == MovementKind.DEPOSIT }.map { it.tsMillis })
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetasUiState())

    init {
        viewModelScope.launch { Reminders.sync(getApplication(), prefs.reminders.first()) }
        viewModelScope.launch { watchForCelebrations() }
    }

    /**
     * Mira cada cambio de datos para detectar (1) metas que pasan de incompletas a completas y
     * (2) logros nuevos. La primera vez que se corre con datos ya existentes, los logros que el usuario ya
     * merecía se guardan en silencio para no celebrar todo junto.
     */
    private suspend fun watchForCelebrations() {
        var previousComplete: Map<Long, Boolean>? = null
        var seeded: Boolean? = null
        dataFlow.collect { d ->
            val complete = d.metas.associate { it.id to (repo.savedFor(it, d.pool) >= it.goal - 0.01) }
            previousComplete?.let { before ->
                d.metas.filter { before[it.id] == false && complete[it.id] == true }
                    .forEach { _events.tryEmit(UiEvent.GoalReached(it.name)) }
            }
            previousComplete = complete

            val deposits = d.movements.filter { it.kind == MovementKind.DEPOSIT }
            val stats = AchievementStats(
                deposits = deposits.size,
                transforms = d.movements.count { it.kind == MovementKind.TRANSFORM },
                purchases = d.purchased.size,
                streak = computeStreak(deposits.map { it.tsMillis }),
                totalSaved = d.pool.cash + d.pool.digital,
                categories = (d.metas.map { it.category } + d.purchased.map { it.category })
                    .filterNotNull().distinctBy { it.lowercase() }.size
            )
            val unlockedIds = d.achievements.map { it.id }.toSet()
            val fresh = achievementsReached(stats).filter { it.name !in unlockedIds }

            val alreadySeeded = seeded ?: prefs.achievementsSeeded.first().also { seeded = it }
            if (!alreadySeeded) {
                repo.unlockAchievements(fresh)
                prefs.markAchievementsSeeded()
                seeded = true
            } else if (fresh.isNotEmpty()) {
                val inserted = repo.unlockAchievements(fresh)
                if (inserted.isNotEmpty()) _events.tryEmit(UiEvent.AchievementsUnlocked(inserted))
            }
        }
    }

    private fun message(@StringRes res: Int, args: List<Any> = emptyList(), undo: UndoAction? = null) {
        _events.tryEmit(UiEvent.Message(res, args, undo))
    }

    fun addMeta(name: String, imageUri: String?, goal: Double, deadlineMillis: Long?, category: String?) {
        viewModelScope.launch { repo.addMeta(name, imageUri, goal, deadlineMillis, category) }
    }

    fun updateMeta(meta: Meta) {
        viewModelScope.launch { repo.updateMeta(meta) }
    }

    fun deleteMeta(meta: Meta) {
        viewModelScope.launch {
            val undo = repo.deleteMeta(meta)
            message(R.string.msg_goal_deleted, listOf(meta.name), undo)
        }
    }

    fun moveMeta(meta: Meta, direction: Int) {
        viewModelScope.launch { repo.moveMeta(uiState.value.metas.map { it.meta }, meta, direction) }
    }

    fun registrar(amount: Double, type: MoneyType) {
        viewModelScope.launch {
            repo.registrar(amount, type)?.let { message(R.string.msg_deposit_done, listOf(fmtMoney(amount)), it) }
        }
    }

    fun restar(amount: Double, type: MoneyType) {
        viewModelScope.launch {
            val undo = repo.restar(amount, type)
            if (undo != null) message(R.string.msg_withdraw_done, listOf(fmtMoney(amount)), undo)
            else message(R.string.err_withdraw_no_balance)
        }
    }

    fun transformar(amount: Double, from: MoneyType) {
        viewModelScope.launch {
            val undo = repo.transformar(amount, from)
            if (undo != null) message(R.string.msg_transform_done, listOf(fmtMoney(amount)), undo)
            else message(R.string.err_transform_no_balance)
        }
    }

    fun marcarComprado(meta: Meta) {
        viewModelScope.launch {
            val undo = repo.marcarComprado(meta)
            if (undo != null) message(R.string.msg_purchase_done, listOf(meta.name), undo)
            else message(R.string.err_purchase_not_enough)
        }
    }

    /** Deshace una operación reciente (el botón "Deshacer" del aviso). */
    fun undo(action: UndoAction) {
        viewModelScope.launch {
            if (repo.undo(action)) message(R.string.msg_undone) else message(R.string.err_undo_failed)
        }
    }

    /** Borra un movimiento del historial revirtiendo su efecto en el pozo. */
    fun deleteMovement(movement: Movement) {
        viewModelScope.launch {
            if (repo.revertMovement(movement.id)) message(R.string.msg_movement_deleted)
            else message(R.string.err_movement_cannot_delete)
        }
    }

    fun setTheme(theme: AccentTheme) {
        viewModelScope.launch { themeStore.setTheme(theme) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeStore.setMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { themeStore.setDynamicColor(enabled) }
    }

    fun setQuickAmounts(amounts: List<Int>) {
        viewModelScope.launch { prefs.setQuickAmounts(amounts) }
    }

    fun setDailyReminder(enabled: Boolean) = updateReminders { prefs.setDailyReminder(enabled) }

    fun setReminderHour(hour: Int) = updateReminders { prefs.setReminderHour(hour) }

    fun setDeadlineReminder(enabled: Boolean) = updateReminders { prefs.setDeadlineReminder(enabled) }

    /** Aplica un cambio en los ajustes de recordatorios y reprograma el aviso diario con el resultado. */
    private fun updateReminders(change: suspend () -> Unit) {
        viewModelScope.launch {
            change()
            Reminders.sync(getApplication(), prefs.reminders.first())
        }
    }
}
