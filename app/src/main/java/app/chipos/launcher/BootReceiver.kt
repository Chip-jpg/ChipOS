package app.chipos.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Opens ChipOS once the stick has finished starting up, so it does not stay on Amazon's home screen. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val launch = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        try {
            context.startActivity(launch)
        } catch (e: Exception) {
            // Fire OS refused; ChipOS opens the next time Amazon's home screen appears.
        }
    }
}
