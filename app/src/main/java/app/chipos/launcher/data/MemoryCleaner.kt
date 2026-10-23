package app.chipos.launcher.data

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.delay

/**
 * Asks Android to close apps that are running in the background, to free memory on a small device.
 *
 * It only touches apps that are not on screen, never ChipOS itself, and never an app that currently
 * has a media session (so Spotify keeps playing). Closed apps restart normally when opened again.
 */
object MemoryCleaner {

    class Result(val closed: Int, val freedBytes: Long)

    /** Suspends for about a second so the memory figures can settle. */
    suspend fun freeUp(context: Context, apps: List<AppEntry>): Result {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val protectedPackages = NowPlaying.mediaPackages() + context.packageName

        val before = available(manager)
        var closed = 0
        for (app in apps) {
            if (app.packageName in protectedPackages) continue
            try {
                manager.killBackgroundProcesses(app.packageName)
                closed++
            } catch (e: SecurityException) {
                // The permission is missing; nothing can be closed.
                return Result(0, 0)
            }
        }
        delay(1000)
        val after = available(manager)
        return Result(closed, (after - before).coerceAtLeast(0L))
    }

    private fun available(manager: ActivityManager): Long {
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return info.availMem
    }
}
