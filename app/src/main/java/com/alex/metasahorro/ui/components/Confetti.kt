package com.alex.metasahorro.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.launch
import kotlin.random.Random

private val CONFETTI_COLORS = listOf(
    Color(0xFF7CD991), Color(0xFFF2B84B), Color(0xFFF2665C), Color(0xFF5B9DF2), Color(0xFFC9A6FF)
)

private data class Piece(
    val startX: Float,
    val color: Color,
    val rotation: Float,
    val delayMs: Int
)

/**
 * Lluvia de confeti que se dispara cada vez que "trigger" cambia de valor.
 * Se superpone sobre la pantalla mientras dura la animación (~1.3s) y luego desaparece sola.
 */
@Composable
fun ConfettiOverlay(trigger: Int, modifier: Modifier = Modifier) {
    var pieces by remember { mutableStateOf(emptyList<Piece>()) }
    var visible by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        pieces = List(28) {
            Piece(
                startX = Random.nextFloat(),
                color = CONFETTI_COLORS[Random.nextInt(CONFETTI_COLORS.size)],
                rotation = Random.nextFloat() * 360f,
                delayMs = Random.nextInt(0, 200)
            )
        }
        visible = true
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(1300, easing = LinearEasing))
        visible = false
    }

    if (visible) {
        Canvas(modifier = modifier.fillMaxSize()) {
            pieces.forEach { piece ->
                val localProgress = (progress.value - piece.delayMs / 1300f).coerceIn(0f, 1f)
                val y = size.height * -0.05f + (size.height * 1.05f) * localProgress
                val x = piece.startX * size.width
                val alpha = 1f - localProgress
                rotate(piece.rotation + localProgress * 540f, pivot = Offset(x, y)) {
                    drawRect(
                        color = piece.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                        topLeft = Offset(x - 4f, y - 7f),
                        size = Size(8f, 14f)
                    )
                }
            }
        }
    }
}
