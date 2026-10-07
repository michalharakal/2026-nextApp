package sk.ainet.examples.smarthome.tools

import sk.ainet.examples.smarthome.remote.BackendDescriptor
import sk.ainet.examples.smarthome.remote.ToolBackend
import sk.ainet.examples.smarthome.remote.ToolIndex
import sk.ainet.examples.smarthome.remote.ToolResult

/**
 * Routes a remote tool call to whichever backend declares the tool. The registry also answers for the
 * *known* tools nobody declared: a home command without a control backend, or `get_weather` without a key,
 * is a soft failure the app shows as a rejected action — never a 404, never an exception.
 */
class ToolRegistry(private val backends: List<ToolBackend>) {
    private val byTool: Map<String, ToolBackend> = buildMap {
        for (b in backends) for (t in b.tools) putIfAbsent(t.name, b)
    }

    fun index(): ToolIndex = ToolIndex(
        tools = backends.flatMap { it.tools }.distinctBy { it.name },
        backends = backends.map { BackendDescriptor(it.id, it.tools.map { t -> t.name }) },
    )

    /** `null` means the name is no tool at all (a 404 for the routes). */
    suspend fun execute(name: String, args: Map<String, String>): ToolResult? {
        byTool[name]?.let { return it.execute(name, args) }
        return when {
            name == HomeTools.GET_WEATHER ->
                ToolResult(name, ok = false, message = "no weather backend on this companion — set ${WeatherBackend.ENV_KEY}")
            name in HomeTools.homeCommandNames ->
                ToolResult(name, ok = false, message = "no control backend")
            else -> null
        }
    }
}
