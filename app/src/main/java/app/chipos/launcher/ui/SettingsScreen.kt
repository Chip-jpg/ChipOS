package app.chipos.launcher.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import app.chipos.launcher.data.BackState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import app.chipos.launcher.data.DockStore
import app.chipos.launcher.data.HiddenApps
import app.chipos.launcher.data.MemoryCleaner
import app.chipos.launcher.data.WallpaperSource
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import app.chipos.launcher.data.AnimationSpeed
import app.chipos.launcher.data.AppEntry
import app.chipos.launcher.data.ChipPrefs
import app.chipos.launcher.data.NowPlaying
import app.chipos.launcher.data.SystemActions
import app.chipos.launcher.data.SystemShortcuts

private enum class SettingsPage { Main, Wifi, Bluetooth, Sound, Apps, About }

/**
 * ChipOS settings. The main page holds ChipOS's own options and links to the pages ChipOS provides
 * itself (Wi-Fi, Bluetooth, Sound, Apps, About). Things only the Fire OS Settings app can change
 * (display modes, updates, reset, accounts) open the matching Fire OS page.
 *
 * Back returns to the main page first, then home (handled in [ChipOsApp]).
 */
@Composable
fun SettingsScreen(apps: List<AppEntry>) {
    var page by remember { mutableStateOf(SettingsPage.Main) }

    BackHandler(enabled = page != SettingsPage.Main) { page = SettingsPage.Main }

    // While a sub-page is open, Back returns to the main page (not Home).
    val inSubPage = page != SettingsPage.Main
    DisposableEffect(inSubPage) {
        if (inSubPage) BackState.inner++
        onDispose { if (inSubPage) BackState.inner-- }
    }

    when (page) {
        SettingsPage.Main -> MainPage(apps = apps, onOpen = { page = it })
        SettingsPage.Wifi -> WifiPage()
        SettingsPage.Bluetooth -> BluetoothPage()
        SettingsPage.Sound -> SoundPage()
        SettingsPage.Apps -> AppsPage(apps)
        SettingsPage.About -> AboutPage()
    }
}

