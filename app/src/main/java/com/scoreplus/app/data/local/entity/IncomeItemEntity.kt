package com.scoreplus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_items")
data class IncomeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val month: Int,
    val year: Int,
    val serverId: Int? = null,
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
