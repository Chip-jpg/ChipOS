package app.chipos.launcher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private const val LISTS_FILE = "chipos_lists"

private fun SharedPreferences.readList(key: String): List<String> =
    getString(key, null)?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

/** Apps hidden from the All apps screen (they stay installed and still work from the dock). */
object HiddenApps {
    private var prefs: SharedPreferences? = null

    var hidden by mutableStateOf<Set<String>>(emptySet())
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(LISTS_FILE, Context.MODE_PRIVATE)
        prefs = p
        hidden = p.readList("hidden").toSet()
    }

    fun hide(packageName: String) = save(hidden + packageName)

    fun show(packageName: String) = save(hidden - packageName)

    private fun save(set: Set<String>) {
        hidden = set
        prefs?.edit()?.putString("hidden", set.joinToString("\n"))?.apply()
    }
}

/** The apps opened most recently through ChipOS, newest first. */
object RecentApps {
    private const val MAX = 12
    private var prefs: SharedPreferences? = null

    var recent by mutableStateOf<List<String>>(emptyList())
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(LISTS_FILE, Context.MODE_PRIVATE)
        prefs = p
        recent = p.readList("recent")
    }

    fun record(packageName: String) {
        val list = (listOf(packageName) + recent.filter { it != packageName }).take(MAX)
        recent = list
        prefs?.edit()?.putString("recent", list.joinToString("\n"))?.apply()
    }

    fun remove(packageName: String) {
        val list = recent.filter { it != packageName }
        recent = list
        prefs?.edit()?.putString("recent", list.joinToString("\n"))?.apply()
    }
}
