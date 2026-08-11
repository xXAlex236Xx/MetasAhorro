package com.alex.metasahorro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.alex.metasahorro.data.AccentTheme

val BgColor = Color(0xFF0E1712)
val SurfaceColor = Color(0xFF17241C)
val SurfaceAltColor = Color(0xFF1E2F24)
val BorderColor = Color(0xFF2A3E30)
val TextColor = Color(0xFFF2F5F0)
val TextMutedColor = Color(0xFF8FA396)
val GoldColor = Color(0xFFF2B84B)
val CashColor = Color(0xFFF2665C)
val DigitalColor = Color(0xFF5B9DF2)

data class AccentPair(val accent: Color, val accentDim: Color)

fun accentFor(theme: AccentTheme): AccentPair = when (theme) {
    AccentTheme.GREEN -> AccentPair(Color(0xFF7CD991), Color(0xFF4F8461))
    AccentTheme.PURPLE -> AccentPair(Color(0xFFC9A6FF), Color(0xFF7C5CC4))
    AccentTheme.RED -> AccentPair(Color(0xFFF2836B), Color(0xFFC24F3B))
    AccentTheme.BLUE -> AccentPair(Color(0xFF6FB8F2), Color(0xFF3E7FBF))
}

@Composable
fun MetasAhorroTheme(theme: AccentTheme, content: @Composable () -> Unit) {
    val accent = accentFor(theme)
    val colorScheme = darkColorScheme(
        primary = accent.accent,
        background = BgColor,
        surface = SurfaceColor,
        onBackground = TextColor,
        onSurface = TextColor,
        secondary = GoldColor,
        error = CashColor
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MetasTypography,
        content = content
    )
}
