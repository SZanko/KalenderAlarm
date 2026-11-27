package at.zanko.kalenderwecker.desktop

import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

private const val RATE = 44_100
private val PCM16 = AudioFormat(RATE.toFloat(), 16, 1, true, false)

/** Decoded audio ready to be written to a line. */
class LoadedSound(val format: AudioFormat, val pcm: ByteArray)

/** One second: 880 Hz for 0.2 s, 0.2 s silence, repeated; 16-bit little-endian mono. */
fun generateBeepPcm(sampleRate: Int = RATE): ByteArray {
    val out = ByteArray(sampleRate * 2)
    val period = (sampleRate * 0.2).toInt()
    for (i in 0 until sampleRate) {
        val on = (i / period) % 2 == 0
        val v = if (on) (sin(2 * PI * 880 * i / sampleRate) * 0.6 * Short.MAX_VALUE).toInt() else 0
        out[2 * i] = (v and 0xFF).toByte()
        out[2 * i + 1] = (v shr 8).toByte()
    }
    return out
}

/**
 * Decodes [path] (WAV/AIFF natively, MP3/OGG via the bundled SPIs) to signed
 * 16-bit PCM, capped at 5 minutes. Anything unreadable falls back to a beep.
 */
fun loadSound(path: String?): LoadedSound {
    val file = path?.let(::File)?.takeIf { it.isFile }
    if (file != null) {
        runCatching {
            AudioSystem.getAudioInputStream(file).use { source ->
                val base = source.format
                val target = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED, base.sampleRate, 16, base.channels,
                    base.channels * 2, base.sampleRate, false,
                )
                AudioSystem.getAudioInputStream(target, source).use { pcm ->
                    val max = (target.frameRate * target.frameSize * 300).toInt()
                    val bytes = pcm.readNBytes(max)
                    if (bytes.isNotEmpty()) return LoadedSound(target, bytes)
                }
            }
        }.onFailure { System.err.println("KalenderWecker: cannot decode $path: $it") }
    }
    return LoadedSound(PCM16, generateBeepPcm())
}

/** Loops a sound on the default output until [stop]. */
class DesktopAlarmPlayer {
    @Volatile private var playing = false
    private var worker: Thread? = null

    fun start(soundPath: String?) {
        stop()
        val sound = loadSound(soundPath)
        playing = true
        worker = thread(name = "alarm-audio", isDaemon = true) {
            runCatching {
                val line: SourceDataLine = AudioSystem.getSourceDataLine(sound.format)
                line.open(sound.format)
                line.start()
                while (playing) {
                    var offset = 0
                    while (playing && offset < sound.pcm.size) {
                        val n = minOf(8192, sound.pcm.size - offset)
                        offset += line.write(sound.pcm, offset, n)
                    }
                }
                line.stop()
                line.close()
            }.onFailure { System.err.println("KalenderWecker: audio output failed: $it") }
        }
    }

    fun stop() {
        playing = false
        worker?.join(1000)
        worker = null
    }
}
