package com.alex.metasahorro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.ui.theme.SurfaceAltColor
import com.alex.metasahorro.ui.theme.TextMutedColor
import kotlinx.coroutines.launch

private data class OnboardingPage(val icon: String, val title: String, val body: String)

private val PAGES = listOf(
    OnboardingPage(
        icon = "🌱",
        title = "Bienvenido a Mis Metas",
        body = "Una app simple para juntar plata para lo que quieras comprar. Todo se guarda en tu teléfono, sin internet ni cuentas."
    ),
    OnboardingPage(
        icon = "💰",
        title = "Registra lo que ahorras",
        body = "Con \"+ Registrar\" sumas dinero a tu total, indicando si es efectivo o digital. Con \"− Restar\" lo descuentas si sacaste algo."
    ),
    OnboardingPage(
        icon = "🌳",
        title = "Cada meta compara contra tu total",
        body = "No se reparte la plata entre metas: si tu total alcanza el precio de algo, esa meta queda lista. Para las más caras, tu total es tu avance."
    ),
    OnboardingPage(
        icon = "🏆",
        title = "Marca, reordena y celebra",
        body = "Cuando una meta esté lista, tócala y marca \"Comprado\"  se descuenta el monto y queda guardada en tu archivo. Usa las flechas para reordenar, y revisa tu racha e historial cuando quieras."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == PAGES.lastIndex

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text("Saltar", color = TextMutedColor) }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val p = PAGES[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(SurfaceAltColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(p.icon, fontSize = 40.sp)
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    p.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    p.body,
                    fontSize = 14.sp,
                    color = TextMutedColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(PAGES.size) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (active) 20.dp else 7.dp, 7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary else SurfaceAltColor)
                )
            }
        }

        Button(
            onClick = {
                if (isLast) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (isLast) "Empezar" else "Siguiente")
        }
    }
}