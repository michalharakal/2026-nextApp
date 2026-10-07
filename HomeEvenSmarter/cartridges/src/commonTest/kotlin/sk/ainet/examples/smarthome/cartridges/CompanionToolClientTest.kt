package sk.ainet.examples.smarthome.cartridges

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompanionToolClientTest {
    private val jsonHeaders = headersOf("Content-Type", "application/json")

    @Test
    fun `calls the tool route and decodes the result`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/tools/get_weather", request.url.encodedPath)
            assertTrue("tomorrow" in request.body.toByteArray().decodeToString())
            respond("""{"tool":"get_weather","ok":true,"message":"Tomorrow: 9 °C, light rain"}""", HttpStatusCode.OK, jsonHeaders)
        }
        val result = CompanionToolClient(engine).call("http://10.0.0.2:8080/", "get_weather", mapOf("when" to "tomorrow"))
        assertTrue(result.ok)
        assertEquals("Tomorrow: 9 °C, light rain", result.message)
    }

    @Test
    fun `a 404 with a result body still decodes`() = runTest {
        val engine = MockEngine {
            respond("""{"tool":"order_pizza","ok":false,"message":"unknown tool 'order_pizza'"}""", HttpStatusCode.NotFound, jsonHeaders)
        }
        val result = CompanionToolClient(engine).call("http://10.0.0.2:8080", "order_pizza", emptyMap())
        assertFalse(result.ok)
        assertTrue("unknown tool" in result.message)
    }

    @Test
    fun `a dead companion throws for the caller to map`() = runTest {
        val engine = MockEngine { throw IllegalStateException("connection refused") }
        assertFailsWith<Exception> { CompanionToolClient(engine).call("http://10.0.0.2:8080", "get_weather", emptyMap()) }
    }

    @Test
    fun `lists the companion's tools`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/tools", request.url.encodedPath)
            respond("""{"tools":[{"name":"get_weather"}],"backends":[{"id":"weather","tools":["get_weather"]}]}""", HttpStatusCode.OK, jsonHeaders)
        }
        val index = CompanionToolClient(engine).tools("http://10.0.0.2:8080")
        assertEquals(listOf("get_weather"), index.tools.map { it.name })
    }
}
