package app.chipos.launcher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * ChipOS's own settings. Each value is Compose state, so the UI updates the moment it changes,
 * and each change is saved to SharedPreferences.
 */
object ChipPrefs {
    private const val FILE = "chipos_prefs"
    private const val KEY_WALLPAPER_SECONDS = "wallpaper_seconds"
    private const val KEY_24_HOUR = "clock_24_hour"
    private const val KEY_GREETING = "show_greeting"
    private const val KEY_DOCK_NAMES = "dock_names_always"
    private const val KEY_NOW_PLAYING = "show_now_playing"
    private const val KEY_AMBIENT = "ambient_minutes"
    private const val KEY_RECENTS = "show_recents"
    private const val KEY_WALLPAPER_MODE = "wallpaper_mode"
    private const val KEY_NAV_SOUNDS = "nav_sounds"
    private const val KEY_WEATHER_NAME = "weather_name"
    private const val KEY_WEATHER_LAT = "weather_lat"
    private const val KEY_WEATHER_LON = "weather_lon"

    /** Seconds between wallpapers; 0 turns rotation off. */
    val wallpaperOptions = listOf(0, 30, 90, 300, 900)

    /** Minutes of inactivity before the ambient screen starts; 0 turns it off. */
    val ambientOptions = listOf(0, 2, 5, 10, 30)

    /** Which wallpapers rotate: 0 = built-in, 1 = your own photos, 2 = both. */
    val wallpaperModeLabels = listOf("Built-in", "My photos", "Both")

    private var store: SharedPreferences? = null

    var wallpaperSeconds by mutableIntStateOf(90)
        private set
    var use24Hour by mutableStateOf(true)
        private set
    var showGreeting by mutableStateOf(true)
        private set
    var dockNamesAlways by mutableStateOf(false)
        private set
    var showNowPlaying by mutableStateOf(true)
        private set
    var showRecents by mutableStateOf(true)
        private set
    var ambientMinutes by mutableIntStateOf(5)
        private set
    var wallpaperMode by mutableIntStateOf(0)
        private set
    var navSounds by mutableStateOf(true)
        private set

    /** Saved weather city ("" = weather off). The name is state so the UI updates; coordinates are not. */
    var weatherName by mutableStateOf("")
        private set
    var weatherLatitude: Double = 0.0
        private set
    var weatherLongitude: Double = 0.0
        private set

    fun init(context: Context) {
        if (store != null) return
        val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        store = prefs
        wallpaperSeconds = prefs.getInt(KEY_WALLPAPER_SECONDS, 90)
        use24Hour = prefs.getBoolean(KEY_24_HOUR, true)
        showGreeting = prefs.getBoolean(KEY_GREETING, true)
        dockNamesAlways = prefs.getBoolean(KEY_DOCK_NAMES, false)
        showNowPlaying = prefs.getBoolean(KEY_NOW_PLAYING, true)
        showRecents = prefs.getBoolean(KEY_RECENTS, true)
        ambientMinutes = prefs.getInt(KEY_AMBIENT, 5)
        wallpaperMode = prefs.getInt(KEY_WALLPAPER_MODE, 0)
        navSounds = prefs.getBoolean(KEY_NAV_SOUNDS, true)
        weatherLatitude = prefs.getString(KEY_WEATHER_LAT, null)?.toDoubleOrNull() ?: 0.0
        weatherLongitude = prefs.getString(KEY_WEATHER_LON, null)?.toDoubleOrNull() ?: 0.0
        weatherName = prefs.getString(KEY_WEATHER_NAME, "") ?: ""
    }

    fun toggleRecents() {
        showRecents = !showRecents
        store?.edit()?.putBoolean(KEY_RECENTS, showRecents)?.apply()
    }

    fun cycleAmbient() {
        val index = ambientOptions.indexOf(ambientMinutes)
        ambientMinutes = ambientOptions[(index + 1) % ambientOptions.size]
        store?.edit()?.putInt(KEY_AMBIENT, ambientMinutes)?.apply()
    }

    fun cycleWallpaperMode() {
        wallpaperMode = (wallpaperMode + 1) % wallpaperModeLabels.size
        store?.edit()?.putInt(KEY_WALLPAPER_MODE, wallpaperMode)?.apply()
    }

    fun setWeather(name: String, latitude: Double, longitude: Double) {
        weatherLatitude = latitude
        weatherLongitude = longitude
        weatherName = name
        store?.edit()
            ?.putString(KEY_WEATHER_NAME, name)
            ?.putString(KEY_WEATHER_LAT, latitude.toString())
            ?.putString(KEY_WEATHER_LON, longitude.toString())
            ?.apply()
    }

    fun clearWeather() {
        weatherName = ""
        store?.edit()?.remove(KEY_WEATHER_NAME)?.apply()
    }

    fun ambientLabel(minutes: Int): String = if (minutes <= 0) "Off" else "$minutes min"

    fun cycleWallpaperSeconds() {
        val index = wallpaperOptions.indexOf(wallpaperSeconds)
        wallpaperSeconds = wallpaperOptions[(index + 1) % wallpaperOptions.size]
        store?.edit()?.putInt(KEY_WALLPAPER_SECONDS, wallpaperSeconds)?.apply()
    }

    fun toggle24Hour() {
        use24Hour = !use24Hour
        store?.edit()?.putBoolean(KEY_24_HOUR, use24Hour)?.apply()
    }

    fun toggleNavSounds() {
        navSounds = !navSounds
        store?.edit()?.putBoolean(KEY_NAV_SOUNDS, navSounds)?.apply()
    }

    fun toggleGreeting() {
        showGreeting = !showGreeting
        store?.edit()?.putBoolean(KEY_GREETING, showGreeting)?.apply()
    }

    fun toggleDockNames() {
        dockNamesAlways = !dockNamesAlways
        store?.edit()?.putBoolean(KEY_DOCK_NAMES, dockNamesAlways)?.apply()
    }

    fun toggleNowPlaying() {
        showNowPlaying = !showNowPlaying
        store?.edit()?.putBoolean(KEY_NOW_PLAYING, showNowPlaying)?.apply()
    }

    fun wallpaperLabel(seconds: Int): String = when {
        seconds <= 0 -> "Off"
        seconds < 60 -> "$seconds sec"
        else -> "${seconds / 60} min"
    }
}
