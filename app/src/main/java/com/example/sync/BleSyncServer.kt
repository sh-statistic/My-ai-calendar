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
import com.example.data.EventEntity
import com.example.data.NoteEntity
import com.example.data.SyncData
import com.example.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Robust Two-Way Bluetooth Low Energy (BLE) Synchronization Server.
 * Exposes a GATT Service with read/write characteristics for Chrome Extension Web Bluetooth.
 */
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

    // In-memory buffer for outgoing sync payload
    @Volatile
    private var cachedSyncBytes: ByteArray = ByteArray(0)

    // Write buffer for incoming data chunks from Chrome
    private val incomingBuffer = StringBuilder()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            onLog("سرویس بلوتوث همگام‌سازی فعال شد (GATT: Hamgam-BLE)")
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            onLog("خطا در انتشار بلوتوث (کد: $errorCode)")
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(
            device: android.bluetooth.BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            scope.launch {
                try {
                    when (characteristic?.uuid) {
                        STATUS_CHAR_UUID -> {
                            val statusText = "HAMGAM_BLE_ONLINE".toByteArray(StandardCharsets.UTF_8)
                            val slice = if (offset < statusText.size) statusText.copyOfRange(offset, statusText.size) else ByteArray(0)
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, slice)
                        }
                        SYNC_CHAR_UUID -> {
                            // Prepare latest full sync payload from Room database
                            val syncData = repository.getFullSyncData("Android-BLE-Host")
                            val jsonString = serializeSyncData(syncData)
                            cachedSyncBytes = jsonString.toByteArray(StandardCharsets.UTF_8)

                            val total = cachedSyncBytes.size
                            val slice = if (offset < total) {
                                val end = minOf(total, offset + 512)
                                cachedSyncBytes.copyOfRange(offset, end)
                            } else {
                                ByteArray(0)
                            }
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, slice)
                            onLog("ارسال داده‌های تقویم به اکستنشن کروم از طریق بلوتوث (${slice.size} بایت)")
                        }
                        else -> {
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                        }
                    }
                } catch (e: Exception) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                }
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
                val chunk = String(value, StandardCharsets.UTF_8)
                incomingBuffer.append(chunk)

                // Try parsing complete JSON payload
                val fullStr = incomingBuffer.toString().trim()
                if (fullStr.startsWith("{") && fullStr.endsWith("}")) {
                    scope.launch {
                        try {
                            val syncData = parseSyncData(fullStr)
                            val merged = repository.mergeSyncData(syncData, "Chrome-Extension-BLE")
                            incomingBuffer.setLength(0) // Clear buffer
                            onLog("همگام‌سازی دوطرفه بلوتوث موفق! (${merged.events.size} رویداد، ${merged.tasks.size} کار، ${merged.notes.size} یادداشت)")
                        } catch (e: Exception) {
                            // Chunk not yet complete or parse error
                        }
                    }
                }
            }
        }
    }

    private fun serializeSyncData(data: SyncData): String {
        val root = JSONObject()
        root.put("deviceId", data.deviceId)
        root.put("timestamp", data.timestamp)

        val eventsArr = JSONArray()
        for (e in data.events) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("title", e.title)
            obj.put("description", e.description)
            obj.put("persianDate", e.persianDate)
            obj.put("gregorianDate", e.gregorianDate)
            obj.put("startTime", e.startTime)
            obj.put("endTime", e.endTime)
            obj.put("category", e.category)
            obj.put("colorHex", e.colorHex)
            obj.put("updatedAt", e.updatedAt)
            obj.put("isDeleted", e.isDeleted)
            eventsArr.put(obj)
        }
        root.put("events", eventsArr)

        val tasksArr = JSONArray()
        for (t in data.tasks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("isCompleted", t.isCompleted)
            obj.put("persianDueDate", t.persianDueDate)
            obj.put("gregorianDueDate", t.gregorianDueDate)
            obj.put("priority", t.priority)
            obj.put("category", t.category)
            obj.put("updatedAt", t.updatedAt)
            obj.put("isDeleted", t.isDeleted)
            tasksArr.put(obj)
        }
        root.put("tasks", tasksArr)

        val notesArr = JSONArray()
        for (n in data.notes) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("persianDate", n.persianDate)
            obj.put("colorHex", n.colorHex)
            obj.put("isPinned", n.isPinned)
            obj.put("updatedAt", n.updatedAt)
            obj.put("isDeleted", n.isDeleted)
            notesArr.put(obj)
        }
        root.put("notes", notesArr)

        return root.toString()
    }

    private fun parseSyncData(jsonStr: String): SyncData {
        val root = JSONObject(jsonStr)
        val deviceId = root.optString("deviceId", "Chrome-Extension")
        val timestamp = root.optLong("timestamp", System.currentTimeMillis())

        val eventsList = mutableListOf<EventEntity>()
        val eventsArr = root.optJSONArray("events")
        if (eventsArr != null) {
            for (i in 0 until eventsArr.length()) {
                val obj = eventsArr.getJSONObject(i)
                eventsList.add(
                    EventEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        persianDate = obj.optString("persianDate", ""),
                        gregorianDate = obj.optString("gregorianDate", ""),
                        startTime = obj.optString("startTime", ""),
                        endTime = obj.optString("endTime", ""),
                        category = obj.optString("category", "کاری"),
                        colorHex = obj.optString("colorHex", "#3B82F6"),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isDeleted = obj.optBoolean("isDeleted", false)
                    )
                )
            }
        }

        val tasksList = mutableListOf<TaskEntity>()
        val tasksArr = root.optJSONArray("tasks")
        if (tasksArr != null) {
            for (i in 0 until tasksArr.length()) {
                val obj = tasksArr.getJSONObject(i)
                tasksList.add(
                    TaskEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        persianDueDate = obj.optString("persianDueDate", ""),
                        gregorianDueDate = obj.optString("gregorianDueDate", ""),
                        priority = obj.optString("priority", "متوسط"),
                        category = obj.optString("category", "عمومی"),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isDeleted = obj.optBoolean("isDeleted", false)
                    )
                )
            }
        }

        val notesList = mutableListOf<NoteEntity>()
        val notesArr = root.optJSONArray("notes")
        if (notesArr != null) {
            for (i in 0 until notesArr.length()) {
                val obj = notesArr.getJSONObject(i)
                notesList.add(
                    NoteEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        persianDate = obj.optString("persianDate", ""),
                        colorHex = obj.optString("colorHex", "#FEF3C7"),
                        isPinned = obj.optBoolean("isPinned", false),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isDeleted = obj.optBoolean("isDeleted", false)
                    )
                )
            }
        }

        return SyncData(
            deviceId = deviceId,
            timestamp = timestamp,
            events = eventsList,
            tasks = tasksList,
            notes = notesList
        )
    }

    fun startAdvertising(): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            onLog("بلوتوث دستگاه خاموش است")
            return false
        }

        try {
            advertiser = bluetoothAdapter.bluetoothLeAdvertiser
            if (advertiser == null) {
                onLog("دستگاه از انتشار بلوتوث LE پشتیبانی نمی‌کند")
                return false
            }

            // Setup GATT Server
            gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)
            val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

            val syncChar = BluetoothGattCharacteristic(
                SYNC_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
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
            onLog("دسترسی به بلوتوث اعطا نشده است")
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
