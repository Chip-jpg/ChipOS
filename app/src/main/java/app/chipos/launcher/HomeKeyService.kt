package app.chipos.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Brings ChipOS back whenever Amazon's home screen opens.
 *
 * Fire OS handles the Home button itself and always opens Amazon's launcher, which cannot be
 * disabled without root. This service is told whenever a new window appears. When that window is
 * Amazon's home screen, it opens ChipOS, so the Home button ends up at ChipOS. It only looks at
 * which app's window opened; it does not read the screen's content.
 *
 * Android blocks apps from opening screens for about 5 seconds after Home is pressed, so ChipOS
 * appears after that delay. There is no way around this without root.
 *
 * Turn it on with scripts\enable-home-button.bat, or in Fire OS Settings > Accessibility.
 */
class HomeKeyService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    /** The package of the window that opened last. */
    private var currentPackage = ""

    private fun openChipOs() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION,
            )
        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.w("ChipOS", "Could not open ChipOS: $e")
        }
    }

    /** If Amazon's screen is still showing a moment later, try again. */
    private val retry = Runnable {
        if (currentPackage == AMAZON_LAUNCHER) openChipOs()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString().orEmpty()

        val isHome = pkg == AMAZON_LAUNCHER && cls.contains("HomeActivity", ignoreCase = true)
        if (pkg == AMAZON_LAUNCHER && !isHome) return
        currentPackage = pkg
        if (!isHome) return

        openChipOs()
        handler.removeCallbacks(retry)
        handler.postDelayed(retry, 2500)
        handler.postDelayed(retry, 6000)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean = false

    override fun onInterrupt() {
        // Nothing to interrupt.
    }

    private companion object {
        const val AMAZON_LAUNCHER = "com.amazon.tv.launcher"
    }
}
