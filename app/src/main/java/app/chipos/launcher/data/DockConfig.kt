package app.chipos.launcher.data

/**
 * One slot in the dock.
 *
 * [packageCandidates] are tried in order; the first one that is installed (and launchable) wins.
 * If none is installed the slot is hidden.
 */
data class DockSlot(
    val label: String,
    val packageCandidates: List<String>,
)

/**
 * The dock, left to right. Edit this list to change which apps appear.
 *
 * To find a package name on your Fire TV:
 *   adb shell pm list packages | findstr /i <part-of-the-name>
 */
object DockConfig {
    val slots: List<DockSlot> = listOf(
        DockSlot("DAZN", listOf("com.dazn")),
        DockSlot("Spotify", listOf("com.spotify.tv.android")),
        DockSlot("YouTube", listOf("com.amazon.firetv.youtube", "com.google.android.youtube.tv")),
        DockSlot("Netflix", listOf("com.netflix.ninja")),
        DockSlot("Prime Video", listOf("com.amazon.firebat", "com.amazon.avod", "com.amazon.amazonvideo.livingroom")),
        DockSlot("ESPN", listOf("com.espn.gtv")),
    )
}
