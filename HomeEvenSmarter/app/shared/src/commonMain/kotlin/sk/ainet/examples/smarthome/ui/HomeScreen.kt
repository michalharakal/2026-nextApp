package sk.ainet.examples.smarthome.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.ainet.examples.smarthome.AppViewModel
import sk.ainet.examples.smarthome.home.BlindPosition
import sk.ainet.examples.smarthome.home.DeviceRef
import sk.ainet.examples.smarthome.home.Door
import sk.ainet.examples.smarthome.home.HomeReducer
import sk.ainet.examples.smarthome.home.Room
import sk.ainet.examples.smarthome.home.RoomState

@Composable
fun HomeScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val home by vm.homeState.collectAsState()
    val changed by vm.recentlyChanged.collectAsState()
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Home", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(16.dp))
            home.activeScene?.let { SceneChip(it.label) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = vm::resetHome) { Text("Reset") }
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 200.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(Room.entries, key = { it.id }) { room -> RoomCard(home.room(room), changed) }
            items(Door.entries, key = { it.id }) { door -> DoorCard(door, home.locks[door] == true, DeviceRef.LockOf(door) in changed) }
        }
    }
}

@Composable
private fun SceneChip(label: String) {
    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Palette.accent2.copy(alpha = 0.2f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text("Scene: $label", color = Palette.accent2, fontSize = 13.sp)
    }
}

@Composable
private fun flashBorder(flash: Boolean): Color {
    val c by animateColorAsState(if (flash) Palette.accent else Palette.outline, tween(if (flash) 80 else 700))
    return c
}

@Composable
private fun RoomCard(state: RoomState, changed: Set<DeviceRef>) {
    val room = state.room
    val flash = changed.any { (it is DeviceRef.LightOf && it.room == room) || (it is DeviceRef.ThermostatOf && it.room == room) || (it is DeviceRef.BlindsOf && it.room == room) }
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).background(Palette.surface).border(2.dp, flashBorder(flash), RoundedCornerShape(16.dp)).padding(14.dp),
    ) {
        Text(room.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            LightTile(state.light.on, state.light.brightness, DeviceRef.LightOf(room) in changed)
            ThermostatTile(state.thermostat.targetCelsius, DeviceRef.ThermostatOf(room) in changed)
            BlindsTile(state.blinds, DeviceRef.BlindsOf(room) in changed)
        }
    }
}

@Composable
private fun LightTile(on: Boolean, brightness: Int, flash: Boolean) {
    val glow by animateFloatAsState(if (on) brightness / 100f else 0f, tween(500))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(56.dp)) {
            val r = size.minDimension / 2
            if (glow > 0f) drawCircle(Palette.lightOn.copy(alpha = 0.15f + 0.35f * glow), radius = r)
            drawCircle(if (glow > 0f) Palette.lightOn.copy(alpha = 0.4f + 0.6f * glow) else Palette.outline, radius = r * 0.45f, center = Offset(size.width / 2, size.height / 2))
            drawRect(if (glow > 0f) Palette.lightOn else Palette.outline, topLeft = Offset(size.width / 2 - r * 0.18f, size.height / 2 + r * 0.4f), size = Size(r * 0.36f, r * 0.3f))
        }
        Text(if (on) "$brightness%" else "off", color = if (on) Palette.text else Palette.textDim, fontSize = 13.sp)
    }
}

@Composable
private fun ThermostatTile(target: Double, flash: Boolean) {
    val t by animateFloatAsState(target.toFloat(), tween(600))
    val warm = ((t - 15f) / 15f).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(Palette.outline, 135f, 270f, false, style = Stroke(6f))
                drawArc(Color(1f, 0.6f - 0.4f * warm, 0.3f - 0.3f * warm), 135f, 270f * warm, false, style = Stroke(6f))
            }
            Text(HomeReducer.formatTemperature(t.toDouble()).removeSuffix(" °C"), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        Text("°C", color = Palette.textDim, fontSize = 13.sp)
    }
}

@Composable
private fun BlindsTile(position: BlindPosition, flash: Boolean) {
    val cover by animateFloatAsState(when (position) { BlindPosition.OPEN -> 0.1f; BlindPosition.HALF -> 0.5f; BlindPosition.CLOSED -> 1f }, tween(700))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(56.dp)) {
            val w = size.width * 0.7f; val h = size.height * 0.9f
            val left = (size.width - w) / 2; val top = (size.height - h) / 2
            drawRect(Palette.accent.copy(alpha = 0.25f), Offset(left, top), Size(w, h))
            drawRect(Palette.outline, Offset(left, top), Size(w, h), style = Stroke(3f))
            val slats = (h * cover / 6f).toInt().coerceAtLeast(if (cover > 0.05f) 1 else 0)
            for (i in 0 until slats) drawRect(Palette.textDim, Offset(left, top + i * 6f), Size(w, 4f))
        }
        Text(position.label.lowercase(), color = Palette.textDim, fontSize = 13.sp)
    }
}

@Composable
private fun DoorCard(door: Door, locked: Boolean, flash: Boolean) {
    val c by animateColorAsState(if (locked) Palette.ok else Palette.warn, tween(400))
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(Palette.surface).border(2.dp, flashBorder(flash), RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(40.dp)) {
            val w = size.width * 0.6f
            drawArc(c, 180f, 180f, false, topLeft = Offset((size.width - w * 0.7f) / 2 + if (locked) 0f else w * 0.15f, 0f), size = Size(w * 0.7f, size.height * 0.6f), style = Stroke(5f))
            drawRoundRect(c, Offset((size.width - w) / 2, size.height * 0.45f), Size(w, size.height * 0.5f), androidx.compose.ui.geometry.CornerRadius(6f))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(door.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(if (locked) "locked" else "unlocked", color = c, fontSize = 13.sp)
        }
    }
}
