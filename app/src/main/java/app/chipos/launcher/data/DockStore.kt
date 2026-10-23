package app.chipos.launcher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The dock's contents and order, chosen by the user.
 *
 * Until the user edits the dock, it follows the defaults in [DockConfig]. After the first edit the
 * list of package names is saved and used instead. Apps that are not installed are skipped when the
 * dock is shown, so a saved app comes back after it is reinstalled (until the next edit).
 */
object DockStore {
    /** Most apps the dock holds. */
    const val MAX = 7

    private const val FILE = "chipos_dock"
    private const val KEY = "packages"

    private var prefs: SharedPreferences? = null

    /** The saved list, or null while the dock still follows the defaults. Compose state. */
    var saved by mutableStateOf<List<String>?>(null)
        private set

    /** Package names currently shown in the dock, in order. Updated by [resolve]. */
    private var shown: List<String> = emptyList()

    val count: Int get() = shown.size

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs = p
        saved = p.getString(KEY, null)?.split("\n")?.filter { it.isNotBlank() }
    }

    /** Turns the saved (or default) list into dock items for the installed apps. */
    fun resolve(apps: List<AppEntry>): List<DockItem> {
        val list = saved
        val items = if (list == null) {
            AppRepository.resolveDock(apps, DockConfig.slots)
        } else {
            list.mapNotNull { pkg ->
                apps.firstOrNull { it.packageName == pkg }?.let { DockItem(it, it.label) }
            }
        }
        shown = items.map { it.entry.packageName }
        return items
    }

    fun position(packageName: String): Int = shown.indexOf(packageName)

    fun contains(packageName: String): Boolean = packageName in shown

    fun canAdd(): Boolean = shown.size < MAX

    fun add(packageName: String) {
        if (packageName in shown || shown.size >= MAX) return
        set(shown + packageName)
    }

    fun remove(packageName: String) {
        set(shown - packageName)
    }

    /** Moves an app one step left (-1) or right (+1). */
    fun move(packageName: String, delta: Int) {
        val from = shown.indexOf(packageName)
        val to = from + delta
        if (from < 0 || to < 0 || to >= shown.size) return
        val list = shown.toMutableList()
        list.removeAt(from)
        list.add(to, packageName)
        set(list)
    }

    /** Goes back to the default dock. */
    fun reset() {
        prefs?.edit()?.remove(KEY)?.apply()
        saved = null
    }

    private fun set(list: List<String>) {
        prefs?.edit()?.putString(KEY, list.joinToString("\n"))?.apply()
        saved = list
    }
}
