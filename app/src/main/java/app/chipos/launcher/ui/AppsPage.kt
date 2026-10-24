package app.chipos.launcher.ui

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import app.chipos.launcher.data.AppEntry

private data class ManagedApp(val entry: AppEntry, val isSystem: Boolean)

/** Apps: open, inspect or uninstall installed apps. */
@Composable
internal fun AppsPage(apps: List<AppEntry>) {
    val context = LocalContext.current
    var showSystem by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<AppEntry?>(null) }

    val managed = remember(apps) {
        val pm = context.packageManager
        apps.map { entry ->
            val system = try {
                (pm.getApplicationInfo(entry.packageName, 0).flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) {
                false
            }
            ManagedApp(entry, system)
        }
    }
    val visible = remember(managed, showSystem) { managed.filter { showSystem || !it.isSystem } }

    SettingsSheet(
        title = "Apps",
        overlayOpen = selected != null,
        overlay = {
            selected?.let { AppOptionsDialog(entry = it, onDismiss = { selected = null }) }
        },
    ) { first ->
        item {
            GlassRow(
                title = "Show system apps",
                subtitle = "Apps that came with the TV. They can't be uninstalled.",
                toggle = showSystem,
                onClick = { showSystem = !showSystem },
                modifier = Modifier.focusRequester(first),
            )
        }
        item { SectionTitle("Installed apps") }
        items(visible, key = { it.entry.packageName }) { app ->
            GlassRow(
                title = app.entry.label,
                subtitle = app.entry.packageName,
                value = "Options",
                onClick = { selected = app.entry },
            )
        }
    }
}
