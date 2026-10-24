package app.chipos.launcher.data

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

object SystemActions {

    fun launch(context: Context, entry: AppEntry) {
        try {
            context.startActivity(entry.launchIntent)
            RecentApps.record(entry.packageName)
        } catch (e: Exception) {
            Toast.makeText(context, "Couldn't open ${entry.label}", Toast.LENGTH_SHORT).show()
        }
    }

    /** True if this device has an activity that handles the shortcut. */
    fun isAvailable(context: Context, shortcut: SystemShortcut): Boolean =
        Intent(shortcut.action).resolveActivity(context.packageManager) != null

    /** Opens one page of the Fire OS Settings app. */
    fun open(context: Context, shortcut: SystemShortcut) {
        try {
            context.startActivity(Intent(shortcut.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, "${shortcut.title} isn't available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the main Fire OS Settings app. A few routes are tried, because Fire OS ships its own
     * settings app. If none works, hold the Home button on the remote: Fire OS shows a quick menu
     * with Settings.
     */
    fun openSettings(context: Context) {
        val pm = context.packageManager
        val attempts = listOfNotNull(
            Intent(Settings.ACTION_SETTINGS),
            pm.getLeanbackLaunchIntentForPackage("com.amazon.tv.settings.v2"),
            pm.getLaunchIntentForPackage("com.amazon.tv.settings.v2"),
        )
        for (intent in attempts) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: Exception) {
                // Try the next route.
            }
        }
        Toast.makeText(context, "Couldn't open Settings.", Toast.LENGTH_LONG).show()
    }
}
