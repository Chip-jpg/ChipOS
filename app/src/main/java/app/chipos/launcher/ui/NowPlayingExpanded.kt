package app.chipos.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.chipos.launcher.data.NowPlaying

/**
 * The full-size player, opened by pressing the album cover on the home screen. Top to bottom: the
 * time, a large cover with the track details and progress beside it, then the playback controls
 * sitting just above the (shrunken) dock. Pressing the cover again, or Back, returns to the normal
 * home screen.
 */
@Composable
internal fun ExpandedPlayer(
    track: NowPlaying.Track,
    resumed: Boolean,
    playFocus: FocusRequester,
    onCollapse: () -> Unit,
    onOpenAllApps: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val controls = track.controller.transportControls

    Box(Modifier.fillMaxSize()) {
        TopButtons(onOpenAllApps, onOpenSettings, Modifier.align(Alignment.TopEnd))

        BoxWithConstraints(Modifier.fillMaxSize()) {
            // The cover takes whatever height is left after the clock and the controls.
            val artSize = (maxHeight - 80.dp - 76.dp - 24.dp).coerceIn(110.dp, 280.dp)

            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                ClockBlock(resumed, centered = true, timeSize = 48.sp)

                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                    ) {
                        AlbumArt(track = track, size = artSize, radius = 28.dp, onClick = onCollapse)
                        Column(Modifier.widthIn(max = 380.dp)) {
                            GlassText(track.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                            if (track.artist.isNotBlank()) {
                                GlassText(
                                    text = track.artist,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.85f),
                                )
                            }
                            if (track.durationMs > 0) {
                                Spacer(Modifier.height(18.dp))
                                TrackProgress(track, resumed, showTimes = true)
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconButton(onClick = { controls.skipToPrevious() }, size = 56.dp) { color ->
                        SkipGlyph(color, forward = false, glyphSize = 22.dp)
                    }
                    GlassIconButton(
                        onClick = { if (track.playing) controls.pause() else controls.play() },
                        modifier = Modifier.focusRequester(playFocus),
                        size = 72.dp,
                    ) { color ->
                        if (track.playing) PauseGlyph(color, glyphSize = 28.dp) else PlayGlyph(color, glyphSize = 28.dp)
                    }
                    GlassIconButton(onClick = { controls.skipToNext() }, size = 56.dp) { color ->
                        SkipGlyph(color, forward = true, glyphSize = 22.dp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
