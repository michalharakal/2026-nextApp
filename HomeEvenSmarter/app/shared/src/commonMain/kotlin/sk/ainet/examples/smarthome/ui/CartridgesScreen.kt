package sk.ainet.examples.smarthome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.ainet.examples.smarthome.AppViewModel
import sk.ainet.examples.smarthome.cartridges.DevicePreference
import sk.ainet.examples.smarthome.cartridges.DownloadEvent
import sk.ainet.examples.smarthome.engine.EngineStatus

/** Where the models come from and whether they are loaded: server, downloads, installed packs, engines, golden set. */
@Composable
fun CartridgesScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val cartridges by vm.cartridges.collectAsState()
    val engines by vm.engines.collectAsState()
    val url by vm.serverUrl.collectAsState()
    val golden by vm.golden.collectAsState()
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cartridges", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)

        Card("Engines") {
            EngineRow("Speech (Moonshine v2 streaming, en)", engines.asr, engines.asrId)
            EngineRow("Understanding (FunctionGemma 270M, home catalog)", engines.nlu, engines.nluId)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Device", color = Palette.textDim)
                SingleChoiceSegmentedButtonRow {
                    DevicePreference.entries.forEachIndexed { i, p ->
                        SegmentedButton(selected = engines.device == p, onClick = { vm.setDevice(p) }, shape = SegmentedButtonDefaults.itemShape(i, DevicePreference.entries.size)) { Text(p.name.lowercase()) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = vm::loadAndWarmUp, enabled = vm.env.engineFactory.available && cartridges.installed.size >= 2) { Text("Load & warm up") }
                OutlinedButton(onClick = vm::runGoldenSet, enabled = engines.ready) { Text("Run golden set") }
                Spacer(Modifier.weight(1f))
                Text("fake engines", color = Palette.textDim, fontSize = 13.sp)
                Switch(engines.fakes, { vm.useFakes(it) })
            }
            if (!vm.env.engineFactory.available) Text("No on-device runtime on ${vm.env.platform} yet — fakes only.", color = Palette.warn, fontSize = 12.sp)
        }

        if (golden.isNotEmpty()) Card("Golden set  ${golden.count { it.passed }}/${golden.size}") {
            for (g in golden) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text((if (g.passed) "✓ " else "✗ ") + g.case.utterance, color = if (g.passed) Palette.ok else Palette.error, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("${g.run.calledTool ?: "no call"} · ${g.run.nluMs} ms", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Palette.textDim)
                }
            }
        }

        Card("Companion server") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(url, vm::setServerUrl, Modifier.weight(1f), singleLine = true, label = { Text("http://<laptop>:8080") })
                Spacer(Modifier.width(8.dp))
                Button(onClick = vm::fetchIndex) { Text("Fetch") }
            }
            cartridges.indexError?.let { Text(it, color = Palette.error, fontSize = 13.sp) }
            cartridges.index?.cartridges?.forEach { entry ->
                val installed = cartridges.installed.any { it.id == entry.id }
                val progress = cartridges.downloads[entry.id]
                Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.id, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            Text("${entry.task} · ${entry.abi}${entry.accelerator?.let { " · $it" } ?: ""} · ${entry.sizeBytes / 1_000_000} MB · ${entry.languages.joinToString()}", color = Palette.textDim, fontSize = 12.sp)
                        }
                        Button(onClick = { vm.download(entry) }, enabled = progress !is DownloadEvent.Progress && progress !is DownloadEvent.Verifying) {
                            Text(if (installed) "Re-download" else "Download")
                        }
                    }
                    when (progress) {
                        is DownloadEvent.Progress -> { LinearProgressIndicator({ progress.fraction }, Modifier.fillMaxWidth().padding(top = 4.dp)); Text("${progress.file} · ${progress.bytesDone / 1_000_000}/${progress.bytesTotal / 1_000_000} MB", fontSize = 11.sp, color = Palette.textDim) }
                        is DownloadEvent.Verifying -> Text("verifying ${progress.file}…", fontSize = 11.sp, color = Palette.warn)
                        is DownloadEvent.Done -> Text("downloaded and verified", fontSize = 11.sp, color = Palette.ok)
                        is DownloadEvent.Failed -> Text(progress.reason, fontSize = 11.sp, color = Palette.error)
                        null -> {}
                    }
                }
            }
        }

        Card("Installed  (${vm.env.store.root})") {
            if (cartridges.installed.isEmpty()) Text("none — download from the companion server, or push a pack_dir with adb", color = Palette.textDim, fontSize = 13.sp)
            for (c in cartridges.installed) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.id, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        Text("${c.task} · ${c.descriptor.family} ${c.descriptor.version} · ${c.descriptor.target.abi}${c.descriptor.target.accelerator?.let { " · $it" } ?: ""} · ${c.sizeBytes / 1_000_000} MB", color = Palette.textDim, fontSize = 12.sp)
                        cartridges.verification[c.id]?.let { Text(it, fontSize = 12.sp, color = if (it.startsWith("verified")) Palette.ok else Palette.warn) }
                    }
                    OutlinedButton(onClick = { vm.verify(c) }) { Text("Verify") }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = { vm.delete(c) }) { Text("Delete") }
                }
            }
            OutlinedButton(onClick = vm::refreshCartridges) { Text("Rescan") }
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.surface).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun EngineRow(name: String, status: EngineStatus, id: String?) {
    val (text, color) = when (status) {
        EngineStatus.Absent -> "not loaded" to Palette.textDim
        is EngineStatus.Missing -> status.reason to Palette.warn
        is EngineStatus.Loading -> "${status.phase} · ${status.elapsedMs / 1000}s" to Palette.warn
        is EngineStatus.Ready -> "ready${if (status.warmUpMs > 0) " in ${status.warmUpMs / 1000}s" else ""}${status.detail.takeIf { it.isNotEmpty() }?.let { " · $it" } ?: ""}" to Palette.ok
        is EngineStatus.Failed -> status.reason to Palette.error
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) { Text(name, fontSize = 14.sp); id?.let { Text(it, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Palette.textDim) } }
        Text(text, color = color, fontSize = 13.sp)
    }
    if (status is EngineStatus.Loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Palette.warn, trackColor = Color.Transparent)
}
