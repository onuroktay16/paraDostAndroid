package com.scoreplus.app.data.local.dao

import androidx.room.*
import com.scoreplus.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isSynced = 0")
    suspend fun getUnsyncedCategories(): List<CategoryEntity>

    @Query("UPDATE categories SET serverId = :serverId, isSynced = 1 WHERE id = :localId")
    suspend fun markCategorySynced(localId: Int, serverId: Int)

    @Query("SELECT * FROM categories WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): CategoryEntity?

    @Query("UPDATE categories SET serverId = :serverId, isSynced = 1 WHERE id = :localId")
    suspend fun updateServerIdAndSynced(localId: Int, serverId: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteCustomCategories()

    @Query("UPDATE categories SET serverId = NULL, isSynced = 0")
    suspend fun resetSyncStatus()
}
