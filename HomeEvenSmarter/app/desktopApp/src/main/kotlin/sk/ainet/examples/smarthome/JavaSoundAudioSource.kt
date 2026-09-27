package sk.ainet.examples.smarthome

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import sk.ainet.examples.smarthome.engine.AudioSource
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine

/** The laptop microphone through javax.sound: 16 kHz mono 16-bit, converted to float frames of 80 ms. */
class JavaSoundAudioSource(private val frameSamples: Int = 1280) : AudioSource {
    override val sampleRateHz: Int = 16_000
    private val format = AudioFormat(sampleRateHz.toFloat(), 16, 1, true, false)

    override fun frames(): Flow<FloatArray> = flow {
        val line = AudioSystem.getLine(javax.sound.sampled.DataLine.Info(TargetDataLine::class.java, format)) as TargetDataLine
        line.open(format, frameSamples * 2 * 8)
        line.start()
        try {
            val bytes = ByteArray(frameSamples * 2)
            while (currentCoroutineContext().isActive) {
                var filled = 0
                while (filled < bytes.size && currentCoroutineContext().isActive) {
                    val n = line.read(bytes, filled, bytes.size - filled)
                    if (n <= 0) break
                    filled += n
                }
                if (filled < bytes.size) break
                emit(FloatArray(frameSamples) { i -> ((bytes[2 * i].toInt() and 0xFF) or (bytes[2 * i + 1].toInt() shl 8)).toShort() / 32768f })
            }
        } finally {
            runCatching { line.stop() }; runCatching { line.close() }
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        fun ifAvailable(): AudioSource? = try {
            val format = AudioFormat(16_000f, 16, 1, true, false)
            if (AudioSystem.isLineSupported(javax.sound.sampled.DataLine.Info(TargetDataLine::class.java, format))) JavaSoundAudioSource() else null
        } catch (_: Throwable) { null }
    }
}
