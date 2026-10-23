package app.chipos.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.chipos.launcher.data.AppEntry
import app.chipos.launcher.data.IconRepository
import app.chipos.launcher.data.TileArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Space between a tile and its name in a grid. */
private val GridLabelGap = 20.dp

/**
 * In the dock the name floats just below the glass shelf instead of inside it, so the shelf can keep
 * even padding on every side. This is the distance from the tile's bottom edge to the name:
 * the shelf's bottom padding (see [DockPadding]) plus a small gap under the shelf.
 */
internal val DockLabelGap = 26.dp + 12.dp

private val TileShape = RoundedCornerShape(18.dp)

/**
 * A 16:9 app tile with a glass rim and highlight. When focused with the remote's D-pad it lifts,
 * scales up and its rim lights up; when pressed it dips slightly.
 *
 * Focus and press animations are read inside layer and draw lambdas, so they redraw the tile
 * without recomposing it. That keeps a grid of tiles cheap on a slow device.
 *
 * @param width tile width, or [Dp.Unspecified] to fill the available width (used in grids).
 * @param labelOnlyWhenFocused show the name only under the focused tile (dock style).
 * @param showLabel false removes the name and the space for it (the compact dock).
 * @param floatingLabel draw the name without taking up layout space, so it can fall outside the
 *   parent's bounds (used by the dock to place names below the shelf).
 * @param modifier applied just before the focus handling, so a FocusRequester passed here works.
 */
@Composable
fun AppTile(
    entry: AppEntry,
    label: String,
    width: Dp,
    labelOnlyWhenFocused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    floatingLabel: Boolean = false,
    showLabel: Boolean = true,
) {
    val art = rememberTileArt(entry)
    // True from the moment a long press fires until the next key press, so the release that ends
    // the long press does not also count as a click.
    val longPressFired = remember { BooleanArray(1) }
    var focused by remember { mutableStateOf(false) }
    // Replaced when focus is lost, which clears a press whose key-up went to another element
    // (for example when a dialog opens during a long press).
    var interaction by remember { mutableStateOf(MutableInteractionSource()) }
    val pressed by interaction.collectIsPressedAsState()

    val focus = animateFloatAsState(if (focused) 1f else 0f, tween(180), label = "tileFocus")
    val press = animateFloatAsState(if (pressed && focused) 1f else 0f, tween(100), label = "tilePress")
    val labelAlpha = animateFloatAsState(
        if (!labelOnlyWhenFocused || focused) 1f else 0f,
        tween(160),
        label = "tileLabel",
    )

    val columnModifier = if (width == Dp.Unspecified) Modifier.fillMaxWidth() else Modifier.width(width)

    Column(
        modifier = columnModifier.zIndex(if (focused) 1f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .graphicsLayer {
                    val f = focus.value
                    val s = 1f + 0.14f * f - 0.08f * press.value
                    scaleX = s
                    scaleY = s
                    // The shadow only exists while the tile is lifted; resting tiles have none.
                    shadowElevation = 22.dp.toPx() * f
                    shape = TileShape
                    clip = true
                }
                .background(Color(0xFF1C1C1E))
                .then(modifier)
                .onFocusChanged {
                    if (focused && !it.isFocused) interaction = MutableInteractionSource()
                    focused = it.isFocused
                }
                .onPreviewKeyEvent { event ->
                    val longPress = onLongPress
                    if (longPress == null || event.type != KeyEventType.KeyDown) {
                        false
                    } else if (event.key == Key.Menu) {
                        // The Fire TV remote's menu button (three lines) opens the options directly.
                        longPress()
                        true
                    } else if (event.key == Key.DirectionCenter || event.key == Key.Enter) {
                        if (event.nativeKeyEvent.repeatCount == 0) {
                            longPressFired[0] = false
                            false
                        } else {
                            if (!longPressFired[0]) {
                                longPressFired[0] = true
                                longPress()
                            }
                            true
                        }
                    } else {
                        false
                    }
                }
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = {
                        if (longPressFired[0]) longPressFired[0] = false else onClick()
                    },
                ),
        ) {
            val tileArt = art
            if (tileArt != null && !tileArt.isBanner) {
                // Icon-only tiles get a dark gradient tinted from the icon's own colors.
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(tileArt.backdropTop, tileArt.backdropBottom)),
                    ),
                )
            }
            if (tileArt != null) {
                if (tileArt.isBanner) {
                    Image(
                        bitmap = tileArt.image,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Image(
                        bitmap = tileArt.image,
                        contentDescription = label,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight(0.62f)
                            .aspectRatio(1f),
                    )
                }
            }

            // Glass finish over the artwork.
            Box(Modifier.fillMaxSize().glassSheen(TileShape) { focus.value })
        }

        if (showLabel) {
            Spacer(Modifier.height(if (floatingLabel) DockLabelGap else GridLabelGap))

            GlassText(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (floatingLabel) {
                            // Zero layout height: the text is drawn, but the tile column stays tile-sized.
                            Modifier.layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, 0) { placeable.place(0, 0) }
                            }
                        } else {
                            Modifier
                        },
                    )
                    .graphicsLayer { alpha = labelAlpha.value },
            )
        }
    }
}

/**
 * Loads tile artwork off the main thread and caches it.
 *
 * The state is keyed by package name, so when the app list changes and a different app takes this
 * tile's position, the old artwork is dropped instead of being shown under the new app's name.
 */
@Composable
private fun rememberTileArt(entry: AppEntry): TileArt? {
    val context = LocalContext.current
    val packageName = entry.packageName
    var art by remember(packageName) { mutableStateOf(IconRepository.cached(packageName)) }

    LaunchedEffect(packageName) {
        if (art == null) {
            art = withContext(Dispatchers.Default) { IconRepository.load(context, entry) }
        }
    }
    return art
}
