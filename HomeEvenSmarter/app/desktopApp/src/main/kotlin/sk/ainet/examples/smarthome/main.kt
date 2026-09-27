package sk.ainet.examples.smarthome

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.io.files.Path
import sk.ainet.examples.smarthome.cartridges.CartridgeStore
import sk.ainet.examples.smarthome.cartridges.JvmEngineFactory
import sk.ainet.examples.smarthome.ui.App
import java.io.File
import java.util.Properties

/**
 * Desktop entry point. Stage 1: the same UI on the built-in fake engines with the laptop microphone — the rehearsal
 * build and the fallback if the phone dies on stage. Real engines arrive with the desktop runtime binding.
 */
fun main() {
    val home = File(System.getProperty("user.home"), ".homeevensmarter").apply { mkdirs() }
    val settings = FileSettings(File(home, "settings.properties"))
    val env = AppEnvironment(
        platform = "jvm",
        store = CartridgeStore(Path(File(home, "cartridges").apply { mkdirs() }.absolutePath)),
        engineFactory = JvmEngineFactory,
        audioSource = JavaSoundAudioSource.ifAvailable(),
        settings = settings,
        startWithFakes = true,
    )
    val vm = AppViewModel(env, CoroutineScope(SupervisorJob() + Dispatchers.Main))
    application {
        Window(onCloseRequest = ::exitApplication, title = "HomeEvenSmarter", state = WindowState(size = DpSize(1400.dp, 860.dp))) {
            App(vm)
        }
    }
}

private class FileSettings(private val file: File) : AppEnvironment.Settings {
    private val props = Properties().apply { if (file.isFile) file.inputStream().use { load(it) } }
    private fun save() = file.outputStream().use { props.store(it, null) }
    override var serverUrl: String
        get() = props.getProperty("serverUrl", "http://localhost:8080")
        set(value) { props.setProperty("serverUrl", value); save() }
    override var devicePreference: String
        get() = props.getProperty("device", "AUTO")
        set(value) { props.setProperty("device", value); save() }
}
