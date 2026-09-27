package sk.ainet.examples.smarthome.cartridges

import sk.ainet.examples.smarthome.engine.AsrEngine
import sk.ainet.examples.smarthome.engine.NluEngine

/** Which IREE device a cartridge should run on. `null` = what it was materialized for. */
enum class DevicePreference { AUTO, CPU, GPU }

/**
 * Creates the real engines from installed cartridges. Implemented per platform: on Android over the blueprint
 * modules' runtime bindings, on the JVM not yet (desktop is the next stage).
 */
interface EngineFactory {
    val platform: String
    val available: Boolean
    fun asr(cartridge: InstalledCartridge, language: String = "en", device: DevicePreference = DevicePreference.AUTO): AsrEngine
    fun nlu(cartridge: InstalledCartridge, device: DevicePreference = DevicePreference.AUTO): NluEngine
}

class UnsupportedEngineFactory(override val platform: String, private val reason: String) : EngineFactory {
    override val available: Boolean = false
    override fun asr(cartridge: InstalledCartridge, language: String, device: DevicePreference): AsrEngine = throw UnsupportedOperationException(reason)
    override fun nlu(cartridge: InstalledCartridge, device: DevicePreference): NluEngine = throw UnsupportedOperationException(reason)
}
