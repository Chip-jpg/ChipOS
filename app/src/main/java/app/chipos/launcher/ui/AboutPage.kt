package app.chipos.launcher.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import app.chipos.launcher.data.SystemActions
import app.chipos.launcher.data.SystemShortcut
import app.chipos.launcher.data.NowPlaying
import app.chipos.launcher.data.WifiTools

/** About: device, memory, storage and network facts, plus how to restart and update. */
@Composable
internal fun AboutPage() {
    val context = LocalContext.current

    val facts = remember {
        val stat = StatFs(Environment.getDataDirectory().path)
        val memory = ActivityManager.MemoryInfo().also {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it)
        }
        val wm = WifiTools.manager(context)
        val version = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "?"
        }
        listOf(
            "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Build" to Build.DISPLAY,
            "Storage free" to "${Formatter.formatFileSize(context, stat.availableBytes)} of " +
                Formatter.formatFileSize(context, stat.totalBytes),
            "Memory free" to "${Formatter.formatFileSize(context, memory.availMem)} of " +
                Formatter.formatFileSize(context, memory.totalMem),
            "IP address" to (WifiTools.ipAddress(wm) ?: "Not connected"),
            "ChipOS" to "version $version",
        )
    }
    // These can change while the page is open, so they are read on every composition.
    val soundsOn = try {
        Settings.System.getInt(context.contentResolver, "sound_effects_enabled", 0) == 1
    } catch (e: Exception) {
        false
    }
    val extra = listOf(
        "Glass backdrop" to (GlassBackdrop.image?.let { "ready (${it.width}x${it.height})" } ?: "NOT built"),
        "System sounds" to if (soundsOn) "on" else "OFF (run android-tv-permissions.bat)",
        "Now playing" to if (NowPlaying.isRegistered) "connected" else "not connected",
    )
    val rows = facts + extra
    val updates = remember {
        SystemShortcut("Updates", "", Settings.ACTION_DEVICE_INFO_SETTINGS)
    }

    SettingsSheet(title = "About") { first ->
        rows.forEachIndexed { index, (label, value) ->
            item(key = label) {
                GlassRow(
                    title = label,
                    value = value,
                    onClick = {},
                    modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                )
            }
        }
        item { SectionTitle("Restart and update") }
        item {
            GlassRow(
                title = "Restart",
                subtitle = "Hold Select and Play/Pause on the remote for about 5 seconds.",
                onClick = {},
            )
        }
        item {
            GlassRow(
                title = "Software update and factory reset",
                subtitle = "These live in the TV settings.",
                value = "›",
                onClick = { SystemActions.open(context, updates) },
            )
        }
    }
}
