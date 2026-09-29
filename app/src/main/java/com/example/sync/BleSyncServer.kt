package com.example.sync

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import com.example.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.UUID

class BleSyncServer(
    private val context: Context,
    private val repository: AppRepository,
    private val onLog: (String) -> Unit = {}
) {
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB")
        val SYNC_CHAR_UUID: UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")
        val STATUS_CHAR_UUID: UUID = UUID.fromString("0000FFF2-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var isAdvertising: Boolean = false
        private set

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            onLog("انتشار بلوتوث LE با موفقیت آغاز شد (شناسه سرویس: Hamgam-Sync)")
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            onLog("عدم موفقیت در انتشار بلوتوث (کد: $errorCode)")
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(
            device: android.bluetooth.BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            try {
                if (characteristic?.uuid == STATUS_CHAR_UUID) {
                    val statusText = "HAMGAM_BLE_ONLINE".toByteArray(StandardCharsets.UTF_8)
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, statusText)
                } else {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, byteArrayOf(1))
                }
            } catch (e: Exception) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
            }
        }

        override fun onCharacteristicWriteRequest(
            device: android.bluetooth.BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (responseNeeded) {
                try {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                } catch (_: Exception) {}
            }

            if (value != null && value.isNotEmpty()) {
                val receivedStr = String(value, StandardCharsets.UTF_8)
                onLog("داده‌ای از طریق بلوتوث دریافت شد: ${value.size} بایت")
                scope.launch {
                    repository.recordLog("بلوتوث BLE", "دریافت داده از طریق Web Bluetooth", 1, true)
                }
            }
        }
    }

    fun startAdvertising(): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            onLog("بلوتوث در دستگاه فعال نیست")
            return false
        }

        try {
            advertiser = bluetoothAdapter.bluetoothLeAdvertiser
            if (advertiser == null) {
                onLog("دستگاه از انتشار BLE پشتیبانی نمی‌کند")
                return false
            }

            // Setup GATT Server
            gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)
            val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

            val syncChar = BluetoothGattCharacteristic(
                SYNC_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            val statusChar = BluetoothGattCharacteristic(
                STATUS_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )

            service.addCharacteristic(syncChar)
            service.addCharacteristic(statusChar)
            gattServer?.addService(service)

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()

            advertiser?.startAdvertising(settings, data, advertiseCallback)
            return true
        } catch (e: SecurityException) {
            onLog("مجوز دسترسی به بلوتوث اعطا نشده است")
            return false
        } catch (e: Exception) {
            onLog("خطا در راه‌اندازی بلوتوث: ${e.message}")
            return false
        }
    }

    fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            gattServer?.close()
        } catch (_: Exception) {}
        gattServer = null
        advertiser = null
        isAdvertising = false
        onLog("انتشار بلوتوث متوقف شد")
    }
}
