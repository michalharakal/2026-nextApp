package sk.ainet.examples.smarthome.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.ainet.examples.smarthome.AppViewModel
import sk.ainet.examples.smarthome.pipeline.PipelineState

/** Hold to talk. The ring follows the microphone level; the label says why it is disabled. */
@Composable
fun TalkButton(vm: AppViewModel) {
    val state by vm.pipelineState.collectAsState()
    val engines by vm.engines.collectAsState()
    val run by vm.run.collectAsState()
    val listening = state == PipelineState.LISTENING
    val busy = state != PipelineState.IDLE && !listening
    val enabled = engines.ready && vm.env.audioSource != null && !busy
    val level by animateFloatAsState((run.level * 8f).coerceIn(0f, 1f))
    val reason = when {
        vm.env.audioSource == null -> "no microphone — type a command"
        !engines.ready -> "engines not ready"
        busy -> "working…"
        listening -> "release to send"
        else -> "hold to talk"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(96.dp).pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = { vm.startListening(); try { awaitRelease() } finally { vm.stopListening() } })
            },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                drawCircle(if (!enabled) Palette.surfaceHigh else if (listening) Palette.accent else Palette.surfaceHigh, radius = r * 0.72f)
                drawCircle(if (listening) Palette.accent.copy(alpha = 0.5f) else Palette.outline, radius = r * (0.78f + 0.2f * level), style = Stroke(6f))
            }
            Text(if (listening) "●" else "🎙", fontSize = 28.sp, color = if (listening) Palette.background else Palette.text)
        }
        Text(reason, fontSize = 12.sp, color = Palette.textDim, fontWeight = FontWeight.Medium)
    }
}
