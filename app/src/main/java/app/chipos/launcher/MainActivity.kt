package app.chipos.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import app.chipos.launcher.data.ChipPrefs
import app.chipos.launcher.data.BackState
import app.chipos.launcher.data.DockStore
import app.chipos.launcher.data.HiddenApps
import app.chipos.launcher.data.RecentApps
import app.chipos.launcher.data.KeyState
import app.chipos.launcher.data.NavSounds
import app.chipos.launcher.data.NowPlaying
import app.chipos.launcher.ui.ChipOsApp

class MainActivity : ComponentActivity() {

    /** Bumped every time the Home button is pressed while ChipOS is already running. */
    private val homeSignal = mutableIntStateOf(0)

    /** Bumped when an app is installed, removed or changed, so the app list refreshes. */
    private val packageSignal = mutableIntStateOf(0)

    /** True while the launcher is on screen; animations and timers pause otherwise. */
    private val resumed = mutableStateOf(false)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            packageSignal.intValue++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChipPrefs.init(this)
        DockStore.init(this)
        HiddenApps.init(this)
        RecentApps.init(this)
        NavSounds.init(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        setContent {
            ChipOsApp(
                homeSignal = homeSignal.intValue,
                packageSignal = packageSignal.intValue,
                resumed = resumed.value,
            )
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER) {
            KeyState.selectHeld = event.action == KeyEvent.ACTION_DOWN
        }
        // While the ambient screen is showing, the first press only wakes it (media and volume keys still work).
        val isMediaKey = when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE -> true
            else -> false
        }
        if (!isMediaKey) {
            if (KeyState.ambient) {
                KeyState.ambient = false
                KeyState.lastKeyTime = SystemClock.elapsedRealtime()
                return true
            }
            KeyState.lastKeyTime = SystemClock.elapsedRealtime()
            NavSounds.onKey(event)
        }

        // Back from All apps or Settings: act on the press itself so it always takes exactly one press.
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0 && BackState.goHome?.invoke() == true) {
                    swallowBackUp = true
                    return true
                }
            } else if (event.action == KeyEvent.ACTION_UP && swallowBackUp) {
                swallowBackUp = false
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private var swallowBackUp = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing Home while we are already the home screen returns to the main view.
        homeSignal.intValue++
    }

    override fun onResume() {
        super.onResume()
        resumed.value = true
        KeyState.ambient = false
        KeyState.lastKeyTime = SystemClock.elapsedRealtime()
        // Retries every time, so the player starts working right after the listener is enabled.
        NowPlaying.start(this)
    }

    override fun onPause() {
        resumed.value = false
        super.onPause()
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }
}
