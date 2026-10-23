package app.chipos.launcher.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.chipos.launcher.data.ChipPrefs
import app.chipos.launcher.data.WallpaperSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Full-screen rotating wallpaper with a soft crossfade.
 * ContentScale.Crop keeps the picture undistorted on any aspect ratio, ultrawide included.
 */
@Composable
fun WallpaperLayer(resumed: Boolean) {
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    val mode = ChipPrefs.wallpaperMode
    val names = remember(mode) { WallpaperSource.list(context, mode).shuffled() }
    var index by remember(mode) { mutableIntStateOf(0) }

    // Only tick while the launcher is visible, so it costs nothing when another app is open.
    val seconds = ChipPrefs.wallpaperSeconds
    LaunchedEffect(resumed, names, seconds) {
        if (!resumed || names.size < 2 || seconds <= 0) return@LaunchedEffect
        while (true) {
            delay(seconds * 1000L)
            index = (index + 1) % names.size
        }
    }

    // produceState keeps the previous image until the next one has finished decoding.
    val bitmap by produceState<ImageBitmap?>(initialValue = null, index, names) {
        value = if (names.isEmpty()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                WallpaperSource.decode(
                    context,
                    names[index % names.size],
                    metrics.widthPixels,
                    metrics.heightPixels,
                )
            }
        }
    }

    Crossfade(
        targetState = bitmap,
        animationSpec = tween(durationMillis = 2000),
        label = "wallpaper",
    ) { image ->
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize())
        }
    }

    // Light scrim so white text stays readable on bright wallpapers.
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x40000000),
                        Color.Transparent,
                        Color(0x99000000),
                    ),
                ),
            ),
    )
}
