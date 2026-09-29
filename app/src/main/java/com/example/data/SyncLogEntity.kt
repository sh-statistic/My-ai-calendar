package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String, // "افزونه کروم (Wi-Fi)", "هات‌اسپات محلی", "بلوتوث BLE", "پشتیبان دستی"
    val details: String,
    val itemsCount: Int = 0,
    val isSuccess: Boolean = true
)
