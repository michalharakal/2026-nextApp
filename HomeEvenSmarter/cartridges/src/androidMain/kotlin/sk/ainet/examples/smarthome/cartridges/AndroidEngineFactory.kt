package sk.ainet.examples.smarthome.cartridges

import android.content.Context
import kotlinx.io.files.Path
import sk.ainet.cartridge.asr.moonshine.MoonshineAsrCartridge
import sk.ainet.cartridge.asr.moonshine.StreamingAsrSession
import sk.ainet.cartridge.nlu.functiongemma.FunctionGemmaNluCartridge
import sk.ainet.cartridge.nlu.functiongemma.NluResolution
import sk.ainet.examples.smarthome.engine.AsrEngine
import sk.ainet.examples.smarthome.engine.AsrSession
import sk.ainet.examples.smarthome.engine.NluEngine
import sk.ainet.examples.smarthome.engine.NluOutcome
import sk.ainet.transformers.iree.android.IreeKvSession
import sk.ainet.transformers.iree.android.IreeMoonshineStream
import java.io.File
import sk.ainet.cartridge.asr.moonshine.PackDir as AsrPackDir
import sk.ainet.cartridge.nlu.functiongemma.PackDir as NluPackDir

/**
 * The Android engines: thin adapters from the blueprint modules' cartridge APIs to the app's engine contracts.
 * Both runtimes are the released SKaiNET-transformers JNI libraries the blueprint modules depend on
 * (`libskainet_moonshine_stream.so`, `libskainet_iree_kv.so`); the model files come from the pack_dir on disk.
 */
class AndroidEngineFactory(private val context: Context) : EngineFactory {
    override val platform: String = "android"
    override val available: Boolean = true

    override fun asr(cartridge: InstalledCartridge, language: String, device: DevicePreference): AsrEngine {
        val pack = AsrPackDir(Path(cartridge.dir.toString()))
        val dev = when (device) {
            DevicePreference.AUTO -> if (pack.targetsAccelerator) IreeMoonshineStream.VULKAN_DEVICE else IreeMoonshineStream.CPU_DEVICE
            DevicePreference.CPU -> IreeMoonshineStream.CPU_DEVICE
            DevicePreference.GPU -> IreeMoonshineStream.VULKAN_DEVICE
        }
        return MoonshineAsrEngine(MoonshineAsrCartridge(pack, language, dev))
    }

    override fun nlu(cartridge: InstalledCartridge, device: DevicePreference): NluEngine {
        val pack = NluPackDir(cartridge.dir.toString())
        val dev = when (device) {
            DevicePreference.AUTO -> null
            DevicePreference.CPU -> "local-task"
            DevicePreference.GPU -> IreeKvSession.VULKAN_DEVICE
        }
        val cacheDir = File(context.filesDir, "nlu-cache").apply { mkdirs() }
        return FunctionGemmaNluEngine(FunctionGemmaNluCartridge(pack, cacheDir, device = dev))
    }
}

class MoonshineAsrEngine(private val cartridge: MoonshineAsrCartridge) : AsrEngine {
    override val id: String get() = cartridge.id
    override val language: String get() = cartridge.language
    override fun open(): AsrSession = Session(cartridge.open())
    override fun close() = cartridge.close()

    private class Session(private val s: StreamingAsrSession) : AsrSession {
        override fun feed(pcm: FloatArray): String? = s.feed(pcm)
        override fun finish(): String = s.finish()
        override fun reset() = s.reset()
    }
}

class FunctionGemmaNluEngine(private val cartridge: FunctionGemmaNluCartridge) : NluEngine {
    override val id: String get() = cartridge.id
    override val toolNames: Set<String> get() = cartridge.toolNames
    val warmUpTiming: Map<String, Long> get() = cartridge.warmUpTiming
    val prefixTokens: Int get() = cartridge.prefixTokens

    override fun warmUp() = cartridge.warmUp()
    override fun close() = cartridge.close()

    override fun resolve(transcript: String, budgetMs: Long): NluOutcome = when (val r = cartridge.resolve(transcript, budgetMs)) {
        is NluResolution.Call -> NluOutcome.Call(r.name, r.args, r.raw, r.timing.toOutcome())
        is NluResolution.NoCall -> NluOutcome.NoCall(r.text, r.timing.toOutcome())
        is NluResolution.Failed -> NluOutcome.Failed(r.reason, r.cause, r.timing?.toOutcome())
    }

    private fun NluResolution.Timing.toOutcome() = NluOutcome.Timing(tokenizeMs, restoreMs, chunkMs, decodeMs, decodeTokens)
}
