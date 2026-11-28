package at.zanko.kalenderwecker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf

/** In-app language switching is implemented for Android and desktop; here the system language is used. */
actual object LocalAppLocale {
    private val local = staticCompositionLocalOf<String?> { null }

    actual val current: String
        @Composable get() = local.current ?: ""

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> = local.provides(value)
}
