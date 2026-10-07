package sk.ainet.examples.smarthome

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import sk.ainet.examples.smarthome.cartridge.CartridgeJson
import sk.ainet.examples.smarthome.remote.ToolBackend
import sk.ainet.examples.smarthome.remote.ToolDescriptor
import sk.ainet.examples.smarthome.remote.ToolIndex
import sk.ainet.examples.smarthome.remote.ToolResult
import sk.ainet.examples.smarthome.tools.HomeTools
import sk.ainet.examples.smarthome.tools.ToolRegistry
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolRoutesTest {
    private val json = CartridgeJson.json

    private class EchoBackend : ToolBackend {
        override val id = "echo"
        override val tools = listOf(ToolDescriptor("get_weather", "echoes"))
        override suspend fun execute(tool: String, args: Map<String, String>) =
            ToolResult(tool, ok = true, message = "echo ${args["when"] ?: "?"}")
    }

    private fun withServer(registry: ToolRegistry, block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        application { module(CartridgeCatalog(File("build/does-not-exist")), registry) }
        block(client)
    }

    @Test
    fun `a home command without a control backend fails softly`() = withServer(ToolRegistry(emptyList())) { client ->
        val response = client.post("/tools/${HomeTools.SET_LIGHT}") {
            contentType(ContentType.Application.Json)
            setBody("""{"args":{"room":"kitchen","state":"on"}}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val result = json.decodeFromString(ToolResult.serializer(), response.bodyAsText())
        assertFalse(result.ok)
        assertEquals("no control backend", result.message)
    }

    @Test
    fun `weather without a key hints at the environment variable`() = withServer(ToolRegistry(emptyList())) { client ->
        val result = json.decodeFromString(ToolResult.serializer(), client.post("/tools/${HomeTools.GET_WEATHER}").bodyAsText())
        assertFalse(result.ok)
        assertTrue("OPENWEATHER_API_KEY" in result.message, result.message)
    }

    @Test
    fun `an unknown tool is 404`() = withServer(ToolRegistry(emptyList())) { client ->
        val response = client.post("/tools/order_pizza") { contentType(ContentType.Application.Json); setBody("""{"args":{}}""") }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `a declared tool is listed and its result passes through`() = withServer(ToolRegistry(listOf(EchoBackend()))) { client ->
        val index = json.decodeFromString(ToolIndex.serializer(), client.get("/tools").bodyAsText())
        assertEquals(listOf("get_weather"), index.tools.map { it.name })
        assertEquals(listOf("echo"), index.backends.map { it.id })

        val result = json.decodeFromString(ToolResult.serializer(), client.post("/tools/get_weather") {
            contentType(ContentType.Application.Json)
            setBody("""{"args":{"when":"tomorrow"}}""")
        }.bodyAsText())
        assertTrue(result.ok)
        assertEquals("echo tomorrow", result.message)
    }

    @Test
    fun `a missing body is an empty call`() = withServer(ToolRegistry(listOf(EchoBackend()))) { client ->
        val result = json.decodeFromString(ToolResult.serializer(), client.post("/tools/get_weather").bodyAsText())
        assertTrue(result.ok)
        assertEquals("echo ?", result.message)
    }

    @Test
    fun `the companion documents its own api`() = withServer(ToolRegistry(emptyList())) { client ->
        val response = client.get("/openapi.yaml")
        assertEquals(HttpStatusCode.OK, response.status)
        val spec = response.bodyAsText()
        assertTrue(spec.startsWith("openapi:"), spec.take(40))
        // the routes of this test suite stay documented
        assertTrue("/tools/{name}" in spec && "/cartridges/index.json" in spec)
    }
}
