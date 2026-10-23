package app.chipos.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import app.chipos.launcher.data.KeyState

/**
 * Shown after a few idle minutes: only the wallpaper (slightly dimmed) and one big clock.
 * The home screen fades out underneath (see ChipOsApp). Any button press ends it.
 */
@Composable
internal fun AmbientScreen(resumed: Boolean) {
    val show = KeyState.ambient
    val alpha = animateFloatAsState(if (show) 1f else 0f, tween(900), label = "ambient")
    if (alpha.value <= 0.01f && !show) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        ClockBlock(resumed = resumed, centered = true, timeSize = 150.sp, showDetails = false)
    }
}
