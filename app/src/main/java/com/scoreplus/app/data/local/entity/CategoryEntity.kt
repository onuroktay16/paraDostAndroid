package com.scoreplus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String,
    val isDefault: Boolean = false,
    val serverId: Int? = null,
    val isSynced: Boolean = false
)
