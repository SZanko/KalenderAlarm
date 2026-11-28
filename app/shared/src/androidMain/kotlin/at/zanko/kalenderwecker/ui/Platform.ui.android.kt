package at.zanko.kalenderwecker.ui

import android.content.Context
import android.content.Intent
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile
import org.koin.mp.KoinPlatform

/** Keeps read access to the picked document across reboots, then returns its content:// URI. */
actual fun PlatformFile.persistableSoundUri(): String = when (val f = androidFile) {
    is AndroidFile.FileWrapper -> f.file.absolutePath
    is AndroidFile.UriWrapper -> {
        val context = KoinPlatform.getKoin().get<Context>()
        runCatching {
            context.contentResolver.takePersistableUriPermission(f.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        f.uri.toString()
    }
}
