package com.scoreplus.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "savings", primaryKeys = ["month", "year"])
data class SavingsEntity(
    val month: Int,
    val year: Int,
    val amount: Double,
    val serverId: Int? = null,
    val isSynced: Boolean = false
)
