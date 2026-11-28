package at.zanko.kalenderwecker.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

/** The app logo (docs/logo/kalenderwecker.svg), used for window and taskbar icons. */
val appIcon: BitmapPainter by lazy {
    val bytes = checkNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream("kalenderwecker.png")) {
        "kalenderwecker.png missing from resources"
    }.use { it.readAllBytes() }
    BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
}
