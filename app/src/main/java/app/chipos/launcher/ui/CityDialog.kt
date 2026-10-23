package app.chipos.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.chipos.launcher.data.ChipPrefs
import app.chipos.launcher.data.Weather
import kotlinx.coroutines.launch

/** Type a city once; the weather then shows next to the clock. */
@Composable
internal fun CityDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun search() {
        if (busy || text.isBlank()) return
        busy = true
        status = "Looking up \"${text.trim()}\"..."
        scope.launch {
            val place = Weather.findCity(text)
            busy = false
            if (place == null) {
                status = "No city found, or no internet connection"
            } else {
                ChipPrefs.setWeather(place.name, place.latitude, place.longitude)
                Weather.refresh()
                onDismiss()
            }
        }
    }

    GlassDialog(
        title = "Weather city",
        message = "Select the box and type a city (for example Milan), then choose Search.",
        onDismiss = onDismiss,
    ) { first ->
        val hasCity = ChipPrefs.weatherName.isNotBlank()
        if (hasCity) {
            GlassRow(
                title = "Turn weather off",
                subtitle = "Currently ${ChipPrefs.weatherName}",
                onClick = {
                    ChipPrefs.clearWeather()
                    Weather.clear()
                    onDismiss()
                },
                modifier = Modifier.focusRequester(first),
            )
        }
        GlassField(
            value = text,
            onValueChange = { text = it },
            hint = if (hasCity) "Change city" else "City name",
            onDone = { search() },
            modifier = if (hasCity) Modifier else Modifier.focusRequester(first),
            showKeyboardOnFocus = false,
        )
        val note = status
        if (note != null) {
            GlassText(
                text = note,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        GlassRow(title = "Search", onClick = { search() })
        GlassRow(title = "Cancel", onClick = onDismiss)
    }
}
