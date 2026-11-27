package at.zanko.kalenderwecker.desktop

import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

/**
 * File-backed settings. `java.util.prefs` (the JVM default) caps values at
 * 8 KB, far too small for a cached TimeEdit calendar. Writes are atomic so
 * the `--refresh`/`--ring` processes never read a half-written file.
 */
fun fileSettings(file: Path = defaultSettingsFile()): Settings {
    val props = Properties()
    if (file.exists()) file.inputStream().use(props::load)
    return PropertiesSettings(props) { updated ->
        file.parent?.createDirectories()
        val tmp = Files.createTempFile(file.parent, "settings", ".tmp")
        tmp.outputStream().use { updated.store(it, "KalenderWecker") }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}

fun defaultSettingsFile(): Path {
    val config = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
        ?: (System.getProperty("user.home") + "/.config")
    return Path(config, "kalenderwecker", "settings.properties")
}
