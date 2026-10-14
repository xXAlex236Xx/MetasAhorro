package com.alex.metasahorro.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alex.metasahorro.R
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")
private val THEME_KEY = stringPreferencesKey("accent_theme")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")

/** Color de acento elegido (verde, morado, rojo o azul). Se ignora si están activados los colores dinámicos. */
enum class AccentTheme(@StringRes val label: Int) {
    GREEN(R.string.accent_green), PURPLE(R.string.accent_purple), RED(R.string.accent_red), BLUE(R.string.accent_blue)
}

/** Apariencia general: seguir al sistema, siempre claro o siempre oscuro. */
enum class ThemeMode(@StringRes val label: Int) {
    SYSTEM(R.string.mode_system), LIGHT(R.string.mode_light), DARK(R.string.mode_dark)
}

/** Guarda la apariencia: acento, modo claro/oscuro/sistema y si se usan los colores dinámicos (Material You, Android 12+). */
class ThemeStore(private val context: Context) {

    val theme = context.dataStore.data.map { prefs ->
        AccentTheme.entries.firstOrNull { it.name == prefs[THEME_KEY] } ?: AccentTheme.GREEN
    }

    /** Por defecto oscuro, que es como se veía la app antes de tener modo claro. */
    val mode = context.dataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[THEME_MODE_KEY] } ?: ThemeMode.DARK
    }

    val dynamicColor = context.dataStore.data.map { prefs -> prefs[DYNAMIC_COLOR_KEY] ?: false }

    suspend fun setTheme(theme: AccentTheme) {
        context.dataStore.edit { it[THEME_KEY] = theme.name }
    }

    suspend fun setMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }
}
