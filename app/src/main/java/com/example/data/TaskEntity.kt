package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val persianDueDate: String = "",
    val gregorianDueDate: String = "",
    val priority: String = "متوسط", // بالا, متوسط, پایین
    val category: String = "عمومی",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
