package sk.ainet.examples.smarthome.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The companion middleware's remote-tool API: the app POSTs a tool call it does not execute locally
 * (today: `get_weather`; with a control backend: the home commands against the real house), the server
 * routes it to whichever [ToolBackend] declares the tool. The app never knows what sits behind a backend.
 */
object RemoteApi {
    const val TOOLS = "/tools"
    const val CONTROL_STATE = "/control/state"
    fun tool(name: String): String = "$TOOLS/$name"
}

/** Request body of `POST /tools/{name}`: the model's string arguments, verbatim. */
@Serializable
data class ToolCall(val args: Map<String, String> = emptyMap())

/** What a backend did with a call. [data] carries machine-readable extras next to the human [message]. */
@Serializable
data class ToolResult(
    val tool: String,
    val ok: Boolean,
    val message: String,
    val data: Map<String, String> = emptyMap(),
)

/** One callable tool as `GET /tools` lists it; [args] maps argument name to a short description. */
@Serializable
data class ToolDescriptor(
    val name: String,
    val description: String = "",
    val args: Map<String, String> = emptyMap(),
)

@Serializable
data class BackendDescriptor(val id: String, val tools: List<String>)

/** Response of `GET /tools`. */
@Serializable
data class ToolIndex(
    val tools: List<ToolDescriptor> = emptyList(),
    val backends: List<BackendDescriptor> = emptyList(),
)

/** One logical device as a control backend reports it, aligned with the device-model configuration shape. */
@Serializable
data class DeviceState(
    @SerialName("device_id") val deviceId: String,
    val room: String? = null,
    val kind: String,
    val state: Map<String, String> = emptyMap(),
)

/** Response of `GET /control/state`: the backend's last known snapshot of the real installation. */
@Serializable
data class ControlState(
    @SerialName("updated_at_ms") val updatedAtMs: Long = 0,
    val backend: String = "none",
    val devices: List<DeviceState> = emptyList(),
)

/**
 * A capability the companion middleware can execute: the weather sample in this repository, or a
 * building-bus control backend from a private sibling project. Implementations are plain Kotlin over
 * this module — no server types — so a backend never depends on the web layer.
 */
interface ToolBackend {
    val id: String
    val tools: List<ToolDescriptor>
    suspend fun execute(tool: String, args: Map<String, String>): ToolResult

    /** Live device states for the control-state endpoint; `null` when the backend has none to report. */
    fun deviceStates(): Flow<List<DeviceState>>? = null
}

/**
 * Discovery seam for optional backends: the companion server loads every provider on its classpath via
 * `java.util.ServiceLoader` and keeps the backends that [create] returns. A provider reads its own
 * configuration from [env] and returns `null` when it is not configured — absence is never an error.
 */
fun interface ToolBackendProvider {
    fun create(env: Map<String, String>): ToolBackend?
}
