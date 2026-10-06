package sk.ainet.examples.smarthome

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import sk.ainet.examples.smarthome.tools.WeatherBackend
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeatherBackendTest {

    private val jsonHeaders = headersOf("Content-Type", "application/json")

    private fun backend(engine: MockEngine, lat: String? = "52.52", lon: String? = "13.40") =
        WeatherBackend(apiKey = "k", homeLat = lat, homeLon = lon, engine = engine)

    @Test
    fun `current weather at the home location`() = runTest {
        val engine = MockEngine { request ->
            assertTrue(request.url.toString().startsWith(WeatherBackend.CURRENT_URL))
            assertEquals("k", request.url.parameters["appid"])
            assertEquals("52.52", request.url.parameters["lat"])
            respond("""{"name":"Berlin","main":{"temp":13.6},"weather":[{"description":"light rain"}]}""", HttpStatusCode.OK, jsonHeaders)
        }
        val result = backend(engine).execute("get_weather", mapOf("when" to "now"))
        assertTrue(result.ok)
        assertEquals("Now in Berlin: 14 °C, light rain", result.message)
        assertEquals("14", result.data["temp_c"])
    }

    @Test
    fun `a spoken place overrides the home location`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("Paris", request.url.parameters["q"])
            respond("""{"name":"Paris","main":{"temp":18.2},"weather":[{"description":"clear sky"}]}""", HttpStatusCode.OK, jsonHeaders)
        }
        val result = backend(engine, lat = null, lon = null).execute("get_weather", mapOf("place" to "Paris"))
        assertTrue(result.ok)
        assertTrue("Paris" in result.message)
    }

    @Test
    fun `tomorrow picks the forecast entry closest to midday`() = runTest {
        val tomorrowNoon = LocalDate.now(ZoneOffset.UTC).plusDays(1).atTime(12, 0).toEpochSecond(ZoneOffset.UTC)
        val engine = MockEngine { request ->
            assertTrue(request.url.toString().startsWith(WeatherBackend.FORECAST_URL))
            respond(
                """{"city":{"name":"Berlin"},"list":[
                     {"dt":${tomorrowNoon - 21_600},"main":{"temp":6.1},"weather":[{"description":"fog"}]},
                     {"dt":$tomorrowNoon,"main":{"temp":9.4},"weather":[{"description":"scattered clouds"}]},
                     {"dt":${tomorrowNoon + 10_800},"main":{"temp":8.0},"weather":[{"description":"rain"}]}]}""",
                HttpStatusCode.OK, jsonHeaders,
            )
        }
        val result = backend(engine).execute("get_weather", mapOf("when" to "tomorrow"))
        assertTrue(result.ok)
        assertEquals("Tomorrow in Berlin: 9 °C, scattered clouds", result.message)
    }

    @Test
    fun `a rejected key is a soft failure with a hint`() = runTest {
        val engine = MockEngine { respond("""{"cod":401}""", HttpStatusCode.Unauthorized, jsonHeaders) }
        val result = backend(engine).execute("get_weather", emptyMap())
        assertFalse(result.ok)
        assertTrue("401" in result.message && WeatherBackend.ENV_KEY in result.message, result.message)
    }

    @Test
    fun `no home location and no place is a soft failure`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.OK, jsonHeaders) }
        val result = backend(engine, lat = null, lon = null).execute("get_weather", emptyMap())
        assertFalse(result.ok)
        assertTrue(WeatherBackend.ENV_LAT in result.message, result.message)
    }

    @Test
    fun `fromEnv only builds a backend when the key is set`() {
        assertEquals(null, WeatherBackend.fromEnv(emptyMap()))
        assertEquals(null, WeatherBackend.fromEnv(mapOf(WeatherBackend.ENV_KEY to " ")))
        assertTrue(WeatherBackend.fromEnv(mapOf(WeatherBackend.ENV_KEY to "k")) != null)
    }
}
