package sk.ainet.examples.smarthome

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.io.files.Path
import sk.ainet.examples.smarthome.cartridges.AndroidEngineFactory
import sk.ainet.examples.smarthome.cartridges.CartridgeStore
import java.io.File

/**
 * Process-wide singletons: the view model (and with it the loaded engines, gigabytes of mmapped weights) must survive
 * activity recreation. Cartridges live in internal storage (`filesDir/cartridges/<id>`), so the IREE runtime mmaps
 * them from a plain, fast file system.
 */
class SmartHomeApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var viewModel: AppViewModel
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("homeevensmarter", Context.MODE_PRIVATE)
        val settings = object : AppEnvironment.Settings {
            override var serverUrl: String
                get() = prefs.getString("serverUrl", "http://192.168.1.10:8080")!!
                set(value) { prefs.edit().putString("serverUrl", value).apply() }
            override var devicePreference: String
                get() = prefs.getString("device", "AUTO")!!
                set(value) { prefs.edit().putString("device", value).apply() }
        }
        val env = AppEnvironment(
            platform = "android",
            store = CartridgeStore(Path(File(filesDir, "cartridges").apply { mkdirs() }.absolutePath)),
            engineFactory = AndroidEngineFactory(this),
            audioSource = AudioRecordSource(),
            settings = settings,
            startWithFakes = true, // real engines load on demand from the Cartridges screen (tens of seconds)
        )
        viewModel = AppViewModel(env, scope)
    }
}
