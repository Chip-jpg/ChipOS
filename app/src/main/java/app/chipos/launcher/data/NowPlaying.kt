package app.chipos.launcher.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import app.chipos.launcher.NowPlayingListener

/**
 * What is playing right now, read from other apps' media sessions (Spotify, YouTube Music, ...).
 *
 * It needs the notification listener to be enabled (scripts\enable-now-playing.bat). Without it,
 * [track] simply stays null and the home screen shows no player.
 */
object NowPlaying {

    class Track(
        val packageName: String,
        val title: String,
        val artist: String,
        val art: ImageBitmap?,
        val playing: Boolean,
        val durationMs: Long,
        val positionMs: Long,
        /** SystemClock.elapsedRealtime() when [positionMs] was reported. */
        val positionTime: Long,
        val speed: Float,
        val controller: MediaController,
    )

    var track by mutableStateOf<Track?>(null)
        private set

    /** A short description of what ChipOS can see, shown in Settings to help diagnose problems. */
    var diagnostics by mutableStateOf("Not started")
        private set

    /** Packages that currently have a media session (Spotify, ...). */
    fun mediaPackages(): Set<String> = controllers.map { it.packageName }.toSet()

    /** True once ChipOS is allowed to read media sessions. */
    val isRegistered: Boolean get() = registered

    private val AUDIO_HINTS = listOf("spotify", "music", "audio", "radio", "podcast", "deezer", "tidal", "soundcloud")

    private var manager: MediaSessionManager? = null
    private var component: ComponentName? = null
    private var registered = false
    private var controllers: List<MediaController> = emptyList()
    private var artSource: Bitmap? = null
    private var artImage: ImageBitmap? = null

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = recompute()
        override fun onMetadataChanged(metadata: MediaMetadata?) = recompute()
        override fun onSessionDestroyed() = recompute()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        attach(list.orEmpty())
    }

    fun isListenerEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Safe to call repeatedly. Call again when the app returns to the screen to retry after setup. */
    fun start(context: Context) {
        val app = context.applicationContext
        val sessions = manager ?: app.getSystemService(MediaSessionManager::class.java) ?: return
        manager = sessions
        val listener = component ?: ComponentName(app, NowPlayingListener::class.java)
        component = listener
        try {
            if (!registered) {
                sessions.addOnActiveSessionsChangedListener(sessionsListener, listener)
                registered = true
            }
            attach(sessions.getActiveSessions(listener))
        } catch (e: SecurityException) {
            // The notification listener is not enabled yet.
            registered = false
            track = null
            diagnostics = "Listener not enabled"
        }
    }

    private fun attach(list: List<MediaController>) {
        controllers.forEach { it.unregisterCallback(callback) }
        controllers = list
        list.forEach { it.registerCallback(callback) }
        recompute()
    }

    private fun recompute() {
        diagnostics = if (controllers.isEmpty()) {
            "Listener on, no media sessions"
        } else {
            "Listener on: " + controllers.joinToString("; ") { c ->
                val state = when (c.playbackState?.state) {
                    PlaybackState.STATE_PLAYING -> "playing"
                    PlaybackState.STATE_PAUSED -> "paused"
                    PlaybackState.STATE_BUFFERING -> "buffering"
                    null -> "no state"
                    else -> "state ${c.playbackState?.state}"
                }
                val info = if (c.metadata == null) ", no track info" else ""
                "${c.packageName} $state$info"
            }
        }
        val tracks = controllers.mapNotNull { build(it) }
        track = tracks.firstOrNull { it.playing } ?: tracks.firstOrNull()
    }

    private fun build(controller: MediaController): Track? {
        val metadata = controller.metadata ?: return null
        val state = controller.playbackState ?: return null
        val playing = state.state == PlaybackState.STATE_PLAYING || state.state == PlaybackState.STATE_BUFFERING
        if (!playing && state.state != PlaybackState.STATE_PAUSED) return null

        val title = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: return null
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
        // Music has an artist or an album; video apps usually have neither. Keeps Netflix off the player.
        // Dedicated audio apps are accepted even if they report no artist or album.
        val audioApp = AUDIO_HINTS.any { controller.packageName.contains(it, ignoreCase = true) }
        if (!audioApp && artist.isBlank() && album.isNullOrBlank()) return null

        val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        return Track(
            packageName = controller.packageName,
            title = title,
            artist = artist,
            art = imageFor(bitmap),
            playing = playing,
            durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
            positionMs = state.position.coerceAtLeast(0L),
            positionTime = state.lastPositionUpdateTime,
            speed = state.playbackSpeed,
            controller = controller,
        )
    }

    /** Converting a bitmap is not free, so the last one is reused while the picture is unchanged. */
    private fun imageFor(bitmap: Bitmap?): ImageBitmap? {
        if (bitmap == null || bitmap.isRecycled) return null
        if (bitmap !== artSource) {
            artSource = bitmap
            artImage = bitmap.asImageBitmap()
        }
        return artImage
    }
}
