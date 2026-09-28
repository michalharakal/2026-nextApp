package sk.ainet.examples.smarthome

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import sk.ainet.examples.smarthome.fakes.Wav
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Uses the phone's text-to-speech engine to produce 16 kHz mono PCM of a sentence; `null` when no voice is available. */
object SpokenUtterance {
    fun synthesize(context: Context, text: String): FloatArray? {
        val ready = CountDownLatch(1)
        var status = TextToSpeech.ERROR
        val tts = TextToSpeech(context) { s -> status = s; ready.countDown() }
        if (!ready.await(10, TimeUnit.SECONDS) || status != TextToSpeech.SUCCESS) { tts.shutdown(); return null }
        if (tts.setLanguage(Locale.US) < 0) { tts.shutdown(); return null }
        val file = File(context.cacheDir, "tts.wav")
        val done = CountDownLatch(1)
        var ok = false
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { ok = true; done.countDown() }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) { done.countDown() }
        })
        tts.synthesizeToFile(text, null, file, "u1")
        done.await(30, TimeUnit.SECONDS); tts.shutdown()
        if (!ok || !file.isFile) return null
        val bytes = file.readBytes()
        val rate = sampleRate(bytes)
        val pcm = Wav.decode16BitMono(bytes)
        return if (rate == 16_000) pcm else resample(pcm, rate, 16_000)
    }

    private fun sampleRate(b: ByteArray): Int {
        var pos = 12
        while (pos + 8 <= b.size) {
            val id = b.decodeToString(pos, pos + 4)
            val size = (b[pos + 4].toInt() and 0xFF) or ((b[pos + 5].toInt() and 0xFF) shl 8) or ((b[pos + 6].toInt() and 0xFF) shl 16) or ((b[pos + 7].toInt() and 0xFF) shl 24)
            if (id == "fmt ") return (b[pos + 12].toInt() and 0xFF) or ((b[pos + 13].toInt() and 0xFF) shl 8) or ((b[pos + 14].toInt() and 0xFF) shl 16)
            pos += 8 + size + (size and 1)
        }
        return 16_000
    }

    private fun resample(pcm: FloatArray, from: Int, to: Int): FloatArray {
        val n = (pcm.size.toLong() * to / from).toInt()
        return FloatArray(n) { i ->
            val x = i.toDouble() * from / to
            val j = x.toInt(); val f = (x - j).toFloat()
            val a = pcm[minOf(j, pcm.size - 1)]; val b = pcm[minOf(j + 1, pcm.size - 1)]
            a + (b - a) * f
        }
    }
}
