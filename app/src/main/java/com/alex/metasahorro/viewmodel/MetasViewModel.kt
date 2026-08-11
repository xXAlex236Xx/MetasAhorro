package com.alex.metasahorro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alex.metasahorro.data.*
import com.alex.metasahorro.repository.MetasRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class MetaConSaldo(
    val meta: Meta,
    val saved: Double,
    val complete: Boolean
)

data class MetasUiState(
    val metas: List<MetaConSaldo> = emptyList(),
    val purchased: List<Purchased> = emptyList(),
    val movements: List<Movement> = emptyList(),
    val pool: PoolState = PoolState(),
    val theme: AccentTheme = AccentTheme.GREEN,
    val streak: Int = 0
)

class MetasViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MetasRepository(AppDatabase.getInstance(application).dao())
    private val themeStore = ThemeStore(application)
    private val onboardingStore = OnboardingStore(application)

    val hasSeenOnboarding: StateFlow<Boolean?> = onboardingStore.hasSeenOnboarding
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    fun completeOnboarding() {
        viewModelScope.launch { onboardingStore.markSeen() }
    }

    val uiState: StateFlow<MetasUiState> = combine(
        repo.metas, repo.pool, repo.purchased, repo.movements, themeStore.theme
    ) { metas, pool, purchased, movements, theme ->
        MetasUiState(
            metas = metas.map { m ->
                val saved = repo.savedFor(m, pool)
                MetaConSaldo(m, saved, saved >= m.goal - 0.01)
            },
            purchased = purchased,
            movements = movements,
            pool = pool,
            theme = theme,
            streak = computeStreak(movements)
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), MetasUiState())

    private fun computeStreak(movements: List<Movement>): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val depositDays = movements.filter { it.kind == MovementKind.DEPOSIT }
            .map { fmt.format(Date(it.tsMillis)) }
            .toSet()
        var streak = 0
        val cal = Calendar.getInstance()
        while (true) {
            val key = fmt.format(cal.time)
            if (depositDays.contains(key)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        return streak
    }

    // ---- Acciones ----
    fun addMeta(name: String, imageUri: String?, goal: Double, deadlineMillis: Long?) {
        viewModelScope.launch { repo.addMeta(name, imageUri, goal, deadlineMillis) }
    }

    fun updateMeta(meta: Meta) {
        viewModelScope.launch { repo.updateMeta(meta) }
    }

    fun deleteMeta(meta: Meta) {
        viewModelScope.launch { repo.deleteMeta(meta) }
    }

    fun moveMeta(meta: Meta, direction: Int) {
        viewModelScope.launch { repo.moveMeta(uiState.value.metas.map { it.meta }, meta, direction) }
    }

    fun registrar(amount: Double, type: MoneyType) {
        viewModelScope.launch { repo.registrar(uiState.value.pool, amount, type) }
    }

    /** Devuelve false (vía callback) si no había saldo suficiente para restar. */
    fun restar(amount: Double, type: MoneyType, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repo.restar(uiState.value.pool, amount, type)) }
    }

    fun marcarComprado(meta: Meta) {
        viewModelScope.launch { repo.marcarComprado(uiState.value.pool, meta) }
    }

    fun setTheme(theme: AccentTheme) {
        viewModelScope.launch { themeStore.setTheme(theme) }
    }
}
