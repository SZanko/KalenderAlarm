package at.zanko.kalenderwecker.android

import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager

/**
 * Device-protected storage is readable before the first unlock after a
 * reboot. Only the armed alarm times live there; settings, the TimeEdit link
 * and the cached schedule stay in credential-protected storage.
 */
fun deviceProtectedPrefs(context: Context, name: String): SharedPreferences =
    context.createDeviceProtectedStorageContext().getSharedPreferences(name, Context.MODE_PRIVATE)

/**
 * Credential-protected settings. Moves back data an earlier build kept in
 * device-protected storage. Must only be called once the user has unlocked.
 */
fun credentialPrefs(context: Context, name: String): SharedPreferences {
    context.moveSharedPreferencesFrom(context.createDeviceProtectedStorageContext(), name)
    return context.getSharedPreferences(name, Context.MODE_PRIVATE)
}

fun isUserUnlocked(context: Context): Boolean =
    context.getSystemService(UserManager::class.java).isUserUnlocked
