package com.alex.metasahorro.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")
private val THEME_KEY = stringPreferencesKey("accent_theme")

enum class AccentTheme(val label: String) {
    GREEN("Verde"), PURPLE("Morado"), RED("Rojo"), BLUE("Azul")
}

class ThemeStore(private val context: Context) {

    val theme = context.dataStore.data.map { prefs ->
        AccentTheme.valueOf(prefs[THEME_KEY] ?: AccentTheme.GREEN.name)
    }

    suspend fun setTheme(theme: AccentTheme) {
        context.dataStore.edit { it[THEME_KEY] = theme.name }
    }
}
