package sk.ainet.examples.smarthome.cartridges

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import sk.ainet.examples.smarthome.cartridge.CartridgeJson
import sk.ainet.examples.smarthome.remote.ControlState
import sk.ainet.examples.smarthome.remote.RemoteApi
import sk.ainet.examples.smarthome.remote.ToolCall
import sk.ainet.examples.smarthome.remote.ToolIndex
import sk.ainet.examples.smarthome.remote.ToolResult

/**
 * The app's side of the companion middleware: executes a remote tool over `POST /tools/{name}`. Timeouts are
 * short — on stage a dead companion must fail in a beat, not hang the pipeline. Transport errors throw; the
 * caller turns them into a failed action.
 */
class CompanionToolClient(engine: HttpClientEngine? = null) {
    private val client =
        if (engine != null) HttpClient(engine) { install(HttpTimeout) { requestTimeoutMillis = 5_000 } }
        else HttpClient(CIO) { install(HttpTimeout) { requestTimeoutMillis = 5_000; connectTimeoutMillis = 2_000 } }

    suspend fun tools(baseUrl: String): ToolIndex {
        val response = client.get(url(baseUrl, RemoteApi.TOOLS))
        if (!response.status.isSuccess()) error("companion answered ${response.status.value} for ${RemoteApi.TOOLS}")
        return CartridgeJson.json.decodeFromString(ToolIndex.serializer(), response.bodyAsText())
    }

    /** The server answers a [ToolResult] body even on 404, so whatever comes back parses into the same shape. */
    suspend fun call(baseUrl: String, tool: String, args: Map<String, String>): ToolResult {
        val response = client.post(url(baseUrl, RemoteApi.tool(tool))) {
            contentType(ContentType.Application.Json)
            setBody(CartridgeJson.json.encodeToString(ToolCall.serializer(), ToolCall(args)))
        }
        val text = response.bodyAsText()
        return runCatching { CartridgeJson.json.decodeFromString(ToolResult.serializer(), text) }
            .getOrElse { ToolResult(tool, ok = false, message = "companion answered ${response.status.value}") }
    }

    suspend fun controlState(baseUrl: String): ControlState {
        val response = client.get(url(baseUrl, RemoteApi.CONTROL_STATE))
        if (!response.status.isSuccess()) error("companion answered ${response.status.value} for ${RemoteApi.CONTROL_STATE}")
        return CartridgeJson.json.decodeFromString(ControlState.serializer(), response.bodyAsText())
    }

    private fun url(baseUrl: String, path: String): String = CartridgeDownloader.normalizeBase(baseUrl) + path
}
