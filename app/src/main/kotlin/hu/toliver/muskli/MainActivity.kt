package hu.toliver.muskli

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import hu.toliver.muskli.ui.home.HomeScreen
import hu.toliver.muskli.ui.settings.SettingsRoute
import hu.toliver.muskli.ui.theme.MuskliTheme

/** Screens reachable from home; no navigation library yet (owner choice, T-017). */
private enum class Screen { HOME, SETTINGS }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MuskliTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                val goHome = { screen = Screen.HOME }
                BackHandler(enabled = screen != Screen.HOME, onBack = goHome)
                when (screen) {
                    Screen.HOME -> HomeScreen(onOpenSettings = { screen = Screen.SETTINGS })
                    Screen.SETTINGS -> SettingsRoute(onBack = goHome)
                }
            }
        }
    }
}
