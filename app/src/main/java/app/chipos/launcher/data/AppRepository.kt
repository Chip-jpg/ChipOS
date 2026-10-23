package app.chipos.launcher.data

import android.content.Context
import android.content.Intent

/** A launchable app. */
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val launchIntent: Intent,
)

/** An app resolved for a dock slot. */
data class DockItem(
    val entry: AppEntry,
    val label: String,
)

object AppRepository {

    /** Packages that must never show up in the app list. */
    private val hiddenPackages = setOf(
        "com.amazon.tv.launcher", // Amazon's own home screen
    )

    /** Every app that can be launched from a TV or phone launcher, sorted by name. */
    fun loadAll(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val seen = HashSet<String>()
        val result = ArrayList<AppEntry>()

        // TV apps first, so their TV activity is the one we launch.
        val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        for (category in categories) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in pm.queryIntentActivities(query, 0)) {
                val activity = info.activityInfo ?: continue
                val pkg = activity.packageName
                if (pkg == context.packageName || pkg in hiddenPackages) continue
                if (!seen.add(pkg)) continue

                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(category)
                    .setClassName(pkg, activity.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                result += AppEntry(
                    packageName = pkg,
                    activityName = activity.name,
                    label = info.loadLabel(pm).toString(),
                    launchIntent = intent,
                )
            }
        }
        return result.sortedBy { it.label.lowercase() }
    }

    /** Matches the configured dock slots against the installed apps. Missing apps are skipped. */
    fun resolveDock(apps: List<AppEntry>, slots: List<DockSlot>): List<DockItem> =
        slots.mapNotNull { slot ->
            val entry = slot.packageCandidates.firstNotNullOfOrNull { pkg ->
                apps.firstOrNull { it.packageName == pkg }
            }
            entry?.let { DockItem(it, slot.label) }
        }
}
