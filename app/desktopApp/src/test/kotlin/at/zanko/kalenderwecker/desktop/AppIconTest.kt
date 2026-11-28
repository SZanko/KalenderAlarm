package at.zanko.kalenderwecker.desktop

import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class AppIconTest {
    @Test
    fun gIVEN_packagedResources_WHEN_loadAppIcon_THEN_512pxLogo() {
        assertEquals(Size(512f, 512f), appIcon.intrinsicSize)
    }
}
