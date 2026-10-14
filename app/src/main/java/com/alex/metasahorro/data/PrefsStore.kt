package com.alex.metasahorro.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.prefsDataStore by preferencesDataStore(name = "prefs")
private val QUICK_AMOUNTS_KEY = stringPreferencesKey("quick_amounts")
private val DAILY_REMINDER_KEY = booleanPreferencesKey("reminder_daily")
private val REMINDER_HOUR_KEY = intPreferencesKey("reminder_hour")
private val DEADLINE_REMINDER_KEY = booleanPreferencesKey("reminder_deadline")
private val ACHIEVEMENTS_SEEDED_KEY = booleanPreferencesKey("achievements_seeded")

/** Montos de los botones rápidos al registrar dinero, mientras el usuario no los cambie. */
val DEFAULT_QUICK_AMOUNTS = listOf(5_000, 10_000, 20_000)

/** Cuántos botones rápidos como máximo. */
const val MAX_QUICK_AMOUNTS = 3

/** Ajustes de los recordatorios: aviso diario (si no registraste hoy) a cierta hora, y aviso de metas por vencer. */
data class ReminderSettings(
    val dailyEnabled: Boolean = false,
    val hour: Int = 20,
    val deadlineEnabled: Boolean = false
)

/**
 * Preferencias del usuario que no son del tema: montos rápidos, recordatorios y una marca de que los
 * logros ya se sembraron. Usa su propio DataStore ("prefs"); el del tema es [ThemeStore].
 */
class PrefsStore(private val context: Context) {

    /** Montos rápidos, de 1 a [MAX_QUICK_AMOUNTS] valores positivos. Si lo guardado no es válido, los de fábrica. */
    val quickAmounts = context.prefsDataStore.data.map { prefs ->
        parseQuickAmounts(prefs[QUICK_AMOUNTS_KEY])
    }

    val reminders = context.prefsDataStore.data.map { prefs ->
        ReminderSettings(
            dailyEnabled = prefs[DAILY_REMINDER_KEY] ?: false,
            hour = (prefs[REMINDER_HOUR_KEY] ?: 20).coerceIn(0, 23),
            deadlineEnabled = prefs[DEADLINE_REMINDER_KEY] ?: false
        )
    }

    /** True cuando los logros que ya merecía el usuario se desbloquearon en silencio (sin celebrar todos juntos). */
    val achievementsSeeded = context.prefsDataStore.data.map { it[ACHIEVEMENTS_SEEDED_KEY] ?: false }

    suspend fun setQuickAmounts(amounts: List<Int>) {
        val clean = amounts.filter { it > 0 }.take(MAX_QUICK_AMOUNTS)
        context.prefsDataStore.edit { it[QUICK_AMOUNTS_KEY] = (clean.ifEmpty { DEFAULT_QUICK_AMOUNTS }).joinToString(",") }
    }

    suspend fun setDailyReminder(enabled: Boolean) {
        context.prefsDataStore.edit { it[DAILY_REMINDER_KEY] = enabled }
    }

    suspend fun setReminderHour(hour: Int) {
        context.prefsDataStore.edit { it[REMINDER_HOUR_KEY] = hour.coerceIn(0, 23) }
    }

    suspend fun setDeadlineReminder(enabled: Boolean) {
        context.prefsDataStore.edit { it[DEADLINE_REMINDER_KEY] = enabled }
    }

    suspend fun markAchievementsSeeded() {
        context.prefsDataStore.edit { it[ACHIEVEMENTS_SEEDED_KEY] = true }
    }

    companion object {
        fun parseQuickAmounts(raw: String?): List<Int> {
            val parsed = raw?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.filter { it > 0 }?.take(MAX_QUICK_AMOUNTS)
            return if (parsed.isNullOrEmpty()) DEFAULT_QUICK_AMOUNTS else parsed
        }
    }
}
