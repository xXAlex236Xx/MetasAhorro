package com.alex.metasahorro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.alex.metasahorro.ui.MainScreen
import com.alex.metasahorro.ui.OnboardingScreen
import com.alex.metasahorro.ui.theme.MetasAhorroTheme
import com.alex.metasahorro.viewmodel.MetasViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MetasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        splash.setKeepOnScreenCondition { viewModel.hasSeenOnboarding.value == null }

        setContent {
            val state by viewModel.uiState.collectAsState()
            val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()

            MetasAhorroTheme(theme = state.theme) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (hasSeenOnboarding) {
                        null -> { /* esperando a que cargue la preferencia; el splash sigue visible */ }
                        false -> OnboardingScreen(onFinish = { viewModel.completeOnboarding() })
                        true -> MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
