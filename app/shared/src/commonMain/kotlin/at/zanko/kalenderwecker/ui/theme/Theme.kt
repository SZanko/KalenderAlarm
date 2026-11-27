package at.zanko.kalenderwecker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Android 12+ wallpaper colours; null where the platform has none. */
@Composable
expect fun dynamicColorScheme(dark: Boolean): ColorScheme?

private val SunriseDark = darkColorScheme(
    primary = Color(0xFFFFB68B),
    onPrimary = Color(0xFF522300),
    primaryContainer = Color(0xFF743500),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFBBC3FF),
    secondaryContainer = Color(0xFF2F3A8B),
    tertiary = Color(0xFF9CD3B5),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KalenderWeckerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = dynamicColorScheme(dark) ?: if (dark) {
        SunriseDark
    } else {
        expressiveLightColorScheme().copy(
            primary = Color(0xFF964900),
            primaryContainer = Color(0xFFFFDBC8),
            onPrimaryContainer = Color(0xFF311300),
            secondary = Color(0xFF4752A5),
            secondaryContainer = Color(0xFFDFE0FF),
        )
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
