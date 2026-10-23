package app.chipos.launcher.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.chipos.launcher.data.ChipPrefs
import app.chipos.launcher.data.NowPlaying
import kotlinx.coroutines.delay

/**
 * "Now playing" card for the home screen, shown beside the clock. It slides in while an app is playing music
 * (or has paused music), and slides away when playback ends. It reads other apps' media sessions,
 * so it needs the notification listener enabled by scripts\enable-now-playing.bat.
 */
@Composable
fun NowPlayingCard(resumed: Boolean, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val live = NowPlaying.track
    // Keep showing the last track while the card slides out, instead of going blank mid-animation.
    val last = remember { arrayOfNulls<NowPlaying.Track>(1) }
    if (live != null) last[0] = live
    val shown = last[0]

    AnimatedVisibility(
        visible = live != null && ChipPrefs.showNowPlaying,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 3 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 3 },
        modifier = modifier,
    ) {
        if (shown != null) PlayerCard(shown, resumed, onExpand)
    }
}

@Composable
private fun PlayerCard(track: NowPlaying.Track, resumed: Boolean, onExpand: () -> Unit) {
    val controls = track.controller.transportControls
    Row(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .liquidGlass(ContinuousCornerShape(28.dp), restTop = 0.22f, restBottom = 0.09f)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Pressing the cover opens the full-size player.
        AlbumArt(track = track, size = 76.dp, radius = 16.dp, onClick = onExpand)

        Column(Modifier.weight(1f)) {
            GlassText(track.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (track.artist.isNotBlank()) {
                GlassText(
                    text = track.artist,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            if (track.durationMs > 0) {
                Spacer(Modifier.height(8.dp))
                TrackProgress(track, resumed)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(onClick = { controls.skipToPrevious() }, size = 36.dp) { color ->
                    SkipGlyph(color, forward = false)
                }
                GlassIconButton(
                    onClick = { if (track.playing) controls.pause() else controls.play() },
                    size = 42.dp,
                ) { color -> if (track.playing) PauseGlyph(color) else PlayGlyph(color) }
                GlassIconButton(onClick = { controls.skipToNext() }, size = 36.dp) { color ->
                    SkipGlyph(color, forward = true)
                }
            }
        }
    }
}

/** A thin progress bar. Ticks once a second, only while the home screen is visible and playing. */
@Composable
internal fun TrackProgress(track: NowPlaying.Track, resumed: Boolean, showTimes: Boolean = false) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(track.playing, track.positionTime, resumed) {
        now = SystemClock.elapsedRealtime()
        while (track.playing && resumed) {
            delay(1000)
            now = SystemClock.elapsedRealtime()
        }
    }

    val rawPosition = if (track.playing) {
        track.positionMs + ((now - track.positionTime) * track.speed).toLong()
    } else {
        track.positionMs
    }
    val position = rawPosition.coerceIn(0L, track.durationMs)
    val fraction = position / track.durationMs.toFloat()

    val bar = Modifier
        .height(if (showTimes) 6.dp else 4.dp)
        .drawBehind {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(Color.White.copy(alpha = 0.25f), size = size, cornerRadius = radius)
            drawRoundRect(
                Color.White,
                size = Size(size.width * fraction, size.height),
                cornerRadius = radius,
            )
        }

    if (showTimes) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassText(formatTime(position), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Box(bar.weight(1f))
            GlassText(formatTime(track.durationMs), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Box(bar.fillMaxWidth())
    }
}

internal fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
internal fun PlayGlyph(color: Color, glyphSize: Dp = 17.dp) {
    Canvas(Modifier.size(glyphSize)) {
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.08f)
            lineTo(size.width * 0.92f, size.height * 0.5f)
            lineTo(size.width * 0.2f, size.height * 0.92f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
internal fun PauseGlyph(color: Color, glyphSize: Dp = 17.dp) {
    Canvas(Modifier.size(glyphSize)) {
        val bar = size.width * 0.28f
        val radius = CornerRadius(bar * 0.25f)
        drawRoundRect(color, Offset(size.width * 0.12f, 0f), Size(bar, size.height), radius)
        drawRoundRect(color, Offset(size.width * 0.60f, 0f), Size(bar, size.height), radius)
    }
}

@Composable
internal fun SkipGlyph(color: Color, forward: Boolean, glyphSize: Dp = 15.dp) {
    Canvas(Modifier.size(glyphSize)) {
        val w = size.width
        val h = size.height
        // A triangle pointing in the skip direction, plus a bar on the far side.
        val path = Path().apply {
            if (forward) {
                moveTo(0f, h * 0.1f)
                lineTo(w * 0.72f, h * 0.5f)
                lineTo(0f, h * 0.9f)
            } else {
                moveTo(w, h * 0.1f)
                lineTo(w * 0.28f, h * 0.5f)
                lineTo(w, h * 0.9f)
            }
            close()
        }
        drawPath(path, color)
        val barX = if (forward) w * 0.80f else 0f
        drawRoundRect(color, Offset(barX, h * 0.1f), Size(w * 0.2f, h * 0.8f), CornerRadius(w * 0.05f))
    }
}

/** A simple music note, shown when the app provides no cover art. */
@Composable
internal fun NoteGlyph(color: Color, glyphSize: Dp = 30.dp) {
    Canvas(Modifier.size(glyphSize)) {
        val w = size.width
        val h = size.height
        drawOval(color, Offset(w * 0.12f, h * 0.64f), Size(w * 0.36f, h * 0.26f))
        drawRect(color, Offset(w * 0.42f, h * 0.12f), Size(w * 0.07f, h * 0.64f))
        val flag = Path().apply {
            moveTo(w * 0.49f, h * 0.12f)
            lineTo(w * 0.86f, h * 0.30f)
            lineTo(w * 0.86f, h * 0.46f)
            lineTo(w * 0.49f, h * 0.30f)
            close()
        }
        drawPath(flag, color)
    }
}

/**
 * Album cover. It is a focusable button: pressing it switches between the compact player and the
 * full-size one. The rim lights up when it is focused with the remote.
 */
@Composable
internal fun AlbumArt(
    track: NowPlaying.Track,
    size: Dp,
    radius: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artShape = remember(radius) { ContinuousCornerShape(radius) }
    var focused by remember { mutableStateOf(false) }
    var interaction by remember { mutableStateOf(MutableInteractionSource()) }
    val focus = animateFloatAsState(if (focused) 1f else 0f, tween(180), label = "artFocus")

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val s = 1f + 0.06f * focus.value
                scaleX = s
                scaleY = s
                shadowElevation = 18.dp.toPx() * focus.value
                shape = artShape
                clip = true
            }
            .background(Color.White.copy(alpha = 0.16f))
            .onFocusChanged {
                if (focused && !it.isFocused) interaction = MutableInteractionSource()
                focused = it.isFocused
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val art = track.art
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = "Now playing",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            NoteGlyph(Color.White.copy(alpha = 0.85f), glyphSize = size * 0.4f)
        }
        Box(Modifier.fillMaxSize().glassSheen(artShape) { focus.value })
    }
}
