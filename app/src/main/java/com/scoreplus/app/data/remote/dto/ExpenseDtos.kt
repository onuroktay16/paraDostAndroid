package com.scoreplus.app.data.remote.dto

data class ExpenseRequest(
    val amount: Double,
    val description: String,
    val categoryLocalId: Int,
    val date: Long,
    val month: Int,
    val year: Int,
    val localId: Int? = null
)

data class ExpenseResponse(
    val id: Int,
    val amount: Double,
    val description: String,
    val categoryLocalId: Int,
    val date: Double,
    val month: Int,
    val year: Int,
    val localId: Int?
)
