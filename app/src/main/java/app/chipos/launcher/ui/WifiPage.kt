@file:Suppress("DEPRECATION")

package app.chipos.launcher.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import app.chipos.launcher.data.Strings
import app.chipos.launcher.data.SystemActions
import app.chipos.launcher.data.SystemShortcut
import app.chipos.launcher.data.WifiNetwork
import app.chipos.launcher.data.WifiSecurity
import app.chipos.launcher.data.WifiTools

private sealed class WifiDialog {
    data class Password(val network: WifiNetwork) : WifiDialog()
    data class Current(val ssid: String?, val ip: String?) : WifiDialog()
}

/** Wi-Fi: turn it on or off, see networks in range, join them and forget them. */
@Composable
internal fun WifiPage() {
    val context = LocalContext.current
    val wm = remember { WifiTools.manager(context) }

    var enabled by remember { mutableStateOf(wm.isWifiEnabled) }
    var hasPermission by remember { mutableStateOf(WifiTools.hasLocationPermission(context)) }
    var networks by remember { mutableStateOf(emptyList<WifiNetwork>()) }
    var currentSsid by remember { mutableStateOf(WifiTools.currentSsid(wm)) }
    var ip by remember { mutableStateOf(WifiTools.ipAddress(wm)) }
    var scanning by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<WifiDialog?>(null) }
    var password by remember { mutableStateOf("") }

    fun refresh() {
        enabled = wm.isWifiEnabled
        currentSsid = WifiTools.currentSsid(wm)
        ip = WifiTools.ipAddress(wm)
        networks = if (hasPermission && enabled) WifiTools.scan(wm) else emptyList()
    }

    fun startScan() {
        if (!enabled || !hasPermission) return
        scanning = wm.startScan()
        refresh()
    }

    fun toast(message: String) = Toast.makeText(context, Strings.t(message), Toast.LENGTH_SHORT).show()

    fun join(network: WifiNetwork, pass: String?) {
        if (WifiTools.connect(wm, network, pass)) {
            toast("Connecting to ${network.ssid}")
        } else {
            toast("Couldn't join ${network.ssid}")
        }
        dialog = null
        password = ""
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) startScan()
    }

    // Keep the page in sync with the system while it is open.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                scanning = false
                refresh()
            }
        }
        val filter = IntentFilter().apply {
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(hasPermission, enabled) {
        refresh()
        if (enabled && hasPermission) scanning = wm.startScan()
    }

    val wifiSettings = remember {
        SystemShortcut("Wi-Fi", "", Settings.ACTION_WIFI_SETTINGS)
    }
    val connected = ip != null

    SettingsSheet(
        title = "Wi-Fi",
        overlayOpen = dialog != null,
        overlay = {
            when (val d = dialog) {
                is WifiDialog.Password -> GlassDialog(
                    title = d.network.ssid,
                    message = "Enter the Wi-Fi password.",
                    onDismiss = { dialog = null; password = "" },
                ) { first ->
                    GlassField(
                        value = password,
                        onValueChange = { password = it },
                        hint = "Password",
                        onDone = {
                            if (password.length >= 8 || d.network.security == WifiSecurity.Wep) {
                                join(d.network, password)
                            } else {
                                toast("Wi-Fi passwords have at least 8 characters")
                            }
                        },
                        modifier = Modifier.focusRequester(first),
                    )
                    GlassRow(
                        title = "Connect",
                        onClick = {
                            if (password.length >= 8 || d.network.security == WifiSecurity.Wep) {
                                join(d.network, password)
                            } else {
                                toast("Wi-Fi passwords have at least 8 characters")
                            }
                        },
                    )
                    GlassRow(title = "Cancel", onClick = { dialog = null; password = "" })
                }

                is WifiDialog.Current -> GlassDialog(
                    title = d.ssid ?: "Connected",
                    message = (d.ip?.let { "IP address $it. " } ?: "") +
                        "Forgetting a network means you need its password to join it again.",
                    onDismiss = { dialog = null },
                ) { first ->
                    GlassRow(
                        title = "Close",
                        onClick = { dialog = null },
                        modifier = Modifier.focusRequester(first),
                    )
                    GlassRow(
                        title = "Disconnect",
                        onClick = {
                            wm.disconnect()
                            dialog = null
                            refresh()
                        },
                    )
                    if (d.ssid != null) {
                        GlassRow(
                            title = "Forget this network",
                            onClick = {
                                WifiTools.forget(wm, d.ssid)
                                dialog = null
                                refresh()
                            },
                        )
                    }
                }

                null -> Unit
            }
        },
    ) { first ->
        item {
            GlassRow(
                title = "Wi-Fi",
                subtitle = if (enabled) "On" else "Off",
                toggle = enabled,
                onClick = {
                    enabled = !enabled
                    wm.isWifiEnabled = enabled
                },
                modifier = Modifier.focusRequester(first),
            )
        }

        if (!hasPermission) {
            item {
                GlassRow(
                    title = "Allow network scanning",
                    subtitle = "Android only lists networks for apps with location permission. ChipOS never reads your location.",
                    value = "Allow",
                    onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                )
            }
        }

        if (enabled && connected) {
            item {
                GlassRow(
                    title = currentSsid?.let { "Connected to $it" } ?: "Connected",
                    subtitle = "IP address $ip",
                    value = "Details",
                    onClick = { dialog = WifiDialog.Current(currentSsid, ip) },
                )
            }
        }

        if (enabled && hasPermission) {
            item {
                GlassRow(
                    title = "Scan again",
                    subtitle = if (scanning) "Looking for networks..." else "Refresh the list of networks",
                    value = if (scanning) "..." else "Scan",
                    onClick = { startScan() },
                )
            }
            item { SectionTitle("Networks") }
            if (networks.isEmpty()) {
                item {
                    GlassRow(
                        title = if (scanning) "Searching..." else "No networks found",
                        subtitle = "If this stays empty, turn Location on in the TV settings and scan again.",
                        value = "Location",
                        onClick = {
                            SystemActions.open(
                                context,
                                SystemShortcut("Location", "", Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                            )
                        },
                    )
                }
            }
            items(networks, key = { it.ssid }) { network ->
                val isCurrent = network.ssid == currentSsid
                GlassRow(
                    title = network.ssid,
                    subtitle = buildString {
                        append(network.security.label)
                        append("  ·  ")
                        append(WifiTools.signalLabel(network.bars))
                        if (network.saved) append("  ·  Saved")
                    },
                    value = if (isCurrent) "Connected" else "",
                    onClick = {
                        when {
                            isCurrent -> dialog = WifiDialog.Current(currentSsid, ip)
                            network.security == WifiSecurity.Unsupported -> {
                                toast("This network needs the TV's Wi-Fi settings")
                                SystemActions.open(context, wifiSettings)
                            }
                            network.saved || network.security == WifiSecurity.Open -> join(network, null)
                            else -> {
                                password = ""
                                dialog = WifiDialog.Password(network)
                            }
                        }
                    },
                )
            }
        }

        item { SectionTitle("More") }
        item {
            GlassRow(
                title = "TV Wi-Fi settings",
                subtitle = "Hidden networks and company or school networks",
                value = "›",
                onClick = { SystemActions.open(context, wifiSettings) },
            )
        }
    }
}
