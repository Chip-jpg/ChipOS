package app.chipos.launcher

import android.service.notification.NotificationListenerService

/**
 * Intentionally empty. Android only lets an app read other apps' media sessions (what is playing,
 * play/pause/skip) if it has an enabled notification listener, so ChipOS declares this one.
 * ChipOS never reads or stores notifications. The listener is switched on by
 * scripts\enable-now-playing.bat.
 */
class NowPlayingListener : NotificationListenerService()
