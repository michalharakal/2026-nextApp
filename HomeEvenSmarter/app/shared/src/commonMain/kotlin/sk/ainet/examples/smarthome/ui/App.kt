package sk.ainet.examples.smarthome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.examples.smarthome.AppViewModel

enum class Destination(val label: String, val glyph: String) { HOME("Home", "⌂"), PIPELINE("Pipeline", "≋"), CARTRIDGES("Cartridges", "▣") }

/**
 * The app: on a wide screen (tablet, phone in landscape, desktop) the home and the pipeline sit side by side for the
 * projector; in portrait the rooms stack above a compact pipeline strip, and the full pipeline view is a tab. The
 * cartridge screen is always its own destination. No orientation is forced.
 */
@Composable
fun App(vm: AppViewModel, start: Destination = Destination.HOME) {
    HomeEvenSmarterTheme {
        var destination by remember { mutableStateOf(start) }
        BoxWithConstraints(Modifier.fillMaxSize().background(Palette.background).safeDrawingPadding()) {
            val wide = maxWidth > 840.dp
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(containerColor = Palette.surface) {
                        for (d in Destination.entries) {
                            if (d == Destination.PIPELINE) continue
                            NavigationRailItem(selected = destination == d, onClick = { destination = d },
                                icon = { Text(d.glyph) }, label = { Text(d.label) })
                        }
                    }
                    when (destination) {
                        Destination.CARTRIDGES -> CartridgesScreen(vm, Modifier.fillMaxSize().padding(16.dp))
                        else -> Row(Modifier.fillMaxSize()) {
                            HomeScreen(vm, Modifier.weight(0.58f).fillMaxSize().padding(16.dp))
                            PipelinePanel(vm, Modifier.weight(0.42f).fillMaxSize().padding(16.dp))
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (destination) {
                            // portrait phone: the rooms on top, a compact pipeline strip with the talk button below
                            Destination.HOME -> Column(Modifier.fillMaxSize()) {
                                HomeScreen(vm, Modifier.weight(1f).fillMaxWidth().padding(12.dp))
                                PipelineStrip(vm, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
                            }
                            Destination.PIPELINE -> PipelinePanel(vm, Modifier.fillMaxSize().padding(12.dp))
                            Destination.CARTRIDGES -> CartridgesScreen(vm, Modifier.fillMaxSize().padding(12.dp))
                        }
                    }
                    NavigationBar(containerColor = Palette.surface) {
                        for (d in Destination.entries) NavigationBarItem(selected = destination == d, onClick = { destination = d },
                            icon = { Text(d.glyph) }, label = { Text(d.label) })
                    }
                }
            }
        }
    }
}
