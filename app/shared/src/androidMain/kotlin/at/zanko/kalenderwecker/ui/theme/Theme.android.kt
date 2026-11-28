package at.zanko.kalenderwecker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// minSdk 31 always has Material You colours.
@Composable
actual fun dynamicColorScheme(dark: Boolean): ColorScheme? {
    val context = LocalContext.current
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
