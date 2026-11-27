package at.zanko.kalenderwecker.android

import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager

/**
 * Settings, toggles and the cached schedule live in device-protected storage
 * so alarms can be re-planned after a reboot before the user unlocks.
 */
fun deviceProtectedPrefs(context: Context, name: String): SharedPreferences {
    val deviceContext = context.createDeviceProtectedStorageContext()
    if (isUserUnlocked(context)) deviceContext.moveSharedPreferencesFrom(context, name)
    return deviceContext.getSharedPreferences(name, Context.MODE_PRIVATE)
}

fun isUserUnlocked(context: Context): Boolean =
    context.getSystemService(UserManager::class.java).isUserUnlocked
