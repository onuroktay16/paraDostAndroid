package com.scoreplus.app.data.local.dao

import androidx.room.*
import com.scoreplus.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

data class MonthYearTuple(val month: Int, val year: Int)
data class MonthExpenseSummary(val month: Int, val year: Int, val total: Double)
data class CategoryYearlySummary(val categoryId: Int, val total: Double)

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE month = :month AND year = :year ORDER BY date DESC")
    fun getExpensesByMonthYear(month: Int, year: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT DISTINCT month, year FROM expenses ORDER BY year DESC, month DESC")
    fun getAllMonthYears(): Flow<List<MonthYearTuple>>

    @Query("SELECT month, year, SUM(amount) as total FROM expenses GROUP BY month, year ORDER BY year DESC, month DESC")
    fun getExpenseSummaryByMonth(): Flow<List<MonthExpenseSummary>>

    @Query("SELECT categoryId, SUM(amount) as total FROM expenses WHERE year = :year GROUP BY categoryId ORDER BY total DESC")
    fun getYearlyExpensesByCategory(year: Int): Flow<List<CategoryYearlySummary>>

    @Query("SELECT SUM(amount) FROM expenses WHERE year = :year")
    fun getTotalExpensesByYear(year: Int): Flow<Double?>

    @Query("SELECT * FROM expenses WHERE isSynced = 0")
    suspend fun getUnsyncedExpenses(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): ExpenseEntity?

    @Query("UPDATE expenses SET serverId = :serverId, isSynced = 1 WHERE id = :localId")
    suspend fun markExpenseSynced(localId: Int, serverId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}
