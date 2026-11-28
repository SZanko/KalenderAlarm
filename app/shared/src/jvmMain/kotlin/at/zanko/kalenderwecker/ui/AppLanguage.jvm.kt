package at.zanko.kalenderwecker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

actual object LocalAppLocale {
    private val systemLocale: Locale = Locale.getDefault()
    private val local = staticCompositionLocalOf { Locale.getDefault().toString() }

    actual val current: String
        @Composable get() = local.current

    /** Applies the stored language before any UI exists (window titles, the systemd ring process). */
    fun applyAtStartup(tag: String?) {
        Locale.setDefault(tag?.let(Locale::forLanguageTag) ?: systemLocale)
    }

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        // Compose resources read Locale.getDefault() on the JVM.
        val locale = value?.let(Locale::forLanguageTag) ?: systemLocale
        Locale.setDefault(locale)
        return local.provides(locale.toString())
    }
}
