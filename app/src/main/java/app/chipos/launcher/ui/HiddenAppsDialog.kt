package app.chipos.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import app.chipos.launcher.data.AppEntry
import app.chipos.launcher.data.HiddenApps

/** Apps hidden from All apps; pick one to show it again. */
@Composable
internal fun HiddenAppsDialog(apps: List<AppEntry>, onDismiss: () -> Unit) {
    val hidden = HiddenApps.hidden.toList()
    GlassDialog(
        title = "Hidden apps",
        message = if (hidden.isEmpty()) "Nothing is hidden. Hold Select on an app and choose Hide from All apps." else "Select an app to show it in All apps again.",
        onDismiss = onDismiss,
    ) { first ->
        hidden.forEachIndexed { index, pkg ->
            val label = apps.firstOrNull { it.packageName == pkg }?.label ?: pkg
            GlassRow(
                title = label,
                subtitle = "Show again",
                compact = true,
                onClick = { HiddenApps.show(pkg) },
                modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
            )
        }
        GlassRow(
            title = "Close",
            compact = true,
            onClick = onDismiss,
            modifier = if (hidden.isEmpty()) Modifier.focusRequester(first) else Modifier,
        )
    }
}
