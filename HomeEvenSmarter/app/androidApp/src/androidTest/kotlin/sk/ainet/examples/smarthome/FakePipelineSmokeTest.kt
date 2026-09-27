package sk.ainet.examples.smarthome

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import sk.ainet.examples.smarthome.home.Room
import sk.ainet.examples.smarthome.tools.HomeTools

/**
 * On-device smoke test on the fake engines: the process-wide view model, the pipeline and the home store work
 * together on a real phone (no cartridges needed). Real-cartridge runs are the golden set on the Cartridges screen.
 */
@RunWith(AndroidJUnit4::class)
class FakePipelineSmokeTest {
    @get:Rule
    val mic: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)

    @Test
    fun typedCommandReachesTheHome() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SmartHomeApp
        val vm = app.viewModel
        vm.useFakes(true)
        vm.resetHome()
        vm.runText("turn on the kitchen light")
        withTimeout(10_000) { while (!vm.homeState.value.room(Room.KITCHEN).light.on) kotlinx.coroutines.delay(50) }
        assertTrue(vm.homeState.value.room(Room.KITCHEN).light.on)
        assertEquals(HomeTools.SET_LIGHT, (vm.run.value.outcome as sk.ainet.examples.smarthome.engine.NluOutcome.Call).name)
    }

    @Test
    fun microphoneDeliversFrames() = runBlocking {
        val frames = withTimeout(5_000) { AudioRecordSource().frames().take(3).toList() }
        assertEquals(3, frames.size)
        assertEquals(1280, frames.first().size)
    }
}
