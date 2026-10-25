package app.chipos.launcher.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import app.chipos.launcher.data.Strings
import app.chipos.launcher.data.AppEntry
import app.chipos.launcher.data.DockStore
import app.chipos.launcher.data.HiddenApps
import app.chipos.launcher.data.RecentApps
import app.chipos.launcher.data.SystemActions

/**
 * Options for one app: open, app info and (for apps you installed) uninstall.
 * Opened by holding Select on an app, or by pressing the remote's menu button.
 */
@Composable
internal fun AppOptionsDialog(entry: AppEntry, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isSystem = remember(entry.packageName) {
        try {
            val flags = context.packageManager.getApplicationInfo(entry.packageName, 0).flags
            (flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }

    GlassDialog(
        title = entry.label,
        onDismiss = onDismiss,
    ) { first ->
        OptionRow(
            title = "Open",
            onClick = {
                onDismiss()
                SystemActions.launch(context, entry)
            },
            modifier = Modifier.focusRequester(first),
        )
        if (entry.packageName in RecentApps.recent && DockStore.position(entry.packageName) < 0) {
            OptionRow(
                title = "Remove from Recent",
                subtitle = "It comes back the next time you open it",
                onClick = {
                    RecentApps.remove(entry.packageName)
                    onDismiss()
                },
            )
        }
        OptionRow(
            title = "App info",
            subtitle = "Storage, permissions, force stop",
            onClick = {
                onDismiss()
                try {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${entry.packageName}"),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, Strings.t("App info isn't available on this device"), Toast.LENGTH_SHORT).show()
                }
            },
        )
        if (!isSystem) {
            OptionRow(
                title = "Uninstall",
                onClick = {
                    onDismiss()
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_DELETE, Uri.parse("package:${entry.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, Strings.t("Couldn't start the uninstaller"), Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
        OptionRow(
            title = "Hide from All apps",
            subtitle = "Undo in Settings > Hidden apps",
            onClick = {
                HiddenApps.hide(entry.packageName)
                onDismiss()
            },
        )
        val dockPosition = DockStore.position(entry.packageName)
        if (dockPosition >= 0) {
            if (dockPosition > 0) {
                OptionRow(title = "Move left in dock", onClick = {
                    DockStore.move(entry.packageName, -1)
                    onDismiss()
                })
            }
            if (dockPosition < DockStore.count - 1) {
                OptionRow(title = "Move right in dock", onClick = {
                    DockStore.move(entry.packageName, 1)
                    onDismiss()
                })
            }
            OptionRow(title = "Remove from dock", onClick = {
                DockStore.remove(entry.packageName)
                onDismiss()
            })
        } else if (DockStore.canAdd()) {
            OptionRow(title = "Add to dock", onClick = {
                DockStore.add(entry.packageName)
                onDismiss()
            })
        } else {
            OptionRow(
                title = "Dock is full",
                subtitle = "Remove an app from the dock first (up to ${DockStore.MAX})",
                onClick = onDismiss,
            )
        }
        OptionRow(title = "Close", onClick = onDismiss)
    }
}

/** A dialog row, a little tighter than the settings rows so long option lists fit. */
@Composable
private fun OptionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    GlassRow(title = title, onClick = onClick, modifier = modifier, subtitle = subtitle, compact = true)
}
