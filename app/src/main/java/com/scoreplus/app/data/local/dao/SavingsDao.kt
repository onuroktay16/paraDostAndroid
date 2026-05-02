package com.scoreplus.app.data.local.dao

import androidx.room.*
import com.scoreplus.app.data.local.entity.SavingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {

    @Query("SELECT * FROM savings WHERE month = :month AND year = :year LIMIT 1")
    fun getSavingsByMonthYear(month: Int, year: Int): Flow<SavingsEntity?>

    @Query("SELECT * FROM savings WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getSavingsByMonthYearSync(month: Int, year: Int): SavingsEntity?

    @Query("SELECT * FROM savings")
    fun getAllSavings(): Flow<List<SavingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavings(savings: SavingsEntity)

    @Query("DELETE FROM savings WHERE month = :month AND year = :year")
    suspend fun deleteSavings(month: Int, year: Int)
}
