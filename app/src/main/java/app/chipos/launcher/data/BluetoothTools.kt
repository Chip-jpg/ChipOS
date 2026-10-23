@file:Suppress("DEPRECATION")

package app.chipos.launcher.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context

/** Bluetooth control through the classic BluetoothAdapter API (fine on Android 9). */
object BluetoothTools {

    fun adapter(context: Context): BluetoothAdapter? =
        context.applicationContext.getSystemService(BluetoothManager::class.java)?.adapter

    fun displayName(device: BluetoothDevice): String =
        try {
            device.name?.takeIf { it.isNotBlank() } ?: device.address
        } catch (e: SecurityException) {
            device.address
        }

    fun bonded(adapter: BluetoothAdapter): List<BluetoothDevice> =
        try {
            adapter.bondedDevices.orEmpty().sortedBy { displayName(it).lowercase() }
        } catch (e: SecurityException) {
            emptyList()
        }

    /** Starts pairing. Remotes, headphones and speakers usually pair without a PIN. */
    fun pair(device: BluetoothDevice): Boolean =
        try {
            device.createBond()
        } catch (e: SecurityException) {
            false
        }

    /**
     * Forgets a paired device. Android has no public call for this, so it uses the hidden
     * removeBond method, which still works on Android 9. Returns false if that fails.
     */
    fun unpair(device: BluetoothDevice): Boolean =
        try {
            device.javaClass.getMethod("removeBond").invoke(device) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
}
