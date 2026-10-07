package sk.ainet.examples.smarthome.tools

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import sk.ainet.examples.smarthome.remote.RemoteApi
import sk.ainet.examples.smarthome.remote.ToolCall
import sk.ainet.examples.smarthome.remote.ToolResult

/** `GET /tools` and `POST /tools/{name}` per the remote-tool contract. A missing or empty body is an empty call. */
fun Route.toolRoutes(tools: ToolRegistry) {
    get(RemoteApi.TOOLS) { call.respond(tools.index()) }
    post("${RemoteApi.TOOLS}/{name}") {
        val name = checkNotNull(call.parameters["name"])
        val body = runCatching { call.receiveNullable<ToolCall>() }.getOrNull() ?: ToolCall()
        when (val result = tools.execute(name, body.args)) {
            null -> call.respond(HttpStatusCode.NotFound, ToolResult(name, ok = false, message = "unknown tool '$name'"))
            else -> call.respond(result)
        }
    }
}
