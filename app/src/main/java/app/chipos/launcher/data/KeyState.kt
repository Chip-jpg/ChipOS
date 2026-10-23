package app.chipos.launcher.data

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Live remote state that Compose's own key handling does not expose. */
object KeyState {
    /** True while Select (or Enter) is physically held down. Updated by MainActivity. */
    var selectHeld by mutableStateOf(false)

    /** When a key was last pressed (SystemClock.elapsedRealtime). Used to detect idling. */
    var lastKeyTime: Long = SystemClock.elapsedRealtime()

    /** True while the ambient screen is showing. While true, MainActivity swallows the next key press. */
    var ambient by mutableStateOf(false)
}
