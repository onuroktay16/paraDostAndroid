package com.scoreplus.app.data.remote.api

import com.scoreplus.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ParaDostApi {

    // ── Auth ─────────────────────────────────────────────────────────────
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<TokenResponse>

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshRequest): Response<Unit>

    // ── Income ────────────────────────────────────────────────────────────
    @GET("income")
    suspend fun getIncome(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): Response<List<IncomeItemResponse>>

    @POST("income")
    suspend fun createIncome(@Body request: IncomeItemRequest): Response<IncomeItemResponse>

    @PUT("income/{id}")
    suspend fun updateIncome(
        @Path("id") id: Int,
        @Body request: IncomeItemRequest
    ): Response<Unit>

    @DELETE("income/{id}")
    suspend fun deleteIncome(@Path("id") id: Int): Response<Unit>

    // ── Expenses ──────────────────────────────────────────────────────────
    @GET("expenses")
    suspend fun getExpenses(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): Response<List<ExpenseResponse>>

    @POST("expenses")
    suspend fun createExpense(@Body request: ExpenseRequest): Response<ExpenseResponse>

    @PUT("expenses/{id}")
    suspend fun updateExpense(
        @Path("id") id: Int,
        @Body request: ExpenseRequest
    ): Response<Unit>

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: Int): Response<Unit>

    // ── Categories ────────────────────────────────────────────────────────
    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryResponse>>

    @POST("categories")
    suspend fun createCategory(@Body request: CategoryRequest): Response<CategoryResponse>

    @POST("categories/bulk")
    suspend fun bulkCreateCategories(@Body requests: List<CategoryRequest>): Response<List<CategoryResponse>>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): Response<Unit>

    // ── Savings ───────────────────────────────────────────────────────────
    @GET("savings")
    suspend fun getSavings(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): Response<List<SavingsResponse>>

    @PUT("savings")
    suspend fun upsertSavings(@Body request: SavingsRequest): Response<SavingsResponse>

    @DELETE("savings")
    suspend fun deleteSavings(@Body request: SavingsRequest): Response<Unit>
}
