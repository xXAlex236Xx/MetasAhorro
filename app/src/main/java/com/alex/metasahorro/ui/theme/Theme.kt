package com.alex.metasahorro.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.alex.metasahorro.data.AccentTheme
import com.alex.metasahorro.data.ThemeMode

/**
 * Colores propios de la app que Material no cubre (superficies, bordes, texto atenuado y los colores de
 * efectivo/digital). Cambian con el modo claro/oscuro y con los colores dinámicos.
 */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val text: Color,
    val textMuted: Color,
    val gold: Color,
    val cash: Color,
    val digital: Color
)

private val DarkAppColors = AppColors(
    isDark = true,
    bg = Color(0xFF0E1712), surface = Color(0xFF17241C), surfaceAlt = Color(0xFF1E2F24),
    border = Color(0xFF2A3E30), text = Color(0xFFF2F5F0), textMuted = Color(0xFF8FA396),
    gold = Color(0xFFF2B84B), cash = Color(0xFFF2665C), digital = Color(0xFF5B9DF2)
)

private val LightAppColors = AppColors(
    isDark = false,
    bg = Color(0xFFF4F8F3), surface = Color(0xFFFFFFFF), surfaceAlt = Color(0xFFE8F0E7),
    border = Color(0xFFCFDCCF), text = Color(0xFF14231A), textMuted = Color(0xFF5A6E60),
    gold = Color(0xFF9A6414), cash = Color(0xFFC23A31), digital = Color(0xFF2F6FD0)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

val BgColor: Color @Composable get() = LocalAppColors.current.bg
val SurfaceColor: Color @Composable get() = LocalAppColors.current.surface
val SurfaceAltColor: Color @Composable get() = LocalAppColors.current.surfaceAlt
val BorderColor: Color @Composable get() = LocalAppColors.current.border
val TextColor: Color @Composable get() = LocalAppColors.current.text
val TextMutedColor: Color @Composable get() = LocalAppColors.current.textMuted
val GoldColor: Color @Composable get() = LocalAppColors.current.gold
val CashColor: Color @Composable get() = LocalAppColors.current.cash
val DigitalColor: Color @Composable get() = LocalAppColors.current.digital

data class AccentPair(val accent: Color, val accentDim: Color)

/** Color de acento del tema elegido, con una versión más oscura para que tenga contraste sobre fondo claro. */
fun accentFor(theme: AccentTheme, dark: Boolean = true): AccentPair = if (dark) {
    when (theme) {
        AccentTheme.GREEN -> AccentPair(Color(0xFF7CD991), Color(0xFF4F8461))
        AccentTheme.PURPLE -> AccentPair(Color(0xFFC9A6FF), Color(0xFF7C5CC4))
        AccentTheme.RED -> AccentPair(Color(0xFFF2836B), Color(0xFFC24F3B))
        AccentTheme.BLUE -> AccentPair(Color(0xFF6FB8F2), Color(0xFF3E7FBF))
    }
} else {
    when (theme) {
        AccentTheme.GREEN -> AccentPair(Color(0xFF2E7D4A), Color(0xFF7CD991))
        AccentTheme.PURPLE -> AccentPair(Color(0xFF6D4BC2), Color(0xFFC9A6FF))
        AccentTheme.RED -> AccentPair(Color(0xFFC24F3B), Color(0xFFF2836B))
        AccentTheme.BLUE -> AccentPair(Color(0xFF2B6FB5), Color(0xFF6FB8F2))
    }
}

private fun paletteScheme(app: AppColors, accent: Color, inverseAccent: Color): ColorScheme {
    val onAccent = if (app.isDark) app.bg else Color.White
    return if (app.isDark) {
        darkColorScheme(
            primary = accent, onPrimary = onAccent, background = app.bg, surface = app.surface,
            onBackground = app.text, onSurface = app.text, secondary = app.gold, error = app.cash,
            inverseSurface = app.text, inverseOnSurface = app.bg, inversePrimary = inverseAccent,
            surfaceVariant = app.surfaceAlt, onSurfaceVariant = app.textMuted,
            outline = app.border, outlineVariant = app.border,
            surfaceContainerLowest = app.bg, surfaceContainerLow = app.surface, surfaceContainer = app.surface,
            surfaceContainerHigh = app.surfaceAlt, surfaceContainerHighest = app.surfaceAlt
        )
    } else {
        lightColorScheme(
            primary = accent, onPrimary = onAccent, background = app.bg, surface = app.surface,
            onBackground = app.text, onSurface = app.text, secondary = app.gold, error = app.cash,
            inverseSurface = app.text, inverseOnSurface = app.bg, inversePrimary = inverseAccent,
            surfaceVariant = app.surfaceAlt, onSurfaceVariant = app.textMuted,
            outline = app.border, outlineVariant = app.border,
            surfaceContainerLowest = app.bg, surfaceContainerLow = app.surface, surfaceContainer = app.surface,
            surfaceContainerHigh = app.surfaceAlt, surfaceContainerHighest = app.surfaceAlt
        )
    }
}

/**
 * Tema de la app. `mode` elige claro, oscuro o el del sistema. Con `dynamicColor` (solo Android 12+) los
 * colores salen del fondo de pantalla del usuario y el acento elegido se ignora. También ajusta las barras
 * del sistema para que sus íconos se vean sobre el fondo actual.
 */
@Composable
fun MetasAhorroTheme(
    theme: AccentTheme,
    mode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val base = if (dark) DarkAppColors else LightAppColors

    val colorScheme: ColorScheme
    val appColors: AppColors
    if (useDynamic) {
        colorScheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        appColors = base.copy(
            bg = colorScheme.background, surface = colorScheme.surfaceContainer,
            surfaceAlt = colorScheme.surfaceContainerHigh, border = colorScheme.outlineVariant,
            text = colorScheme.onBackground, textMuted = colorScheme.onSurfaceVariant
        )
    } else {
        // El acento "inverso" es el del modo contrario: se usa sobre los avisos (snackbar), que tienen el fondo invertido.
        colorScheme = paletteScheme(base, accentFor(theme, dark).accent, accentFor(theme, !dark).accent)
        appColors = base
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = appColors.bg.toArgb()
            window.navigationBarColor = appColors.bg.toArgb()
            window.setBackgroundDrawable(ColorDrawable(appColors.bg.toArgb()))
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(colorScheme = colorScheme, typography = MetasTypography, content = content)
    }
}
