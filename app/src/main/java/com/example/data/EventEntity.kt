package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val persianDate: String, // Format: YYYY/MM/DD
    val gregorianDate: String = "", // Format: YYYY-MM-DD
    val startTime: String = "", // Format: HH:mm
    val endTime: String = "", // Format: HH:mm
    val category: String = "کاری", // کاری, شخصی, مهم, جلسه, یادآوری
    val colorHex: String = "#F59E0B",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
