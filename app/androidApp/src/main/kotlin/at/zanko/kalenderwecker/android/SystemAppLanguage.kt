package at.zanko.kalenderwecker.android

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

/**
 * Mirrors a language picked in the app into Android's per-app language (13+),
 * so the alarm notification and the system settings show the same language.
 * Only called when the user changes it in-app, never at startup, so a language
 * chosen in the system settings is not overwritten.
 */
fun syncSystemAppLanguage(context: Context, tag: String?) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val localeManager = context.getSystemService(LocaleManager::class.java)
    val target = LocaleList.forLanguageTags(tag.orEmpty())
    if (localeManager.applicationLocales != target) localeManager.setApplicationLocales(target)
}
