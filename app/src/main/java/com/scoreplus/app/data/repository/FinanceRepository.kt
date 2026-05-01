package com.scoreplus.app.data.repository

import android.util.Log
import com.scoreplus.app.data.local.AppDatabase
import com.scoreplus.app.data.local.dao.CategoryYearlySummary
import com.scoreplus.app.data.local.dao.MonthExpenseSummary
import com.scoreplus.app.data.local.dao.MonthIncomeSummary
import com.scoreplus.app.data.local.dao.MonthYearTuple
import com.scoreplus.app.data.local.entity.CategoryEntity
import com.scoreplus.app.data.local.entity.ExpenseEntity
import com.scoreplus.app.data.local.entity.IncomeItemEntity
import com.scoreplus.app.data.local.entity.SavingsEntity
import com.scoreplus.app.data.remote.TokenStore
import com.scoreplus.app.data.remote.api.ParaDostApi
import com.scoreplus.app.data.remote.dto.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull

private const val TAG = "FinanceRepository"

class FinanceRepository(
    database: AppDatabase,
    private val api: ParaDostApi,
    private val tokenStore: TokenStore
) {

    private val categoryDao = database.categoryDao()
    private val expenseDao  = database.expenseDao()
    private val incomeDao   = database.incomeDao()
    private val savingsDao  = database.savingsDao()

    private suspend fun isAuthenticated(): Boolean =
        tokenStore.accessToken.firstOrNull()?.isNotBlank() == true

    // ── Categories ────────────────────────────────────────────────────────

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
        if (isAuthenticated()) {
            try {
                val resp = api.createCategory(
                    CategoryRequest(category.name, category.icon, category.isDefault, category.id)
                )
                if (resp.isSuccessful) {
                    resp.body()?.let { categoryDao.markCategorySynced(category.id, it.id) }
                }
            } catch (e: Exception) { Log.w(TAG, "Category sync failed: ${e.message ?: "error"}") }
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
        if (isAuthenticated() && category.serverId != null) {
            try { api.deleteCategory(category.serverId) } catch (e: Exception) { Log.w(TAG, e.message ?: "error") }
        }
    }

    // ── Expenses ──────────────────────────────────────────────────────────

    fun getExpensesByMonthYear(month: Int, year: Int): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByMonthYear(month, year)

    suspend fun insertExpense(expense: ExpenseEntity) {
        expenseDao.insertExpense(expense)
        if (isAuthenticated()) {
            try {
                val resp = api.createExpense(
                    ExpenseRequest(expense.amount, expense.description, expense.categoryId, expense.date, expense.month, expense.year, expense.id)
                )
                if (resp.isSuccessful) {
                    resp.body()?.let { expenseDao.markExpenseSynced(expense.id, it.id) }
                }
            } catch (e: Exception) { Log.w(TAG, "Expense sync failed: ${e.message ?: "error"}") }
        }
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
        if (isAuthenticated() && expense.serverId != null) {
            try { api.deleteExpense(expense.serverId) } catch (e: Exception) { Log.w(TAG, e.message ?: "error") }
        }
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        val updated = expense.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        expenseDao.updateExpense(updated)
        if (isAuthenticated() && expense.serverId != null) {
            try {
                api.updateExpense(
                    expense.serverId,
                    ExpenseRequest(expense.amount, expense.description, expense.categoryId, expense.date, expense.month, expense.year)
                )
                expenseDao.markExpenseSynced(expense.id, expense.serverId)
            } catch (e: Exception) { Log.w(TAG, e.message ?: "error") }
        }
    }

    // ── Income ────────────────────────────────────────────────────────────

    fun getIncomeItemsByMonthYear(month: Int, year: Int): Flow<List<IncomeItemEntity>> =
        incomeDao.getIncomeItemsByMonthYear(month, year)

    suspend fun insertIncomeItem(item: IncomeItemEntity) {
        incomeDao.insertIncomeItem(item)
        if (isAuthenticated()) {
            try {
                val resp = api.createIncome(
                    IncomeItemRequest(item.amount, item.description, item.date, item.month, item.year, item.id)
                )
                if (resp.isSuccessful) {
                    resp.body()?.let { incomeDao.markIncomeItemSynced(item.id, it.id) }
                }
            } catch (e: Exception) { Log.w(TAG, "Income sync failed: ${e.message ?: "error"}") }
        }
    }

    suspend fun updateIncomeItem(item: IncomeItemEntity) {
        val updated = item.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        incomeDao.updateIncomeItem(updated)
        if (isAuthenticated() && item.serverId != null) {
            try {
                api.updateIncome(item.serverId, IncomeItemRequest(item.amount, item.description, item.date, item.month, item.year))
                incomeDao.markIncomeItemSynced(item.id, item.serverId)
            } catch (e: Exception) { Log.w(TAG, e.message ?: "error") }
        }
    }

    suspend fun deleteIncomeItem(item: IncomeItemEntity) {
        incomeDao.deleteIncomeItem(item)
        if (isAuthenticated() && item.serverId != null) {
            try { api.deleteIncome(item.serverId) } catch (e: Exception) { Log.w(TAG, e.message ?: "error") }
        }
    }

    // ── Savings ───────────────────────────────────────────────────────────

    fun getSavingsByMonthYear(month: Int, year: Int): Flow<SavingsEntity?> =
        savingsDao.getSavingsByMonthYear(month, year)

    fun getAllSavings(): Flow<List<SavingsEntity>> = savingsDao.getAllSavings()

    suspend fun upsertSavings(savings: SavingsEntity) {
        savingsDao.upsertSavings(savings)
        if (isAuthenticated()) {
            try {
                api.upsertSavings(SavingsRequest(savings.month, savings.year, savings.amount))
            } catch (e: Exception) { Log.w(TAG, e.message ?: "error") }
        }
    }

    suspend fun deleteSavings(month: Int, year: Int) {
        savingsDao.deleteSavings(month, year)
        if (isAuthenticated()) {
            try { api.deleteSavings(SavingsRequest(month, year, 0.0)) } catch (e: Exception) { Log.w(TAG, e.message ?: "error") }
        }
    }

    // ── Yearly queries ────────────────────────────────────────────────────

    fun getYearlyExpensesByCategory(year: Int): Flow<List<CategoryYearlySummary>> =
        expenseDao.getYearlyExpensesByCategory(year)

    fun getTotalExpensesByYear(year: Int): Flow<Double?> =
        expenseDao.getTotalExpensesByYear(year)

    fun getTotalIncomeByYear(year: Int): Flow<Double?> =
        incomeDao.getTotalIncomeByYear(year)

    fun getExpenseSummaryByMonth(): Flow<List<MonthExpenseSummary>> =
        expenseDao.getExpenseSummaryByMonth()

    fun getIncomeSummaryByMonth(): Flow<List<MonthIncomeSummary>> =
        incomeDao.getIncomeSummaryByMonth()

    fun getAllMonthYears(): Flow<List<MonthYearTuple>> {
        return combine(
            expenseDao.getAllMonthYears(),
            incomeDao.getAllMonthYears()
        ) { expenseMonths, incomeMonths ->
            (expenseMonths + incomeMonths)
                .map { MonthYearTuple(it.month, it.year) }
                .distinctBy { "${it.month}-${it.year}" }
                .sortedWith(compareByDescending<MonthYearTuple> { it.year }.thenByDescending { it.month })
        }
    }
}
