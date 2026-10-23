package app.chipos.launcher.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Current weather for one city, from Open-Meteo (https://open-meteo.com): free, no account or key.
 * The only data sent is the city name you type once, and then its coordinates.
 */
object Weather {

    class Place(val name: String, val latitude: Double, val longitude: Double)

    class Now(val temperatureC: Int, val description: String, val city: String)

    /** The latest reading, or null when no city is set or it could not be fetched. */
    var now by mutableStateOf<Now?>(null)
        private set

    /** Looks a city up by name. Returns null if nothing matches or there is no connection. */
    suspend fun findCity(query: String): Place? = withContext(Dispatchers.IO) {
        try {
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                URLEncoder.encode(query.trim(), "UTF-8") + "&count=1&language=en&format=json"
            val result = JSONObject(download(url)).optJSONArray("results")?.optJSONObject(0)
                ?: return@withContext null
            val country = result.optString("country_code")
            Place(
                name = result.getString("name") + if (country.isNotBlank()) ", $country" else "",
                latitude = result.getDouble("latitude"),
                longitude = result.getDouble("longitude"),
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Fetches the current weather for the saved city. Safe to call repeatedly. */
    suspend fun refresh() {
        if (ChipPrefs.weatherName.isBlank()) {
            now = null
            return
        }
        val latitude = ChipPrefs.weatherLatitude
        val longitude = ChipPrefs.weatherLongitude
        val city = ChipPrefs.weatherName
        val reading = withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,weather_code&timezone=auto"
                val current = JSONObject(download(url)).getJSONObject("current")
                Now(
                    temperatureC = Math.round(current.getDouble("temperature_2m")).toInt(),
                    description = describe(current.getInt("weather_code")),
                    city = city,
                )
            } catch (e: Exception) {
                null
            }
        }
        // Keep showing the last good reading if this attempt failed.
        if (reading != null) now = reading
    }

    fun clear() {
        now = null
    }

    private fun download(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** WMO weather interpretation codes, as used by Open-Meteo. */
    private fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        85, 86 -> "Snow showers"
        in 95..99 -> "Thunderstorm"
        else -> "Weather"
    }
}
