package app.chipos.launcher.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.chipos.launcher.data.Strings
import app.chipos.launcher.data.AppEntry
import app.chipos.launcher.data.ChipPrefs
import app.chipos.launcher.data.DockItem
import app.chipos.launcher.data.NowPlaying
import app.chipos.launcher.data.SystemActions
import app.chipos.launcher.data.Weather
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Content is capped to this width so ultrawide screens get a centered layout, not a stretched one. */
internal val MaxContentWidth = 1500.dp

/** Padding between the dock shelf's edge and its tiles, on every side. */
internal val DockPadding = 26.dp

/** Extra space under the dock. Raise it to lift the dock, lower it to sit closer to the bottom edge. */
private val DockBottomGap = 8.dp

@Composable
fun HomeScreen(
    dock: List<DockItem>,
    recents: List<AppEntry>,
    resumed: Boolean,
    onOpenAllApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onAppOptions: (AppEntry) -> Unit,
) {
    val firstTile = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }

    // The full-size player: opened by pressing the album cover, closed by pressing it again or Back.
    val live = NowPlaying.track
    val lastTrack = remember { arrayOfNulls<NowPlaying.Track>(1) }
    if (live != null) lastTrack[0] = live
    var expanded by remember { mutableStateOf(false) }
    val showExpanded = expanded && live != null && ChipPrefs.showNowPlaying

    BackHandler(enabled = showExpanded) { expanded = false }

    // Put focus somewhere sensible whenever the layout changes: the play button in the full-size
    // player, otherwise the first dock tile, so the remote always works immediately.
    LaunchedEffect(dock.size, showExpanded) {
        delay(80)
        try {
            if (showExpanded) playFocus.requestFocus() else if (dock.isNotEmpty()) firstTile.requestFocus()
        } catch (e: Exception) {
            // Not attached yet; the first D-pad press will pick a target.
        }
    }

    // Overscan-safe margins (about 5% of a 1080p screen).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 27.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Crossfade(targetState = showExpanded, animationSpec = tween(280), label = "player") { isExpanded ->
                    val track = lastTrack[0]
                    if (isExpanded && track != null) {
                        ExpandedPlayer(
                            track = track,
                            resumed = resumed,
                            playFocus = playFocus,
                            onCollapse = { expanded = false },
                            onOpenAllApps = onOpenAllApps,
                            onOpenSettings = onOpenSettings,
                        )
                    } else {
                        TopBar(
                            resumed = resumed,
                            onOpenAllApps = onOpenAllApps,
                            onOpenSettings = onOpenSettings,
                            onExpandPlayer = { expanded = true },
                        )
                    }
                }
                if (!showExpanded && ChipPrefs.showRecents && recents.isNotEmpty()) {
                    RecentRow(
                        recents = recents,
                        onAppOptions = onAppOptions,
                        modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 28.dp),
                    )
                }
            }
            Dock(items = dock, firstTile = firstTile, onAppOptions = onAppOptions, compact = showExpanded)
            // Gap below the dock labels. Small on purpose: the screen's 27dp safe margin is already applied.
            Spacer(Modifier.height(DockBottomGap))
        }
    }
}

/**
 * The dock sits on a floating glass shelf, like a pane of frosted glass under the apps.
 * [compact] shrinks it (smaller tiles, no names) while the full-size player is open.
 */
