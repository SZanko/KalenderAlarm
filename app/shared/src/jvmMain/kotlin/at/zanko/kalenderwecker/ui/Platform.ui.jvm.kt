package at.zanko.kalenderwecker.ui

import io.github.vinceglb.filekit.PlatformFile

actual fun PlatformFile.persistableSoundUri(): String = file.absolutePath
