package at.zanko.kalenderwecker.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopAlarmPlayerTest {
    private fun sample(pcm: ByteArray, i: Int): Int =
        (pcm[2 * i].toInt() and 0xFF) or (pcm[2 * i + 1].toInt() shl 8)

    @Test
    fun gIVEN_default_WHEN_generateBeep_THEN_oneSecond16BitMono() {
        assertEquals(88_200, generateBeepPcm().size)
    }

    @Test
    fun gIVEN_beep_WHEN_inspect_THEN_toneThenSilence() {
        val pcm = generateBeepPcm()
        val tone = (0 until 8_820).map { sample(pcm, it) }
        val silence = (8_820 until 17_640).map { sample(pcm, it) }

        assertTrue(tone.any { it != 0 })
        assertTrue(silence.all { it == 0 })
    }

    @Test
    fun gIVEN_missingFile_WHEN_loadPcm_THEN_fallsBackToBeep() {
        val clip = loadSound("/definitely/not/here.mp3")

        assertEquals(generateBeepPcm().size, clip.pcm.size)
    }
}
