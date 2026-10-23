package app.chipos.launcher.data

/**
 * Lets MainActivity handle the Back button for the All apps and Settings screens directly,
 * on the key press itself, instead of waiting for the system's Back callback.
 */
object BackState {
    /** Set by the UI: leaves the current screen for Home. Returns true if it did something. */
    var goHome: (() -> Boolean)? = null

    /** How many dialogs or settings sub-pages are open; they handle Back themselves. */
    var inner = 0
}
