package com.scoreplus.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "incomes", primaryKeys = ["month", "year"])
data class IncomeEntity(
    val month: Int,
    val year: Int,
    val amount: Double,
    val note: String = ""
)