@Composable
private fun MainPage(apps: List<AppEntry>, onOpen: (SettingsPage) -> Unit) {
    val context = LocalContext.current
    val shortcuts = remember {
        SystemShortcuts.all.filter { SystemActions.isAvailable(context, it) }
    }
    var animationScale by remember { mutableFloatStateOf(AnimationSpeed.current(context)) }
    val canChangeSpeed = remember { AnimationSpeed.canWrite(context) }
    val nowPlayingStatus = NowPlaying.diagnostics
    val nowPlayingReady = remember(nowPlayingStatus) { NowPlaying.isListenerEnabled(context) }

    val scope = rememberCoroutineScope()
    var cityDialog by remember { mutableStateOf(false) }
    var hiddenDialog by remember { mutableStateOf(false) }
    var freeing by remember { mutableStateOf(false) }
    val myPhotos = remember(ChipPrefs.wallpaperMode) { WallpaperSource.userFiles(context).size }

    SettingsSheet(
        title = "Settings",
        overlayOpen = cityDialog || hiddenDialog,
        overlay = {
            if (cityDialog) {
                CityDialog(onDismiss = { cityDialog = false })
            } else {
                HiddenAppsDialog(apps = apps, onDismiss = { hiddenDialog = false })
            }
        },
    ) { first ->
        item { SectionTitle("Device") }
        item {
            GlassRow(
                title = "Wi-Fi",
                subtitle = "Join networks and see your connection",
                value = "›",
                onClick = { onOpen(SettingsPage.Wifi) },
                modifier = Modifier.focusRequester(first),
            )
        }
        item {
            GlassRow(
                title = "Bluetooth",
                subtitle = "Headphones, speakers, controllers, keyboards",
                value = "›",
                onClick = { onOpen(SettingsPage.Bluetooth) },
            )
        }
        item {
            GlassRow(
                title = "Sound",
                subtitle = "Volume and mute",
                value = "›",
                onClick = { onOpen(SettingsPage.Sound) },
            )
        }
        item {
            GlassRow(
                title = "Apps",
                subtitle = "Open, inspect and uninstall apps",
                value = "›",
                onClick = { onOpen(SettingsPage.Apps) },
            )
        }
        item {
            GlassRow(
                title = "About",
                subtitle = "Device, memory, storage, network",
                value = "›",
                onClick = { onOpen(SettingsPage.About) },
            )
        }

        item { SectionTitle("Speed") }
        item {
            GlassRow(
                title = "Animation speed",
                subtitle = if (canChangeSpeed) {
                    "Shorter animations make the whole device feel quicker"
                } else {
                    "One-time setup needed: run scripts\\grant-permissions.bat on your PC"
                },
                value = AnimationSpeed.label(animationScale),
                onClick = {
                    if (!canChangeSpeed) {
                        Toast.makeText(context, "Run scripts\\grant-permissions.bat first", Toast.LENGTH_LONG).show()
                    } else {
                        val next = AnimationSpeed.next(animationScale)
                        if (AnimationSpeed.set(context, next)) animationScale = next
                    }
                },
            )
        }

        item { SectionTitle("Home screen") }
        item {
            GlassRow(
                title = "Now playing card",
                subtitle = if (nowPlayingReady) {
                    "Shows music above the dock. $nowPlayingStatus"
                } else {
                    "One-time setup needed: run scripts\\enable-now-playing.bat on your PC"
                },
                toggle = ChipPrefs.showNowPlaying,
                onClick = { ChipPrefs.toggleNowPlaying() },
            )
        }
        item {
            GlassRow(
                title = "Wallpaper changes every",
                subtitle = "How often the background picture rotates",
                value = ChipPrefs.wallpaperLabel(ChipPrefs.wallpaperSeconds),
                onClick = { ChipPrefs.cycleWallpaperSeconds() },
            )
        }
        item {
            GlassRow(
                title = "24-hour clock",
                subtitle = "Off shows AM and PM",
                toggle = ChipPrefs.use24Hour,
                onClick = { ChipPrefs.toggle24Hour() },
            )
        }
        item {
            GlassRow(
                title = "Greeting",
                subtitle = "Show \"Good morning\" next to the date",
                toggle = ChipPrefs.showGreeting,
                onClick = { ChipPrefs.toggleGreeting() },
            )
        }
        item {
            GlassRow(
                title = "Always show dock names",
                subtitle = "Off shows a name only under the selected app",
                toggle = ChipPrefs.dockNamesAlways,
                onClick = { ChipPrefs.toggleDockNames() },
            )
        }

        item {
            GlassRow(
                title = "Dock",
                subtitle = "Hold Select on any app (here or in All apps) to add, remove or move it",
                value = "Reset",
                onClick = {
                    DockStore.reset()
                    Toast.makeText(context, "Dock reset to the default apps", Toast.LENGTH_SHORT).show()
                },
            )
        }
        item {
            GlassRow(
                title = "Recent apps row",
                subtitle = "Shows the last apps you opened above the dock",
                toggle = ChipPrefs.showRecents,
                onClick = { ChipPrefs.toggleRecents() },
            )
        }
        item {
            GlassRow(
                title = "Hidden apps",
                subtitle = "Apps hidden from All apps (hold Select on an app to hide it)",
                value = HiddenApps.hidden.size.toString(),
                onClick = { hiddenDialog = true },
            )
        }
        item {
            GlassRow(
                title = "Weather",
                subtitle = "Shown under the date. Uses open-meteo.com; needs internet",
                value = ChipPrefs.weatherName.ifBlank { "Off" },
                onClick = { cityDialog = true },
            )
        }
        item {
            GlassRow(
                title = "Ambient screen after",
                subtitle = "Dims the home screen and shows a big clock when nothing is pressed",
                value = ChipPrefs.ambientLabel(ChipPrefs.ambientMinutes),
                onClick = { ChipPrefs.cycleAmbient() },
            )
        }
        item {
            GlassRow(
                title = "Wallpapers",
                subtitle = if (myPhotos == 0) {
                    "My photos: none yet, run scripts\\push-wallpapers.bat on your PC"
                } else {
                    "$myPhotos of your own photos found"
                },
                value = ChipPrefs.wallpaperModeLabels[ChipPrefs.wallpaperMode],
                onClick = { ChipPrefs.cycleWallpaperMode() },
            )
        }

        item { SectionTitle("Performance") }
        item {
            GlassRow(
                title = if (freeing) "Freeing memory..." else "Free memory",
                subtitle = "Closes background apps. Anything playing music is left alone",
                onClick = {
                    if (freeing) return@GlassRow
                    freeing = true
                    scope.launch {
                        val result = MemoryCleaner.freeUp(context, apps)
                        freeing = false
                        val message = if (result.closed == 0) {
                            "Nothing to close"
                        } else {
                            "Closed ${result.closed} apps, freed about ${result.freedBytes / (1024 * 1024)} MB"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                },
            )
        }

        item { SectionTitle("TV") }
        items(shortcuts.size) { index ->
            val shortcut = shortcuts[index]
            GlassRow(
                title = shortcut.title,
                subtitle = shortcut.subtitle,
                value = "›",
                onClick = { SystemActions.open(context, shortcut) },
            )
        }
        item {
            GlassRow(
                title = "All TV settings",
                subtitle = "Open the full system Settings app",
                value = "›",
                onClick = { SystemActions.openSettings(context) },
            )
        }
    }
}
