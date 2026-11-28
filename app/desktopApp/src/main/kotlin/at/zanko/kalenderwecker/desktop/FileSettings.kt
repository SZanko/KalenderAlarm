package at.zanko.kalenderwecker.desktop

import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Properties
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

/**
 * File-backed settings shared by the GUI and the `--refresh`/`--ring`
 * processes. `java.util.prefs` (the JVM default) caps values at 8 KB, too
 * small for a cached TimeEdit calendar.
 *
 * Every operation re-reads the file under an exclusive lock and a write only
 * changes its own key, so concurrent processes never undo each other's
 * writes. Files are replaced atomically.
 */
fun fileSettings(file: Path = defaultSettingsFile()): Settings = LockedFileSettings(file)

private class LockedFileSettings(private val file: Path) : Settings {
    private val lockFile: Path = file.resolveSibling(file.fileName.toString() + ".lock")

    private fun <T> withProps(write: Boolean, block: (Settings) -> T): T {
        file.parent?.createDirectories()
        return FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            channel.lock().use {
                val props = Properties()
                if (file.exists()) file.inputStream().use(props::load)
                var dirty = false
                val result = block(PropertiesSettings(props) { dirty = true })
                if (write && dirty) save(props)
                result
            }
        }
    }

    private fun save(props: Properties) {
        val tmp = Files.createTempFile(file.parent, "settings", ".tmp")
        tmp.outputStream().use { props.store(it, "KalenderWecker") }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun <T> read(block: (Settings) -> T) = withProps(write = false, block)
    private fun write(block: (Settings) -> Unit) = withProps(write = true, block)

    override val keys: Set<String> get() = read { it.keys }
    override val size: Int get() = read { it.size }
    override fun clear() = write { it.clear() }
    override fun remove(key: String) = write { it.remove(key) }
    override fun hasKey(key: String): Boolean = read { it.hasKey(key) }

    override fun putInt(key: String, value: Int) = write { it.putInt(key, value) }
    override fun getInt(key: String, defaultValue: Int): Int = read { it.getInt(key, defaultValue) }
    override fun getIntOrNull(key: String): Int? = read { it.getIntOrNull(key) }

    override fun putLong(key: String, value: Long) = write { it.putLong(key, value) }
    override fun getLong(key: String, defaultValue: Long): Long = read { it.getLong(key, defaultValue) }
    override fun getLongOrNull(key: String): Long? = read { it.getLongOrNull(key) }

    override fun putString(key: String, value: String) = write { it.putString(key, value) }
    override fun getString(key: String, defaultValue: String): String = read { it.getString(key, defaultValue) }
    override fun getStringOrNull(key: String): String? = read { it.getStringOrNull(key) }

    override fun putFloat(key: String, value: Float) = write { it.putFloat(key, value) }
    override fun getFloat(key: String, defaultValue: Float): Float = read { it.getFloat(key, defaultValue) }
    override fun getFloatOrNull(key: String): Float? = read { it.getFloatOrNull(key) }

    override fun putDouble(key: String, value: Double) = write { it.putDouble(key, value) }
    override fun getDouble(key: String, defaultValue: Double): Double = read { it.getDouble(key, defaultValue) }
    override fun getDoubleOrNull(key: String): Double? = read { it.getDoubleOrNull(key) }

    override fun putBoolean(key: String, value: Boolean) = write { it.putBoolean(key, value) }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = read { it.getBoolean(key, defaultValue) }
    override fun getBooleanOrNull(key: String): Boolean? = read { it.getBooleanOrNull(key) }
}

fun defaultSettingsFile(): Path {
    val config = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
        ?: (System.getProperty("user.home") + "/.config")
    return Path(config, "kalenderwecker", "settings.properties")
}
