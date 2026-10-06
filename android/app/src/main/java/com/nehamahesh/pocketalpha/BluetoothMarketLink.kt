package com.nehamahesh.pocketalpha

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper

data class NearbyBluetoothDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

class BluetoothMarketLink(
    private val context: Context,
    private val onDevicesChanged: (List<NearbyBluetoothDevice>) -> Unit,
    private val onStatusChanged: (String) -> Unit
) {
    private val manager = context.getSystemService(BluetoothManager::class.java)
    private val adapter get() = manager.adapter
    private val scanner get() = adapter?.bluetoothLeScanner
    private val handler = Handler(Looper.getMainLooper())
    private val devices = linkedMapOf<String, NearbyBluetoothDevice>()
    private var activeGatt: BluetoothGatt? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!hasPermissions(context)) return
            val device = result.device
            val item = NearbyBluetoothDevice(
                name = device.name ?: "BLE device",
                address = device.address,
                rssi = result.rssi
            )
            devices[item.address] = item
            onDevicesChanged(devices.values.sortedByDescending { it.rssi })
        }

        override fun onScanFailed(errorCode: Int) {
            onStatusChanged("Bluetooth scan failed ($errorCode)")
        }
    }

    fun startScan() {
        if (!hasPermissions(context)) {
            onStatusChanged("Bluetooth permission required")
            return
        }
        val bleScanner = scanner
        if (adapter?.isEnabled != true || bleScanner == null) {
            onStatusChanged("Bluetooth is off or BLE is unavailable")
            return
        }

        devices.clear()
        onDevicesChanged(emptyList())
        onStatusChanged("Scanning for nearby BLE devices…")
        bleScanner.startScan(scanCallback)
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            stopScan()
            onStatusChanged("Bluetooth scan complete")
        }, SCAN_WINDOW_MS)
    }

    fun stopScan() {
        if (hasPermissions(context)) {
            runCatching { scanner?.stopScan(scanCallback) }
        }
    }

    fun connect(address: String) {
        if (!hasPermissions(context)) {
            onStatusChanged("Bluetooth permission required")
            return
        }

        val device: BluetoothDevice = runCatching { adapter?.getRemoteDevice(address) }.getOrNull()
            ?: run {
                onStatusChanged("Bluetooth device unavailable")
                return
            }

        activeGatt?.close()
        onStatusChanged("Connecting to ${device.name ?: address}…")
        activeGatt = device.connectGatt(
            context,
            false,
            object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                        onStatusChanged("Connected to ${gatt.device.name ?: gatt.device.address}")
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        onStatusChanged("Bluetooth device disconnected")
                        gatt.close()
                        if (activeGatt === gatt) activeGatt = null
                    }
                }
            }
        )
    }

    fun close() {
        stopScan()
        activeGatt?.close()
        activeGatt = null
    }

    companion object {
        private const val SCAN_WINDOW_MS = 8_000L

        fun requiredPermissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }

        fun hasPermissions(context: Context): Boolean =
            requiredPermissions().all {
                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            }
    }
}
