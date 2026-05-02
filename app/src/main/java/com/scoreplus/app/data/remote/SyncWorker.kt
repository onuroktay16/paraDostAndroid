package com.scoreplus.app.data.remote

import android.content.Context
import android.util.Log
import androidx.work.*
import com.scoreplus.app.ScorePlusApp
import com.scoreplus.app.data.local.entity.CategoryEntity
import com.scoreplus.app.data.local.entity.ExpenseEntity
import com.scoreplus.app.data.local.entity.IncomeItemEntity
import com.scoreplus.app.data.local.entity.SavingsEntity
import com.scoreplus.app.data.remote.dto.*
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

private const val TAG = "SyncWorker"

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "paradost_sync_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as ScorePlusApp
        val tokenStore = app.tokenStore
        val api = app.api

        val accessToken = tokenStore.accessToken.firstOrNull()
        if (accessToken.isNullOrBlank()) {
            Log.d(TAG, "Not authenticated, skipping sync")
            return Result.success()
        }

        return try {
            syncExpenses(app, api)
            syncIncomeItems(app, api)
            syncCategories(app, api)
            Log.d(TAG, "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun syncExpenses(app: ScorePlusApp, api: com.scoreplus.app.data.remote.api.ParaDostApi) {
        val unsynced = app.database.expenseDao().getUnsyncedExpenses()
        for (expense in unsynced) {
            try {
                if (expense.serverId != null) {
                    // Already has server ID → update
                    api.updateExpense(
                        expense.serverId,
                        ExpenseRequest(expense.amount, expense.description, expense.categoryId, expense.date, expense.month, expense.year)
                    )
                    app.database.expenseDao().markExpenseSynced(expense.id, expense.serverId)
                } else {
                    // No server ID → create
                    val resp = api.createExpense(
                        ExpenseRequest(expense.amount, expense.description, expense.categoryId, expense.date, expense.month, expense.year, expense.id)
                    )
                    if (resp.isSuccessful) {
                        resp.body()?.let { app.database.expenseDao().markExpenseSynced(expense.id, it.id) }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync expense ${expense.id}: ${e.message}")
            }
        }
    }

    private suspend fun syncIncomeItems(app: ScorePlusApp, api: com.scoreplus.app.data.remote.api.ParaDostApi) {
        val unsynced = app.database.incomeDao().getUnsyncedIncomeItems()
        for (item in unsynced) {
            try {
                if (item.serverId != null) {
                    api.updateIncome(item.serverId, IncomeItemRequest(item.amount, item.description, item.date, item.month, item.year))
                    app.database.incomeDao().markIncomeItemSynced(item.id, item.serverId)
                } else {
                    val resp = api.createIncome(IncomeItemRequest(item.amount, item.description, item.date, item.month, item.year, item.id))
                    if (resp.isSuccessful) {
                        resp.body()?.let { app.database.incomeDao().markIncomeItemSynced(item.id, it.id) }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync income item ${item.id}: ${e.message}")
            }
        }
    }

    private suspend fun syncCategories(app: ScorePlusApp, api: com.scoreplus.app.data.remote.api.ParaDostApi) {
        val unsynced = app.database.categoryDao().getUnsyncedCategories()
        for (category in unsynced) {
            try {
                val resp = api.createCategory(CategoryRequest(category.name, category.icon, category.isDefault, category.id))
                if (resp.isSuccessful) {
                    resp.body()?.let { app.database.categoryDao().markCategorySynced(category.id, it.id) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync category ${category.id}: ${e.message}")
            }
        }
    }

    suspend fun pullFromBackend(app: ScorePlusApp, api: com.scoreplus.app.data.remote.api.ParaDostApi) {
        try {
            // 1. Kategorileri çek: remoteLocalId -> localId eşlemesi oluştur
            val categoryMapping = mutableMapOf<Int, Int>() // remote.localId -> local.id
            val remoteCategories = api.getCategories()
            if (remoteCategories.isSuccessful) {
                remoteCategories.body()?.forEach { remote ->
                    val existing = app.database.categoryDao().getByServerId(remote.id)
                    if (existing != null) {
                        if (remote.localId != null) categoryMapping[remote.localId] = existing.id
                    } else {
                        val matchByName = app.database.categoryDao().getByName(remote.name)
                        if (matchByName != null) {
                            app.database.categoryDao().updateServerIdAndSynced(matchByName.id, remote.id)
                            if (remote.localId != null) categoryMapping[remote.localId] = matchByName.id
                        } else {
                            val newId = app.database.categoryDao().insertCategory(
                                CategoryEntity(name = remote.name, icon = remote.icon, isDefault = remote.isDefault, serverId = remote.id, isSynced = true)
                            )
                            if (remote.localId != null) categoryMapping[remote.localId] = newId.toInt()
                        }
                    }
                }
            }

            // 2. Gelirleri çek
            val remoteIncome = api.getIncome()
            if (remoteIncome.isSuccessful) {
                remoteIncome.body()?.forEach { remote ->
                    val existing = app.database.incomeDao().getByServerId(remote.id)
                    if (existing == null) {
                        app.database.incomeDao().insertIncomeItem(
                            IncomeItemEntity(
                                amount = remote.amount,
                                description = remote.description,
                                date = remote.date.toLong(),
                                month = remote.month,
                                year = remote.year,
                                serverId = remote.id,
                                isSynced = true
                            )
                        )
                    }
                }
            }

            // 3. Giderleri çek
            val remoteExpenses = api.getExpenses()
            if (remoteExpenses.isSuccessful) {
                remoteExpenses.body()?.forEach { remote ->
                    val existing = app.database.expenseDao().getByServerId(remote.id)
                    if (existing == null) {
                        val localCategoryId = categoryMapping[remote.categoryLocalId] ?: remote.categoryLocalId
                        app.database.expenseDao().insertExpense(
                            ExpenseEntity(
                                categoryId = localCategoryId,
                                amount = remote.amount,
                                description = remote.description,
                                date = remote.date.toLong(),
                                month = remote.month,
                                year = remote.year,
                                serverId = remote.id,
                                isSynced = true
                            )
                        )
                    }
                }
            }

            // 4. Birikimleri çek
            val remoteSavings = api.getSavings()
            if (remoteSavings.isSuccessful) {
                remoteSavings.body()?.forEach { remote ->
                    val existing = app.database.savingsDao().getSavingsByMonthYearSync(remote.month, remote.year)
                    if (existing == null) {
                        app.database.savingsDao().upsertSavings(
                            SavingsEntity(month = remote.month, year = remote.year, amount = remote.amount, isSynced = true)
                        )
                    }
                }
            }

            Log.d(TAG, "Pull from backend completed")
        } catch (e: Exception) {
            Log.e(TAG, "Pull from backend failed: ${e.message}", e)
        }
    }
}
