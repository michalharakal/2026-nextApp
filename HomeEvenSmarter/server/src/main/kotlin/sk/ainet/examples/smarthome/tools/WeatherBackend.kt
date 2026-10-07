package sk.ainet.examples.smarthome.tools

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import sk.ainet.examples.smarthome.cartridge.CartridgeJson
import sk.ainet.examples.smarthome.remote.ToolBackend
import sk.ainet.examples.smarthome.remote.ToolDescriptor
import sk.ainet.examples.smarthome.remote.ToolResult
import sk.ainet.examples.smarthome.tools.HomeTools.Args
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The in-repo sample backend: `get_weather` against the OpenWeather REST API. Configured entirely from the
 * environment — no key, no backend. The home location is fixed per companion ([ENV_LAT]/[ENV_LON]); a spoken
 * place name overrides it.
 */
class WeatherBackend(
    private val apiKey: String,
    private val homeLat: String?,
    private val homeLon: String?,
    engine: HttpClientEngine? = null,
) : ToolBackend {

    private val client = HttpClient(engine ?: CIO.create()) { install(HttpTimeout) { requestTimeoutMillis = 10_000 } }

    override val id: String = "weather"

    override val tools: List<ToolDescriptor> = listOf(
        ToolDescriptor(
            name = HomeTools.GET_WEATHER,
            description = "Report the weather or the forecast for the home location or a named place.",
            args = mapOf(Args.WHEN to "now|today|tomorrow (default now)", Args.PLACE to "city name, optional"),
        ),
    )

    override suspend fun execute(tool: String, args: Map<String, String>): ToolResult {
        val whenArg = args[Args.WHEN]?.lowercase()?.takeIf { it in setOf("now", "today", "tomorrow") } ?: "now"
        val place = args[Args.PLACE]?.takeIf { it.isNotBlank() }
        if (place == null && (homeLat == null || homeLon == null)) {
            return ToolResult(tool, ok = false, message = "no home location on this companion — set $ENV_LAT and $ENV_LON")
        }
        return try {
            val response = client.get(if (whenArg == "tomorrow") FORECAST_URL else CURRENT_URL) {
                parameter("appid", apiKey)
                parameter("units", "metric")
                if (place != null) parameter("q", place) else { parameter("lat", homeLat); parameter("lon", homeLon) }
            }
            if (!response.status.isSuccess()) {
                val hint = if (response.status.value == 401) " (check $ENV_KEY)" else ""
                return ToolResult(tool, ok = false, message = "weather service answered ${response.status.value}$hint")
            }
            val json = CartridgeJson.json.parseToJsonElement(response.bodyAsText()).jsonObject
            if (whenArg == "tomorrow") tomorrow(tool, json) else current(tool, whenArg, json)
        } catch (e: Exception) {
            ToolResult(tool, ok = false, message = "weather service unreachable: ${e.message ?: e::class.simpleName}")
        }
    }

    /** `/weather`: `{ name, main: { temp }, weather: [ { description } ] }` */
    private fun current(tool: String, whenArg: String, json: kotlinx.serialization.json.JsonObject): ToolResult {
        val name = json["name"]?.jsonPrimitive?.content.orEmpty()
        val temp = json.getValue("main").jsonObject.getValue("temp").jsonPrimitive.content.toDouble()
        val condition = json.getValue("weather").jsonArray.first().jsonObject.getValue("description").jsonPrimitive.content
        return result(tool, if (whenArg == "today") "Today" else "Now", name, temp, condition)
    }

    /** `/forecast`: 3-hour steps for 5 days; the entry closest to tomorrow 12:00 UTC is "tomorrow". */
    private fun tomorrow(tool: String, json: kotlinx.serialization.json.JsonObject): ToolResult {
        val target = LocalDate.now(ZoneOffset.UTC).plusDays(1).atTime(12, 0).toEpochSecond(ZoneOffset.UTC)
        val entry = json.getValue("list").jsonArray.map { it.jsonObject }
            .minByOrNull { abs(it.getValue("dt").jsonPrimitive.content.toLong() - target) }
            ?: return ToolResult(tool, ok = false, message = "weather service returned an empty forecast")
        val name = json["city"]?.jsonObject?.get("name")?.jsonPrimitive?.content.orEmpty()
        val temp = entry.getValue("main").jsonObject.getValue("temp").jsonPrimitive.content.toDouble()
        val condition = entry.getValue("weather").jsonArray.first().jsonObject.getValue("description").jsonPrimitive.content
        return result(tool, "Tomorrow", name, temp, condition)
    }

    private fun result(tool: String, lead: String, place: String, temp: Double, condition: String): ToolResult {
        val where = place.takeIf { it.isNotEmpty() }?.let { " in $it" } ?: ""
        return ToolResult(
            tool, ok = true,
            message = "$lead$where: ${temp.roundToInt()} °C, $condition",
            data = mapOf("temp_c" to temp.roundToInt().toString(), "condition" to condition, "place" to place),
        )
    }

    companion object {
        const val ENV_KEY = "OPENWEATHER_API_KEY"
        const val ENV_LAT = "HOME_LAT"
        const val ENV_LON = "HOME_LON"

        // pinned endpoints: an upstream API change is a one-line fix
        const val CURRENT_URL = "https://api.openweathermap.org/data/2.5/weather"
        const val FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast"

        /** `null` when no key is configured — the backend is then simply absent. */
        fun fromEnv(env: Map<String, String>, engine: HttpClientEngine? = null): WeatherBackend? =
            env[ENV_KEY]?.takeIf { it.isNotBlank() }?.let { WeatherBackend(it, env[ENV_LAT], env[ENV_LON], engine) }
    }
}
