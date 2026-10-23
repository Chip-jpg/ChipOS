@file:Suppress("DEPRECATION")

package app.chipos.launcher.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager

enum class WifiSecurity(val label: String) {
    Open("Open"),
    Psk("Password"),
    Wep("Password (WEP)"),

    /** Company/school networks (802.1X) and WPA3-only networks: not handled here. */
    Unsupported("Needs Fire OS settings"),
}

data class WifiNetwork(
    val ssid: String,
    val security: WifiSecurity,
    /** 0 (weak) to 3 (excellent). */
    val bars: Int,
    val saved: Boolean,
)

/**
 * Wi-Fi control through the classic WifiManager API.
 *
 * ChipOS targets Android 9 (API 28), where an app may still scan, add, connect and forget networks.
 * Android 10 and newer removed this, which is why the target SDK is kept at 28.
 */
object WifiTools {

    fun manager(context: Context): WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /** Android only returns scan results to apps with a location permission. */
    fun hasLocationPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun securityOf(capabilities: String): WifiSecurity = when {
        capabilities.contains("EAP") -> WifiSecurity.Unsupported
        capabilities.contains("WEP") -> WifiSecurity.Wep
        capabilities.contains("PSK") -> WifiSecurity.Psk
        capabilities.contains("SAE") -> WifiSecurity.Unsupported
        else -> WifiSecurity.Open
    }

    fun signalLabel(bars: Int): String = when {
        bars >= 3 -> "Excellent signal"
        bars == 2 -> "Good signal"
        bars == 1 -> "Fair signal"
        else -> "Weak signal"
    }

    fun savedSsids(wm: WifiManager): Set<String> = try {
        wm.configuredNetworks.orEmpty().mapNotNull { it.SSID?.trim('"') }.toSet()
    } catch (e: SecurityException) {
        emptySet()
    }

    fun savedNetworkId(wm: WifiManager, ssid: String): Int? = try {
        wm.configuredNetworks.orEmpty().firstOrNull { it.SSID == "\"$ssid\"" }?.networkId
    } catch (e: SecurityException) {
        null
    }

    /** Networks in range: one entry per name, strongest signal, strongest first. */
    fun scan(wm: WifiManager): List<WifiNetwork> {
        val results = try {
            wm.scanResults.orEmpty()
        } catch (e: SecurityException) {
            return emptyList()
        }
        val saved = savedSsids(wm)
        return results
            .filter { !it.SSID.isNullOrBlank() }
            .groupBy { it.SSID }
            .map { (ssid, group) ->
                val best = group.maxByOrNull { it.level }!!
                WifiNetwork(
                    ssid = ssid,
                    security = securityOf(best.capabilities.orEmpty()),
                    bars = WifiManager.calculateSignalLevel(best.level, 4),
                    saved = ssid in saved,
                )
            }
            .sortedWith(compareByDescending<WifiNetwork> { it.bars }.thenBy { it.ssid.lowercase() })
    }

    /** Name of the network we are connected to, or null if unknown (no permission) or offline. */
    fun currentSsid(wm: WifiManager): String? {
        val info = wm.connectionInfo ?: return null
        if (info.networkId == -1) return null
        val name = info.ssid?.trim('"')
        return if (name.isNullOrBlank() || name == "<unknown ssid>") null else name
    }

    fun ipAddress(wm: WifiManager): String? {
        val raw = wm.connectionInfo?.ipAddress ?: return null
        if (raw == 0) return null
        return "${raw and 0xff}.${raw shr 8 and 0xff}.${raw shr 16 and 0xff}.${raw shr 24 and 0xff}"
    }

    /**
     * Joins a network. Pass the password for secured networks the device has not saved yet.
     * Returns false if Android refused to add or enable the network.
     */
    fun connect(wm: WifiManager, network: WifiNetwork, password: String?): Boolean {
        val existing = savedNetworkId(wm, network.ssid)
        val id = if (password == null && existing != null) {
            existing
        } else {
            val config = WifiConfiguration().apply {
                SSID = "\"${network.ssid}\""
                when (network.security) {
                    WifiSecurity.Psk -> preSharedKey = "\"$password\""
                    WifiSecurity.Wep -> {
                        allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                        wepKeys[0] = "\"$password\""
                        wepTxKeyIndex = 0
                    }
                    else -> allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                }
                if (existing != null) networkId = existing
            }
            if (existing != null) {
                if (wm.updateNetwork(config) == -1) return false
                existing
            } else {
                wm.addNetwork(config)
            }
        }
        if (id < 0) return false
        wm.disconnect()
        val enabled = wm.enableNetwork(id, true)
        wm.reconnect()
        return enabled
    }

    /** Disconnects and forgets a saved network. */
    fun forget(wm: WifiManager, ssid: String) {
        val id = savedNetworkId(wm, ssid) ?: return
        wm.disconnect()
        wm.removeNetwork(id)
        wm.saveConfiguration()
    }
}
