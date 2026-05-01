package com.scoreplus.app.data.remote.dto

data class IncomeItemRequest(
    val amount: Double,
    val description: String,
    val date: Long,
    val month: Int,
    val year: Int,
    val localId: Int? = null
)

data class IncomeItemResponse(
    val id: Int,
    val amount: Double,
    val description: String,
    val date: Double,
    val month: Int,
    val year: Int,
    val localId: Int?
)
