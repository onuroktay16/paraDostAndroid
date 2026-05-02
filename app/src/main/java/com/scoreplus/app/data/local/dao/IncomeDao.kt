package com.scoreplus.app.data.local.dao

import androidx.room.*
import com.scoreplus.app.data.local.entity.IncomeItemEntity
import kotlinx.coroutines.flow.Flow

data class MonthIncomeSummary(val month: Int, val year: Int, val total: Double)

@Dao
interface IncomeDao {

    @Query("SELECT * FROM income_items WHERE month = :month AND year = :year ORDER BY date DESC")
    fun getIncomeItemsByMonthYear(month: Int, year: Int): Flow<List<IncomeItemEntity>>

    @Query("SELECT month, year, SUM(amount) as total FROM income_items GROUP BY month, year ORDER BY year DESC, month DESC")
    fun getIncomeSummaryByMonth(): Flow<List<MonthIncomeSummary>>

    @Query("SELECT DISTINCT month, year FROM income_items ORDER BY year DESC, month DESC")
    fun getAllMonthYears(): Flow<List<MonthYearTuple>>

    @Query("SELECT SUM(amount) FROM income_items WHERE year = :year")
    fun getTotalIncomeByYear(year: Int): Flow<Double?>

    @Query("DELETE FROM income_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM income_items WHERE isSynced = 0")
    suspend fun getUnsyncedIncomeItems(): List<IncomeItemEntity>

    @Query("SELECT * FROM income_items WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): IncomeItemEntity?

    @Query("UPDATE income_items SET serverId = :serverId, isSynced = 1 WHERE id = :localId")
    suspend fun markIncomeItemSynced(localId: Int, serverId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeItem(item: IncomeItemEntity)

    @Update
    suspend fun updateIncomeItem(item: IncomeItemEntity)

    @Delete
    suspend fun deleteIncomeItem(item: IncomeItemEntity)
}
