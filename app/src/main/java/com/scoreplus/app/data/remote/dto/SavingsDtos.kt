package com.scoreplus.app.data.remote.dto

data class SavingsRequest(val month: Int, val year: Int, val amount: Double)

data class SavingsResponse(
    val id: Int,
    val month: Int,
    val year: Int,
    val amount: Double
)
