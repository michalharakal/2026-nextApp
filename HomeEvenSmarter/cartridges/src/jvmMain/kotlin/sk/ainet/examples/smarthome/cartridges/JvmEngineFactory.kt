package sk.ainet.examples.smarthome.cartridges

/** Desktop is the second stage: the IREE runtime bindings of the blueprint modules exist for Android only today. */
object JvmEngineFactory : EngineFactory by UnsupportedEngineFactory(
    platform = "jvm",
    reason = "no desktop runtime binding yet — run the desktop app with the built-in fake engines",
)
