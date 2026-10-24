package app.chipos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.chipos.launcher.data.AppEntry
import app.chipos.launcher.data.SystemActions
import kotlinx.coroutines.delay

/**
 * Every installed app, on a large glass sheet floating over the dimmed wallpaper.
 * Back returns to the home screen (handled in [ChipOsApp]).
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun AllAppsScreen(apps: List<AppEntry>, resumed: Boolean, onAppOptions: (AppEntry) -> Unit) {
    val context = LocalContext.current
    val firstTile = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query) {
        val text = query.trim()
        if (text.isEmpty()) apps else apps.filter { it.label.contains(text, ignoreCase = true) }
    }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(apps.size) {
        if (apps.isNotEmpty()) {
            delay(80)
            try {
                firstTile.requestFocus()
            } catch (e: Exception) {
                // Not attached yet; the first D-pad press will pick a target.
            }
        }
    }

    // Coming back from an app, Android puts focus on the first thing on screen, which is the search
    // box, and opens the keyboard for it. Put focus back on the apps and close the keyboard.
    LaunchedEffect(resumed) {
        if (resumed && apps.isNotEmpty()) {
            delay(250)
            keyboard?.hide()
            try {
                firstTile.requestFocus()
            } catch (e: Exception) {
                // Not attached yet; the first D-pad press will pick a target.
            }
            delay(450)
            keyboard?.hide()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.30f))
            .padding(horizontal = 48.dp, vertical = 27.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxSize()
                .liquidGlass(SheetShape, restTop = 0.16f, restBottom = 0.05f)
                .clip(SheetShape)
                .padding(horizontal = 36.dp, vertical = 28.dp),
        ) {
            GlassText(
                text = "All apps",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            GlassText(
                text = "Hold Select on an app for options",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(12.dp))
            // Press Up from the first row of apps to reach the search box, then Select to type.
            GlassField(
                value = query,
                onValueChange = { query = it },
                hint = "Search apps",
                onDone = { keyboard?.hide() },
                showKeyboardOnFocus = false,
            )
            Spacer(Modifier.height(8.dp))
            if (shown.isEmpty()) {
                GlassText(
                    text = "No apps match \"$query\"",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                // Extra padding gives the focused (scaled-up) tile room to grow without clipping.
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            ) {
                itemsIndexed(
                    shown,
                    key = { _, app -> app.packageName },
                    contentType = { _, _ -> "tile" },
                ) { index, app ->
                    AppTile(
                        entry = app,
                        label = app.label,
                        width = Dp.Unspecified,
                        labelOnlyWhenFocused = false,
                        onClick = { SystemActions.launch(context, app) },
                        onLongPress = { onAppOptions(app) },
                        modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                    )
                }
            }
        }
    }
}
