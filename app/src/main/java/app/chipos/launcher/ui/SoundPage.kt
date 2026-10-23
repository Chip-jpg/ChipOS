package app.chipos.launcher.ui

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Sound: media volume and mute. */
@Composable
internal fun SoundPage() {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val max = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var volume by remember { mutableIntStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var muted by remember { mutableStateOf(audio.isStreamMute(AudioManager.STREAM_MUSIC)) }

    fun setVolume(target: Int) {
        val clamped = target.coerceIn(0, max)
        try {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        } catch (e: SecurityException) {
            // Some devices refuse while Do Not Disturb is on; the bar then simply does not move.
        }
        volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        muted = audio.isStreamMute(AudioManager.STREAM_MUSIC)
    }

    SettingsSheet(title = "Sound") { first ->
        item {
            GlassRow(
                title = "Volume",
                subtitle = "Press left and right to change it",
                onClick = {},
                trailing = { ink -> VolumeBar(fraction = if (max == 0) 0f else volume / max.toFloat(), color = ink) },
                modifier = Modifier
                    .focusRequester(first)
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            false
                        } else {
                            when (event.key) {
                                Key.DirectionLeft -> { setVolume(volume - 1); true }
                                Key.DirectionRight -> { setVolume(volume + 1); true }
                                else -> false
                            }
                        }
                    },
            )
        }
        item {
            GlassRow(
                title = "Mute",
                subtitle = "Silence media audio",
                toggle = muted,
                onClick = {
                    try {
                        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0)
                    } catch (e: SecurityException) {
                        // Ignore; the switch reflects the real state below.
                    }
                    muted = audio.isStreamMute(AudioManager.STREAM_MUSIC)
                    volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                },
            )
        }
        item {
            GlassRow(
                title = "Good to know",
                subtitle = "If your TV or soundbar controls the volume itself, this bar may have no effect. Use your TV remote then.",
                onClick = {},
            )
        }
    }
}

@Composable
private fun VolumeBar(fraction: Float, color: Color) {
    Box(
        Modifier
            .size(width = 240.dp, height = 12.dp)
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(color.copy(alpha = 0.25f), size = size, cornerRadius = radius)
                drawRoundRect(
                    color,
                    size = Size(size.width * fraction.coerceIn(0f, 1f), size.height),
                    cornerRadius = radius,
                )
            },
    )
}
