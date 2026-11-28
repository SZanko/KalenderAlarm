package at.zanko.kalenderwecker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import at.zanko.kalenderwecker.ui.LocalizedContent
import at.zanko.kalenderwecker.ui.home.HomeScreen
import at.zanko.kalenderwecker.ui.settings.SettingsScreen
import at.zanko.kalenderwecker.ui.theme.KalenderWeckerTheme

/**
 * Root composable. [banner] lets a platform show status above the content
 * (e.g. a missing Android permission or the desktop "app must stay open" note).
 */
@Composable
fun App(banner: @Composable () -> Unit = {}) = LocalizedContent {
    KalenderWeckerTheme {
        var showSettings by rememberSaveable { mutableStateOf(false) }
        PlatformBackHandler(enabled = showSettings) { showSettings = false }
        AnimatedContent(
            targetState = showSettings,
            transitionSpec = {
                val dir = if (targetState) 1 else -1
                slideInHorizontally { it * dir } togetherWith slideOutHorizontally { -it * dir }
            },
        ) { settings ->
            if (settings) {
                SettingsScreen(onBack = { showSettings = false }, banner = banner)
            } else {
                HomeScreen(onOpenSettings = { showSettings = true }, banner = banner)
            }
        }
    }
}

@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
