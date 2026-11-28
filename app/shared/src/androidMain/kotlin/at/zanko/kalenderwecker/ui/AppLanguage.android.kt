package at.zanko.kalenderwecker.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

actual object LocalAppLocale {
    private var systemLocale: Locale? = null

    actual val current: String
        @Composable get() = Locale.getDefault().toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val base = LocalConfiguration.current
        if (systemLocale == null) systemLocale = Locale.getDefault()
        val locale = value?.let(Locale::forLanguageTag) ?: systemLocale!!
        Locale.setDefault(locale)
        // Compose resources on Android resolve against LocalConfiguration.
        val configuration = Configuration(base).apply { setLocale(locale) }
        return LocalConfiguration.provides(configuration)
    }
}
