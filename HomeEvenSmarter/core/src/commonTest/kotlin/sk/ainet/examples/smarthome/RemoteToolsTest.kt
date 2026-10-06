package sk.ainet.examples.smarthome

import sk.ainet.examples.smarthome.cartridge.CartridgeJson
import sk.ainet.examples.smarthome.remote.ControlState
import sk.ainet.examples.smarthome.remote.DeviceState
import sk.ainet.examples.smarthome.remote.ToolCall
import sk.ainet.examples.smarthome.remote.ToolResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemoteToolsTest {
    private val json = CartridgeJson.json

    @Test
    fun `tool call and result round-trip`() {
        val call = ToolCall(mapOf("when" to "tomorrow"))
        assertEquals(call, json.decodeFromString(ToolCall.serializer(), json.encodeToString(ToolCall.serializer(), call)))

        val result = ToolResult("get_weather", ok = true, message = "Tomorrow: 9 °C, light rain", data = mapOf("temp_c" to "9"))
        assertEquals(result, json.decodeFromString(ToolResult.serializer(), json.encodeToString(ToolResult.serializer(), result)))
    }

    @Test
    fun `control state uses snake_case on the wire`() {
        val state = ControlState(updatedAtMs = 42, backend = "none", devices = listOf(DeviceState("light.living_room", "living_room", "light", mapOf("on" to "true"))))
        val wire = json.encodeToString(ControlState.serializer(), state)
        assertTrue("updated_at_ms" in wire && "device_id" in wire, wire)
        assertEquals(state, json.decodeFromString(ControlState.serializer(), wire))
    }

    @Test
    fun `missing optional fields decode with defaults`() {
        val result = json.decodeFromString(ToolResult.serializer(), """{"tool":"set_light","ok":false,"message":"no control backend"}""")
        assertEquals(emptyMap(), result.data)
        val empty = json.decodeFromString(ToolCall.serializer(), """{}""")
        assertEquals(emptyMap(), empty.args)
    }
}
