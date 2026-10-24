package app.chipos.launcher.data

import android.provider.Settings

/**
 * A page of the Fire OS Settings app that ChipOS opens as-is, for things Android only lets the
 * system's own Settings app change (resets, updates, accounts, display modes).
 *
 * Fire OS answers some of these actions and not others, so the Settings screen checks each one at
 * runtime and hides the ones this device cannot open.
 */
data class SystemShortcut(
    val title: String,
    val subtitle: String,
    val action: String,
)

object SystemShortcuts {
    val all: List<SystemShortcut> = listOf(
        SystemShortcut("Display & resolution", "Screen resolution, HDR and picture options", Settings.ACTION_DISPLAY_SETTINGS),
        SystemShortcut("Accessibility", "Captions, screen reader, magnifier", Settings.ACTION_ACCESSIBILITY_SETTINGS),
        SystemShortcut("Date & time", "Time zone and clock", Settings.ACTION_DATE_SETTINGS),
        SystemShortcut("Language & keyboard", "Menu language and keyboards", Settings.ACTION_LOCALE_SETTINGS),
        SystemShortcut("Updates, restart & reset", "Software update and factory reset", Settings.ACTION_DEVICE_INFO_SETTINGS),
        SystemShortcut("Developer options", "ADB debugging", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    )
}
