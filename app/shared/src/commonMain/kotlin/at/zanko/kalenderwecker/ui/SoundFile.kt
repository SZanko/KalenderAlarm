package at.zanko.kalenderwecker.ui

import io.github.vinceglb.filekit.PlatformFile

/** Turns a picked file into a string the platform's alarm player can open later. */
expect fun PlatformFile.persistableSoundUri(): String
