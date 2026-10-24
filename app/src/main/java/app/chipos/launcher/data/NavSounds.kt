package app.chipos.launcher.data

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

/**
 * The system's own keyboard-click sounds, triggered by remote keys: one for moving, one for
 * selecting, one for going back. They follow the TV's "System sounds / Sound effects" setting.
 */
object NavSounds {
    private var audio: AudioManager? = null
    private var lastMove = 0L

    fun init(context: Context) {
        audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    fun onKey(event: KeyEvent) {
        if (!ChipPrefs.navSounds || event.action != KeyEvent.ACTION_DOWN) return
        val am = audio ?: return
        // All three use the soft keyboard-click family, so moving sounds like selecting and going back.
        val effect = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> AudioManager.FX_KEYPRESS_SPACEBAR
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER ->
                if (event.repeatCount == 0) AudioManager.FX_KEYPRESS_STANDARD else return
            KeyEvent.KEYCODE_BACK ->
                if (event.repeatCount == 0) AudioManager.FX_KEYPRESS_RETURN else return
            else -> return
        }
        // Holding a direction key repeats quickly; keep the ticks from piling up.
        val now = SystemClock.elapsedRealtime()
        if (event.repeatCount > 0 && now - lastMove < 110) return
        lastMove = now
        try {
            am.playSoundEffect(effect)
        } catch (e: Exception) {
            // No sound is better than a crash.
        }
    }
}
