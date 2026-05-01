package com.scoreplus.app.data.remote.dto

data class CategoryRequest(
    val name: String,
    val icon: String,
    val isDefault: Boolean,
    val localId: Int? = null
)

data class CategoryResponse(
    val id: Int,
    val name: String,
    val icon: String,
    val isDefault: Boolean,
    val localId: Int?
)
