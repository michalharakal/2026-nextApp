package sk.ainet.examples.smarthome.ui

import androidx.compose.runtime.Composable
import sk.ainet.examples.smarthome.design.SkaiNet
import sk.ainet.examples.smarthome.design.SkaiNetTheme

/**
 * The app's palette, mapped onto the SKaiNET brand tokens from `:app:design`. The names stay semantic
 * (accent = speech/listening, accent2 = NLU, …) so screens never reference brand colors directly.
 */
object Palette {
    val background = SkaiNet.background
    val surface = SkaiNet.surface
    val surfaceHigh = SkaiNet.surfaceHigh
    val outline = SkaiNet.outline
    val text = SkaiNet.text
    val textDim = SkaiNet.textDim
    val accent = SkaiNet.red            // ASR / listening — the brand red
    val accent2 = SkaiNet.nlu           // NLU
    val ok = SkaiNet.ok                 // action done
    val warn = SkaiNet.warn             // warm-up, pending
    val error = SkaiNet.error
    val lightOn = SkaiNet.lightOn
    val cloud = SkaiNet.cloud
}

@Composable
fun HomeEvenSmarterTheme(content: @Composable () -> Unit) {
    SkaiNetTheme(content)
}
