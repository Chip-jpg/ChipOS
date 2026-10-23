@file:Suppress("DEPRECATION")

package app.chipos.launcher.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import app.chipos.launcher.data.BluetoothTools

/** Bluetooth: turn it on or off, pair new devices, and forget paired ones. */
@Composable
internal fun BluetoothPage() {
    val context = LocalContext.current
    val adapter = remember { BluetoothTools.adapter(context) }

    if (adapter == null) {
        SettingsSheet(title = "Bluetooth") { first ->
            item {
                GlassRow(
                    title = "No Bluetooth on this device",
                    onClick = {},
                    modifier = Modifier.focusRequester(first),
                )
            }
        }
        return
    }

    var on by remember { mutableStateOf(adapter.isEnabled) }
    var paired by remember { mutableStateOf(BluetoothTools.bonded(adapter)) }
    var found by remember { mutableStateOf(emptyList<BluetoothDevice>()) }
    var discovering by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    var selected by remember { mutableStateOf<BluetoothDevice?>(null) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    fun startSearch() {
        if (!adapter.isEnabled) return
        found = emptyList()
        if (adapter.isDiscovering) adapter.cancelDiscovery()
        discovering = adapter.startDiscovery()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) startSearch()
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null && found.none { it.address == device.address }) {
                            found = found + device
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> discovering = true
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> discovering = false
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        on = adapter.isEnabled
                        if (!on) {
                            found = emptyList()
                            discovering = false
                        }
                        paired = BluetoothTools.bonded(adapter)
                    }
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        paired = BluetoothTools.bonded(adapter)
                        found = found.filter { d -> paired.none { it.address == d.address } }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
            // A search left running keeps the radio busy and slows pairing.
            if (adapter.isEnabled && adapter.isDiscovering) adapter.cancelDiscovery()
        }
    }

    SettingsSheet(
        title = "Bluetooth",
        overlayOpen = selected != null,
        overlay = {
            val device = selected
            if (device != null) {
                GlassDialog(
                    title = BluetoothTools.displayName(device),
                    message = "Unpairing removes this device. To use it again, put it in pairing mode and pair it again.",
                    onDismiss = { selected = null },
                ) { first ->
                    GlassRow(
                        title = "Close",
                        onClick = { selected = null },
                        modifier = Modifier.focusRequester(first),
                    )
                    GlassRow(
                        title = "Unpair",
                        onClick = {
                            if (!BluetoothTools.unpair(device)) toast("Couldn't unpair. Use the Fire OS Bluetooth settings.")
                            selected = null
                            paired = BluetoothTools.bonded(adapter)
                        },
                    )
                }
            }
        },
    ) { first ->
        item {
            GlassRow(
                title = "Bluetooth",
                subtitle = if (on) "On" else "Off",
                toggle = on,
                onClick = {
                    if (on) adapter.disable() else adapter.enable()
                    on = !on
                },
                modifier = Modifier.focusRequester(first),
            )
        }

        if (on) {
            item {
                GlassRow(
                    title = "Search for devices",
                    subtitle = if (discovering) {
                        "Searching. Put your device in pairing mode."
                    } else if (!hasPermission) {
                        "Android needs location permission to search. ChipOS never reads your location."
                    } else {
                        "Headphones, speakers, controllers, keyboards"
                    },
                    value = if (discovering) "..." else if (hasPermission) "Search" else "Allow",
                    onClick = {
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            startSearch()
                        }
                    },
                )
            }

            item { SectionTitle("Paired devices") }
            if (paired.isEmpty()) {
                item { GlassRow(title = "Nothing paired yet", onClick = {}) }
            }
            items(paired, key = { it.address }) { device ->
                GlassRow(
                    title = BluetoothTools.displayName(device),
                    subtitle = "Paired",
                    value = "Options",
                    onClick = { selected = device },
                )
            }

            item { SectionTitle("Available devices") }
            if (found.isEmpty()) {
                item {
                    GlassRow(
                        title = if (discovering) "Searching..." else "Nothing found",
                        onClick = {},
                    )
                }
            }
            items(found, key = { it.address }) { device ->
                GlassRow(
                    title = BluetoothTools.displayName(device),
                    subtitle = "Select to pair",
                    value = "Pair",
                    onClick = {
                        if (BluetoothTools.pair(device)) {
                            toast("Pairing with ${BluetoothTools.displayName(device)}")
                        } else {
                            toast("Couldn't start pairing")
                        }
                    },
                )
            }
        }
    }
}
