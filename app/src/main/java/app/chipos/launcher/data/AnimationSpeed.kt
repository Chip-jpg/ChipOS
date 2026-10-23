package app.chipos.launcher.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * System-wide animation speed (the same three values as Developer options > Animation scale).
 * Shorter animations make every app feel quicker on a slow device.
 *
 * Android only lets an app change these after it has been granted WRITE_SECURE_SETTINGS, which
 * scripts\grant-permissions.bat does over ADB.
 */
object AnimationSpeed {
    /** Animation scales offered, from slowest to fastest. 0 turns animations off. */
    val options = listOf(1f, 0.5f, 0.25f, 0f)

    private val keys = listOf(
        Settings.Global.WINDOW_ANIMATION_SCALE,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        Settings.Global.ANIMATOR_DURATION_SCALE,
    )

    fun canWrite(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun current(context: Context): Float =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

    /** The next option after [scale], wrapping around. */
    fun next(scale: Float): Float {
        val index = options.indices.minByOrNull { kotlin.math.abs(options[it] - scale) } ?: 0
        return options[(index + 1) % options.size]
    }

    fun label(scale: Float): String = when {
        scale >= 0.99f -> "Normal"
        scale >= 0.49f -> "Fast"
        scale > 0f -> "Faster"
        else -> "Off"
    }

    /** Returns false if Android refused the change. */
    fun set(context: Context, scale: Float): Boolean = try {
        keys.forEach { Settings.Global.putFloat(context.contentResolver, it, scale) }
        true
    } catch (e: SecurityException) {
        false
    }
}
