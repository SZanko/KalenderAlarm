package at.zanko.kalenderwecker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.zanko.kalenderwecker.data.SettingsRepository
import org.koin.compose.koinInject

data class AppLanguage(val tag: String, val nativeName: String)

object AppLanguages {
    /** Languages with translations; names stay in their own language so everyone finds theirs. */
    val supported = listOf(
        AppLanguage("en", "English"),
        AppLanguage("de", "Deutsch"),
        AppLanguage("sv", "Svenska"),
    )
}

/**
 * Overrides the locale Compose resources resolve against. `null` restores the
 * system language. Platform actuals follow the Compose Multiplatform guide for
 * in-app locale switching.
 */
expect object LocalAppLocale {
    val current: String @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

/** Shows [content] in the language chosen in settings; switching re-creates it in the new language. */
@Composable
fun LocalizedContent(content: @Composable () -> Unit) {
    val settings = koinInject<SettingsRepository>()
    val state by settings.settings.collectAsStateWithLifecycle()
    val language = state.language
    CompositionLocalProvider(LocalAppLocale provides language) {
        key(language) { content() }
    }
}
