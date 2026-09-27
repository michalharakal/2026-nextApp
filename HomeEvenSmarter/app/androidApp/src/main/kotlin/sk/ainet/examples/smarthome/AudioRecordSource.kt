package sk.ainet.examples.smarthome

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import sk.ainet.examples.smarthome.engine.AudioSource

/** The phone microphone as 16 kHz mono float frames of 80 ms, on the IO dispatcher until the collector cancels. */
class AudioRecordSource(private val frameSamples: Int = 1280) : AudioSource {
    override val sampleRateHz: Int = 16_000

    override fun frames(): Flow<FloatArray> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val bufferBytes = maxOf(minBuffer, frameSamples * 4 * 4)
        val recorder = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRateHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, bufferBytes)
        } catch (e: SecurityException) {
            throw IllegalStateException("microphone permission not granted", e)
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) { recorder.release(); throw IllegalStateException("AudioRecord failed to initialize") }
        try {
            recorder.startRecording()
            val frame = FloatArray(frameSamples)
            while (currentCoroutineContext().isActive) {
                var filled = 0
                while (filled < frameSamples && currentCoroutineContext().isActive) {
                    val n = recorder.read(frame, filled, frameSamples - filled, AudioRecord.READ_BLOCKING)
                    if (n < 0) throw IllegalStateException("AudioRecord.read returned $n")
                    filled += n
                }
                if (filled == frameSamples) emit(frame.copyOf())
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)
}
