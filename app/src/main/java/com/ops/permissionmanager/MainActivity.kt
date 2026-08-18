package com.ops.permissionmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.permissionmanager.core.ui.OpsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle()
            val systemDarkTheme = isSystemInDarkTheme()

            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDarkTheme
            }

            OpsTheme(
                darkTheme = darkTheme,
                dynamicColor = true
            ) {
                val windowBackground = MaterialTheme.colorScheme.background
                val navigationBarColor = MaterialTheme.colorScheme.surfaceContainer
                SideEffect {
                    window.decorView.setBackgroundColor(windowBackground.toArgb())
                    window.navigationBarColor = navigationBarColor.toArgb()
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
                }

                OpsApp()
            }
        }
    }
}

@Composable
fun OpsApp(modifier: Modifier = Modifier) {
    val viewModel: RootCheckViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isChecking -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.isAnyAvailable -> OpsNavHost(modifier = modifier)
        else -> RootGuideScreen(onRetry = viewModel::checkAvailability)
    }
}
