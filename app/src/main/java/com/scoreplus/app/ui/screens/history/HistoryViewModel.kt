package com.scoreplus.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scoreplus.app.data.local.entity.SavingsEntity
import com.scoreplus.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar

data class MonthSummaryItem(
    val month: Int,
    val year: Int,
    val income: Double,
    val totalExpenses: Double,
    val customSavings: Double? = null
) {
    val balance: Double get() = income - totalExpenses
    val displaySavings: Double get() = customSavings ?: balance
}

class HistoryViewModel(private val repository: FinanceRepository) : ViewModel() {

    private fun allMonthsFromStart(): List<Pair<Int, Int>> {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentYear = now.get(Calendar.YEAR)
        val result = mutableListOf<Pair<Int, Int>>()
        var month = currentMonth
        var year = currentYear
        while (year > 2026 || (year == 2026 && month >= 1)) {
            result.add(Pair(month, year))
            if (month == 1) { month = 12; year-- } else month--
        }
        return result
    }

    val monthlySummaries: StateFlow<List<MonthSummaryItem>> = combine(
        repository.getIncomeSummaryByMonth(),
        repository.getExpenseSummaryByMonth(),
        repository.getAllSavings()
    ) { incomeSummaries, expenseSummaries, allSavings ->
        allMonthsFromStart().map { (month, year) ->
            val income = incomeSummaries.find { it.month == month && it.year == year }
            val expense = expenseSummaries.find { it.month == month && it.year == year }
            val savings = allSavings.find { it.month == month && it.year == year }
            MonthSummaryItem(
                month = month,
                year = year,
                income = income?.total ?: 0.0,
                totalExpenses = expense?.total ?: 0.0,
                customSavings = savings?.amount
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class HistoryViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
