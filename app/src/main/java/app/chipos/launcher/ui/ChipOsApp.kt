package app.chipos.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.chipos.launcher.data.AppEntry
import app.chipos.launcher.data.AppRepository
import app.chipos.launcher.data.BackState
import app.chipos.launcher.data.ChipPrefs
import app.chipos.launcher.data.DockStore
import app.chipos.launcher.data.HiddenApps
import app.chipos.launcher.data.RecentApps
import app.chipos.launcher.data.KeyState
import app.chipos.launcher.data.Weather
import android.os.SystemClock
import app.chipos.launcher.data.NowPlaying
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class Screen { Home, AllApps, Settings }

@Composable
fun ChipOsApp(
    homeSignal: Int,
    packageSignal: Int,
    resumed: Boolean,
) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.Home) }
    // The app whose options dialog is open (from a long press), if any.
    var optionsFor by remember { mutableStateOf<AppEntry?>(null) }

    // Keep trying to connect the music player until the notification listener has been enabled,
    // so it starts working right after scripts\enable-now-playing.bat runs, with no restart.
    LaunchedEffect(resumed) {
        while (resumed) {
            if (!NowPlaying.isRegistered) NowPlaying.start(context)
            delay(4000)
        }
    }

    // Weather: refresh now, then every 30 minutes while the launcher is on screen.
    val weatherCity = ChipPrefs.weatherName
    LaunchedEffect(resumed, weatherCity) {
        if (weatherCity.isBlank()) Weather.clear()
        while (resumed && weatherCity.isNotBlank()) {
            Weather.refresh()
            delay(30 * 60 * 1000L)
        }
    }

    // Ambient mode: after a few minutes without a button press, dim everything and show the clock.
    val ambientMinutes = ChipPrefs.ambientMinutes
    LaunchedEffect(resumed, ambientMinutes) {
        if (!resumed) KeyState.ambient = false
        while (resumed && ambientMinutes > 0) {
            val idle = SystemClock.elapsedRealtime() - KeyState.lastKeyTime
            if (!KeyState.ambient && idle >= ambientMinutes * 60_000L) KeyState.ambient = true
            delay(5000)
        }
    }

    // Pressing Home always returns to the main screen.
    LaunchedEffect(homeSignal) { screen = Screen.Home }

    val apps by produceState(initialValue = emptyList<AppEntry>(), packageSignal) {
        value = withContext(Dispatchers.IO) { AppRepository.loadAll(context) }
    }
    val dock = remember(apps, DockStore.saved) { DockStore.resolve(apps) }

    // All apps hides the apps you chose to hide; the home screen shows the latest opened apps.
    val hidden = HiddenApps.hidden
    val visibleApps = remember(apps, hidden) { apps.filter { it.packageName !in hidden } }
    val recentPackages = RecentApps.recent
    val recents = remember(apps, hidden, dock, recentPackages) {
        val inDock = dock.map { it.entry.packageName }.toSet()
        recentPackages
            .mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
            .filter { it.packageName !in inDock && it.packageName !in hidden && it.packageName != context.packageName }
            .take(5)
    }

    // Lets MainActivity handle Back for All apps / Settings directly (see BackState).
    SideEffect {
        BackState.goHome = {
            if (screen != Screen.Home && optionsFor == null && BackState.inner == 0) {
                screen = Screen.Home
                true
            } else {
                false
            }
        }
    }

    // On a launcher, Back must never leave the app: it only closes sub-screens.
    BackHandler {
        if (screen != Screen.Home) screen = Screen.Home
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        WallpaperLayer(resumed = resumed)
        // While the options dialog is open the screens underneath cannot take focus.
        val homeAlpha = animateFloatAsState(if (KeyState.ambient) 0f else 1f, tween(700), label = "homeFade")
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = homeAlpha.value }
                .focusProperties { canFocus = optionsFor == null && !KeyState.ambient },
        ) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    dock = dock,
                    recents = recents,
                    resumed = resumed,
                    onOpenAllApps = { screen = Screen.AllApps },
                    onOpenSettings = { screen = Screen.Settings },
                    onAppOptions = { optionsFor = it },
                )
                Screen.AllApps -> AllAppsScreen(
                    apps = visibleApps,
                    resumed = resumed,
                    onAppOptions = { optionsFor = it },
                )
                Screen.Settings -> SettingsScreen(apps = apps)
            }
        }
        AmbientScreen(resumed = resumed)
        optionsFor?.let { AppOptionsDialog(entry = it, onDismiss = { optionsFor = null }) }
    }
}
