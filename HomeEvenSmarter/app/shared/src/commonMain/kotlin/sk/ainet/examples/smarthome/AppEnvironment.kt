package sk.ainet.examples.smarthome

import sk.ainet.examples.smarthome.cartridges.CartridgeStore
import sk.ainet.examples.smarthome.cartridges.EngineFactory
import sk.ainet.examples.smarthome.engine.AudioSource

/** What a platform hands the shared UI: where cartridges live, how to build engines, where audio comes from. */
class AppEnvironment(
    val platform: String,
    val store: CartridgeStore,
    val engineFactory: EngineFactory,
    /** The microphone; `null` when the platform has none (typed commands still work). */
    val audioSource: AudioSource?,
    val settings: Settings,
    /** Start with the built-in fake engines (rehearsal, tests, platforms without a runtime). */
    val startWithFakes: Boolean = !engineFactory.available,
) {
    /** Two persisted strings are all the app needs. */
    interface Settings {
        var serverUrl: String
        var devicePreference: String
    }

    class MemorySettings(override var serverUrl: String = "http://192.168.1.10:8080", override var devicePreference: String = "AUTO") : Settings
}
