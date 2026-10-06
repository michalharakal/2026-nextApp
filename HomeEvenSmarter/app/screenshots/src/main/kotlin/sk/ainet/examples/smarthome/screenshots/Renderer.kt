package sk.ainet.examples.smarthome.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat

/**
 * Headless Skia renderer: an off-screen scene at exactly (widthPx, heightPx) whose [Density] maps the pixels back
 * to a plausible logical device size. Needs no display, no GPU, no emulator.
 */
object Renderer {
    private const val FRAME_NANOS = 16_666_667L
    private const val MAX_FRAMES = 1200 // 20 s of virtual time; every animation in the app settles well below this

    @OptIn(ExperimentalComposeUiApi::class)
    fun render(spec: DeviceSpec, content: @Composable () -> Unit): ByteArray =
        ImageComposeScene(spec.widthPx, spec.heightPx, Density(spec.density), Dispatchers.Unconfined) { content() }.use { scene ->
            // Pump virtual frames until composition, layout and every animation are idle, then grab the frame.
            // Time is the scene's own clock, not the wall clock, so the settled pixels are reproducible.
            var nanos = 0L
            var image = scene.render(nanos)
            var frames = 0
            while (scene.hasInvalidations() && frames < MAX_FRAMES) {
                image.close()
                nanos += FRAME_NANOS
                image = scene.render(nanos)
                frames++
            }
            check(frames < MAX_FRAMES) { "${spec.id}: still animating after $MAX_FRAMES frames — a non-settling animation breaks reproducibility" }
            check(image.width == spec.widthPx && image.height == spec.heightPx) {
                "${spec.id}: rendered ${image.width}x${image.height}, spec says ${spec.widthPx}x${spec.heightPx}"
            }
            val png = image.encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("${spec.id}: PNG encoding failed")
            image.close()
            png
        }
}