@Composable
private fun Dock(
    items: List<DockItem>,
    firstTile: FocusRequester,
    onAppOptions: (AppEntry) -> Unit,
    compact: Boolean,
) {
    val context = LocalContext.current
    if (items.isEmpty()) return

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val count = items.size
        // Tiles shrink on narrow screens and stop growing on wide ones.
        val fullGap = 18.dp
        val available = maxWidth - 22.dp * 2
        val fullTile = ((available - fullGap * (count - 1)) / count).coerceIn(90.dp, 124.dp)

        // These animate, so the dock shrinks and grows smoothly.
        val tileWidth by animateDpAsState(if (compact) 100.dp else fullTile, tween(300), label = "dockTile")
        val gap by animateDpAsState(if (compact) 16.dp else fullGap, tween(300), label = "dockGap")
        val sidePadding by animateDpAsState(if (compact) 18.dp else 22.dp, tween(300), label = "dockSide")
        val topPadding by animateDpAsState(if (compact) 18.dp else 22.dp, tween(300), label = "dockTop")
        val bottomPadding by animateDpAsState(if (compact) 18.dp else 12.dp, tween(300), label = "dockBottom")

        Row(
            modifier = Modifier
                .liquidGlass(RoundedCornerShape(36.dp), restTop = 0.18f, restBottom = 0.06f)
                // Under each tile: the gap, the name, then the bottom padding, so the name sits
                // centered between the tile and the bottom edge of the shelf.
                .padding(start = sidePadding, end = sidePadding, top = topPadding, bottom = bottomPadding),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            items.forEachIndexed { index, item ->
                // key() ties each tile's state (focus, artwork) to its app, not to its position.
                key(item.entry.packageName) {
                    AppTile(
                        entry = item.entry,
                        label = item.label,
                        width = tileWidth,
                        labelOnlyWhenFocused = !ChipPrefs.dockNamesAlways,
                        onClick = { SystemActions.launch(context, item.entry) },
                        onLongPress = { onAppOptions(item.entry) },
                        showLabel = !compact,
                        modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                    )
                }
            }
        }
    }
}

/** The last few apps you opened that are not already in the dock. */
@Composable
private fun RecentRow(
    recents: List<AppEntry>,
    onAppOptions: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier.padding(start = 8.dp)) {
        GlassText(
            text = "RECENT",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Color.White.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            recents.forEach { entry ->
                key(entry.packageName) {
                    AppTile(
                        entry = entry,
                        label = entry.label,
                        width = 128.dp,
                        labelOnlyWhenFocused = true,
                        onClick = { SystemActions.launch(context, entry) },
                        onLongPress = { onAppOptions(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    resumed: Boolean,
    onOpenAllApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onExpandPlayer: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        ClockBlock(resumed)
        // The music player sits between the clock and the buttons, and only takes space when shown.
        Box(
            modifier = Modifier.weight(1f).padding(horizontal = 28.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            NowPlayingCard(resumed = resumed, onExpand = onExpandPlayer)
        }
        TopButtons(onOpenAllApps, onOpenSettings)
    }
}

/** The "All apps" and "Settings" glass buttons in the top right corner. */
@Composable
internal fun TopButtons(
    onOpenAllApps: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        GlassButton(
            label = "All apps",
            onClick = onOpenAllApps,
            modifier = Modifier.padding(top = 8.dp),
        ) { color -> AppsGlyph(color) }
        Spacer(Modifier.width(20.dp))
        GlassButton(
            label = "Settings",
            onClick = onOpenSettings,
            modifier = Modifier.padding(top = 8.dp),
        ) { color -> GearGlyph(color) }
    }
}

/** Big, bold clock with a greeting and the date underneath. */
@Composable
internal fun ClockBlock(
    resumed: Boolean,
    centered: Boolean = false,
    timeSize: TextUnit = 64.sp,
    showDetails: Boolean = true,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(resumed) {
        if (!resumed) return@LaunchedEffect
        while (true) {
            now = LocalDateTime.now()
            delay(15_000)
        }
    }

    val use24Hour = ChipPrefs.use24Hour
    val timeFormat = remember(use24Hour) {
        DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", Locale.ENGLISH)
    }
    val weather = Weather.now
    val italian = ChipPrefs.italian
    val dateFormat = remember(italian) { DateTimeFormatter.ofPattern("EEEE d MMMM", Strings.locale) }
    val time = timeFormat.format(now)
    val date = dateFormat.format(now)
    val greeting = when (now.hour) {
        in 5..11 -> Strings.t("Good morning")
        in 12..17 -> Strings.t("Good afternoon")
        else -> Strings.t("Good evening")
    }

    Column(horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        GlassText(
            text = time,
            fontSize = timeSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-2).sp,
        )
        if (showDetails) GlassText(
            text = if (ChipPrefs.showGreeting) "$greeting  \u00B7  $date" else date,
            fontSize = if (centered) 17.sp else 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.92f),
        )
        if (showDetails && weather != null) {
            GlassText(
                text = "${weather.temperatureC}\u00B0  \u00B7  ${Strings.t(weather.description)}  \u00B7  ${weather.city}",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.82f),
            )
        }
    }
}
