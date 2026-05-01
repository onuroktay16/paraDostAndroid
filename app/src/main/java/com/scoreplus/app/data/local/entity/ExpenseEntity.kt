package com.scoreplus.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val amount: Double,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val month: Int,
    val year: Int,
    val serverId: Int? = null,
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
